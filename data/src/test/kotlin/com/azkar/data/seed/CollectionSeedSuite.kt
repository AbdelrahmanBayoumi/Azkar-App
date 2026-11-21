package com.azkar.data.seed

import com.azkar.data.repo.exposed.ExposedCollectionRepository
import com.azkar.data.repo.exposed.ExposedTagRepository
import com.azkar.data.table.TagTable
import com.azkar.data.transaction.ExposedTransactionRunner
import com.azkar.domain.model.CollectionKind
import com.azkar.domain.model.impl.RemembranceCollectionImpl
import com.azkar.testUtils.TestDatabase
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** Shipped collection content, initial user state and idempotent bootstrap. *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionSeedSuite by testSuite("Seeding the official collections") {
    test("every shipped collection is present, official, and fully described") {
        withSeed { seeded ->
            val official = seeded.collections.findAll().filter { it.kind == CollectionKind.OFFICIAL }

            assertThat(official.map { it.name })
                .containsExactlyInAnyOrderElementsOf(OfficialCollectionCatalogue.entries.map { it.name })
            assertThat(official).allSatisfy { collection ->
                assertThat(collection.name).isNotBlank()
                assertThat(collection.description).isNotBlank()
                assertThat(collection.iconKey).isNotBlank()
            }
        }
    }

    test("a shipped collection starts unpinned, off the home rail, and with no cover") {
        withSeed { seeded ->
            val morning = seeded.collectionNamed(OfficialCollectionCatalogue.entries.first())

            assertThat(morning.isPinned).isFalse()
            assertThat(morning.isOnHome).isFalse()
            assertThat(morning.coverImagePath).isNull()
            // Null rather than a timestamp: a collection nobody has opened has not been visited, and
            // a default here would make the recently-opened ordering report a visit at seed time.
            assertThat(morning.lastOpenedAt).isNull()
        }
    }

    test("every shipped collection resolves at least one remembrance") {
        withSeed { seeded ->
            val empty =
                OfficialCollectionCatalogue.entries.filter { entry ->
                    seeded.collectionNamed(entry).members.isEmpty()
                }

            assertThat(empty.map { it.name })
                .describedAs("Every shipped collection must have its explicit members")
                .isEmpty()
        }
    }

    test("stored membership follows seed keys and order independently of subject labels") {
        withSeed { seeded ->
            OfficialCollectionCatalogue.entries.forEach { entry ->
                assertThat(seeded.collectionNamed(entry).members.map { it.id })
                    .containsExactlyElementsOf(entry.members.map { seeded.remembranceIds.getValue(it) })
            }
        }
    }

    test("every collection label is a tag the seed dataset actually uses") {
        withSeed { seeded ->
            val datasetTags = tagNamesIn(seeded.database)
            val missing =
                OfficialCollectionCatalogue.entries
                    .mapNotNull { it.tagName }
                    .filterNot { it.lowercase() in datasetTags }

            assertThat(missing).describedAs("A shipped label must be used by the dataset").isEmpty()
        }
    }

    test("no two catalogue entries have the same canonical name") {
        val entries = OfficialCollectionCatalogue.entries

        assertThat(entries.map { it.name }).doesNotHaveDuplicates()
    }

    test("every entry has a subject label independent of membership") {
        val entries = OfficialCollectionCatalogue.entries

        assertThat(entries.mapNotNull { it.tagName })
            .describedAs("The starred shelf is the user's, so it cannot be a catalogue entry")
            .hasSize(entries.size)
    }

    test("a shipped collection carries its own tag as a label") {
        withSeed { seeded ->
            val labels =
                OfficialCollectionCatalogue.entries.associate { entry ->
                    entry.name to seeded.collectionNamed(entry).tags.map { it.name.lowercase() }
                }

            labels.forEach { (name, tags) ->
                assertThat(tags)
                    .describedAs("$name should carry its subject label")
                    .hasSize(1)
            }
        }
    }

    test("seeding again is a no-op rather than a second set of rows") {
        withSeed { seeded ->
            val before = seeded.collections.findAll().map { it.id to it.updatedAt }

            reseed(seeded.database)

            assertThat(seeded.collections.findAll().map { it.id to it.updatedAt }).isEqualTo(before)
        }
    }

    test("seeding again never overwrites what the user changed") {
        withSeed { seeded ->
            val morning = seeded.collectionNamed(OfficialCollectionCatalogue.entries.first())
            // Tamper with everything at once, because that is what a user editing a shipped shelf
            // looks like from the seed's side: the name, the description, the icon, the pin and the
            // cover are all theirs now, and the seed has no business taking any of them back.
            transaction(seeded.database) {
                exec(
                    "UPDATE collection SET name = 'Mine', description = 'Mine', icon_key = 'star', " +
                        "pinned_at = CURRENT_TIMESTAMP, cover_image_path = 'cover.png' WHERE id = ${morning.id}",
                )
            }

            reseed(seeded.database)

            val after = requireNotNull(seeded.collections.findById(morning.id))
            assertThat(after.name).isEqualTo("Mine")
            assertThat(after.description).isEqualTo("Mine")
            assertThat(after.iconKey).isEqualTo("star")
            assertThat(after.isPinned).isTrue()
            assertThat(after.coverImagePath).isEqualTo("cover.png")
        }
    }

    test("seeding leaves a custom collection entirely alone") {
        withSeed { seeded ->
            val mineId =
                requireNotNull(
                    seeded.collections
                        .save(RemembranceCollectionImpl(name = "Mine", description = "My own shelf"))
                        .id,
                )

            reseed(seeded.database)

            val reloaded = requireNotNull(seeded.collections.findById(mineId))
            assertThat(reloaded.name).isEqualTo("Mine")
            assertThat(reloaded.description).isEqualTo("My own shelf")
            assertThat(reloaded.kind).isEqualTo(CollectionKind.CUSTOM)
            // A custom collection gets no labels from the seed, because the seed only labels what it
            // also seeded. Inventing one here would hand the user a label they never chose.
            assertThat(reloaded.tags).isEmpty()
        }
    }
}

/** The seeded database plus a collection repository over it, which is all this suite needs. */
private class SeededCollections(
    val database: Database,
    val collections: ExposedCollectionRepository,
    val remembranceIds: Map<String, Long>,
) {
    /**
     * The collection a catalogue entry produced.
     *
     * Matched by name rather than by a key of our own, because there is no longer a key to match:
     * the seed's whole point was to stop carrying an identity that has to be recognised across
     * launches.
     */
    suspend fun collectionNamed(entry: OfficialCollectionCatalogue.Entry) =
        requireNotNull(collections.findAll().singleOrNull { it.name == entry.name }) {
            "The seed did not produce ${entry.name}"
        }
}

/** Runs each test against a fresh database with the complete shipped dataset. */
private suspend fun withSeed(block: suspend (SeededCollections) -> Unit) {
    val database = TestDatabase.bootstrapWithH2()
    val ids =
        transaction(database) {
            val seeded = SeedDataLoader.load()
            CollectionSeed.load(seeded.remembranceIds)
            seeded.remembranceIds
        }
    val transactions = ExposedTransactionRunner(database)
    block(
        SeededCollections(
            database,
            ExposedCollectionRepository(transactions, ExposedTagRepository(transactions)),
            ids,
        ),
    )
}

/** Runs the seed the way a second call to the bootstrapper would. */
private fun reseed(database: Database) {
    transaction(database) { CollectionSeed.load() }
}

/** The lowercased names of every tag row, which is the set a catalogue tag can resolve against. */
private fun tagNamesIn(database: Database): Set<String> =
    transaction(database) {
        TagTable.selectAll().mapTo(mutableSetOf()) { it[TagTable.name].lowercase() }
    }
