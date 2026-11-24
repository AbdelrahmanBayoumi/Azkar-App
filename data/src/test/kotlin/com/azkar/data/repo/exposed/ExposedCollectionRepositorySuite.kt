package com.azkar.data.repo.exposed

import com.azkar.data.seed.OfficialCollectionCatalogue
import com.azkar.data.table.CollectionMemberTable
import com.azkar.domain.exception.CollectionNotFoundException
import com.azkar.domain.model.CollectionKind
import com.azkar.domain.model.RemembranceCollection
import com.azkar.domain.model.impl.RemembranceCollectionImpl
import com.azkar.domain.model.impl.TagImpl
import com.azkar.domain.model.renamed
import com.azkar.testUtils.ExposedRepositories
import com.azkar.testUtils.seededCollections
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Behaviour of the collection repository against a real database.
 *
 * The two things that cannot be checked any other way are both here: the membership rows the seed
 * wrote for a shipped collection, and the position bookkeeping of a user-made one, which is a run of
 * renumbering updates.
 *
 * The reference fixture seeds the two tags the catalogue names — `morning` and `evening`, stored
 * folded while the catalogue spells them `Morning` and `Evening`, which is what keeps the tag match
 * honest — so the shelf comes up with two populated shipped collections and the rest empty.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val ExposedCollectionRepositorySuite by testSuite("ExposedCollectionRepository") {
    seededCollections {
        testSuite("seeded membership") {
            test("a shipped collection's members are the rows the seed wrote for it") { repositories ->
                // Remembrances 1 and 3 carry the `Morning` tag in the reference fixture.
                val morning = collectionNamed(repositories, "Morning Azkar")

                assertThat(morning.members.map { it.id }).containsExactly(1L, 3L)
            }

            test("the tag match is case-insensitive on both sides") { repositories ->
                // The tag is stored folded as `evening`; the catalogue spells it `Evening`.
                val evening = collectionNamed(repositories, "Evening Azkar")

                assertThat(evening.members.map { it.id }).containsExactly(2L)
            }

            test("a shipped collection whose tag this fixture never created comes up empty") { repositories ->
                // `Travel` is a shipped collection whose tag this fixture never created. It must come
                // up empty rather than throw and take the whole shelf down with it.
                val travel = collectionNamed(repositories, "Travel")

                assertThat(travel.members).isEmpty()
            }

            test("a shipped collection carries its own tag as a label") { repositories ->
                val morning = collectionNamed(repositories, "Morning Azkar")

                assertThat(morning.tags.map { it.name }).containsExactly("morning")
            }

            test("membership is rows like any other, so a member added later joins the shelf") { repositories ->
                // One table for membership means a star added after the seed is an ordinary write
                // rather than a second rule. The seed wrote the rows that existed when it ran; what
                // happens afterwards is the user's business, on a shipped collection included.
                val morningId = collectionNamed(repositories, "Morning Azkar").requireId()

                repositories.collections.addMembers(morningId, listOf(2L))

                // In any order, and deliberately: a member added later lands at the end of the
                // sequence rather than among the ones the seed ordered, which is the one thing
                // this test is not about.
                assertThat(collectionNamed(repositories, "Morning Azkar").members.map { it.id })
                    .containsExactlyInAnyOrder(1L, 2L, 3L)
            }

            test("the membership rows of a shipped collection are its own") { repositories ->
                val morningId = collectionNamed(repositories, "Morning Azkar").requireId()

                assertThat(membershipRowCount(repositories.database, morningId)).isEqualTo(2)
            }

            testSuite("summaries") {
                test("a summary carries the same member count as the collection it stands for") { repositories ->
                    // The whole point of the summary is that it can be read without the members, so
                    // the count it reports has to be the count the full aggregate would report.
                    // Two code paths reaching one number is the only thing that keeps the Home rail's
                    // "twelve passages" from drifting from the shelf's eleven.
                    val summaries = repositories.collections.findSummaries().associateBy { it.id }
                    assertThat(summaries).hasSize(OfficialCollectionCatalogue.entries.size)

                    repositories.collections.findAll().forEach { collection ->
                        val id = requireNotNull(collection.id)
                        assertThat(summaries[id]?.memberCount)
                            .describedAs("Summary count for collection '${collection.name}'")
                            .isEqualTo(collection.members.size)
                    }
                }

                test("a summary's label, cover and pin stamp match the collection it stands for") { repositories ->
                    val morning = collectionNamed(repositories, "Morning Azkar")
                    repositories.collections.setCoverImage(morning.requireId(), "cover.png")
                    val id = morning.requireId()

                    val summary = repositories.collections.findSummary(id)

                    assertThat(summary?.name).isEqualTo(morning.name)
                    assertThat(summary?.description).isEqualTo(morning.description.orEmpty())
                    assertThat(summary?.iconKey).isEqualTo(morning.iconKey.orEmpty())
                    assertThat(summary?.coverImagePath).isEqualTo("cover.png")
                    assertThat(summary?.homePinnedAt).isEqualTo(morning.homePinnedAt)
                }

                test("findSummary(unknown) is null rather than an empty summary") { repositories ->
                    assertThat(repositories.collections.findSummary(9999L)).isNull()
                }

                test("the rail order puts pinned collections first, most recent first") { repositories ->
                    // The rail is the first thing the app paints, so its order is the one thing a
                    // reader must not have to think about: whatever the user chose to see is there,
                    // and it is there in the order they chose it.
                    val morningId = collectionNamed(repositories, "Morning Azkar").requireId()
                    repositories.collections.setHomePinned(morningId, pinned = true)

                    val summaries = repositories.collections.findSummaries()

                    assertThat(summaries.first().id).isEqualTo(morningId)
                    assertThat(summaries.filter { it.homePinnedAt != null }).hasSize(1)
                    // Everything the user never pinned follows, in a stable order.
                    assertThat(summaries.map { it.id }.subList(1, summaries.size)).isSorted()
                }
            }
        }

        testSuite("custom collections") {
            test("save() inserts, then updates, returning the stored aggregate each time") { repositories ->
                val created = repositories.collections.save(RemembranceCollectionImpl(name = "Before Sleep"))
                val createdId = requireNotNull(created.id)

                assertThat(created.name).isEqualTo("Before Sleep")
                assertThat(created.kind).isEqualTo(CollectionKind.CUSTOM)
                assertThat(created.createdAt).isNotNull()

                val renamed = repositories.collections.save(created.renamed("After Sleep"))

                assertThat(renamed.id).isEqualTo(createdId)
                assertThat(renamed.name).isEqualTo("After Sleep")
                assertThat(repositories.collections.findAll().map { it.name }).contains("After Sleep")
            }

            test("findAll() returns official and custom collections together, ordered by id") { repositories ->
                repositories.collections.save(RemembranceCollectionImpl(name = "Mine"))

                val all = repositories.collections.findAll()

                assertThat(all).hasSize(OfficialCollectionCatalogue.entries.size + 1)
                assertThat(all.map { it.id }).isSorted()
                assertThat(all.first().kind).isEqualTo(CollectionKind.OFFICIAL)
                assertThat(all.last().kind).isEqualTo(CollectionKind.CUSTOM)
            }

            test("findById(null) is absence, not a lookup of id 0") { repositories ->
                assertThat(repositories.collections.findById(null)).isNull()
            }

            test("addMembers() appends in the order given") { repositories ->
                val collection = newCollection(repositories)

                val added = repositories.collections.addMembers(collection, listOf(3L, 1L, 2L))

                assertThat(added).isEqualTo(3)
                assertThat(
                    repositories.collections
                        .findById(collection)
                        ?.members
                        ?.map { it.id },
                ).containsExactly(3L, 1L, 2L)
            }

            test("addMembers() counts only the ids that were not already members") { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.addMembers(collection, listOf(1L, 2L))

                val added = repositories.collections.addMembers(collection, listOf(2L, 3L))

                assertThat(added).describedAs("2 was already there, 3 was not").isEqualTo(1)
                assertThat(
                    repositories.collections
                        .findById(collection)
                        ?.members
                        ?.map { it.id },
                ).containsExactly(1L, 2L, 3L)
            }

            test("addMembers() of nothing is a no-op rather than an error") { repositories ->
                val collection = newCollection(repositories)

                assertThat(repositories.collections.addMembers(collection, emptyList())).isZero()
            }

            test("removeMembers() closes the gap a removal leaves behind") { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.addMembers(collection, listOf(1L, 2L, 3L))

                val removed = repositories.collections.removeMembers(collection, listOf(2L))

                assertThat(removed).isEqualTo(1)
                // The positions must be a dense 0-based run, or the next append would land past the
                // hole and leave it there permanently.
                assertThat(positions(repositories.database, collection)).containsExactly(0, 1)
                assertThat(
                    repositories.collections
                        .findById(collection)
                        ?.members
                        ?.map { it.id },
                ).containsExactly(1L, 3L)
            }

            test("removing an id that is not a member reports zero and changes nothing") { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.addMembers(collection, listOf(1L, 2L))

                val removed = repositories.collections.removeMembers(collection, listOf(3L))

                assertThat(removed).isZero()
                assertThat(
                    repositories.collections
                        .findById(collection)
                        ?.members
                        ?.map { it.id },
                ).containsExactly(1L, 2L)
            }

            test(
                "re-adding a removed member appends it at the end rather than reclaiming its old slot",
            ) { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.addMembers(collection, listOf(1L, 2L, 3L))
                repositories.collections.removeMembers(collection, listOf(1L))

                val added = repositories.collections.addMembers(collection, listOf(1L))

                // A removed row carries no memory of where it was, so it comes back last. The
                // alternative — remembering the slot — would mean a position column that is not a
                // dense rank, which is the one invariant the rest of the reader relies on.
                assertThat(added).isEqualTo(1)
                assertThat(
                    repositories.collections
                        .findById(collection)
                        ?.members
                        ?.map { it.id },
                ).containsExactly(2L, 3L, 1L)
                assertThat(positions(repositories.database, collection)).containsExactly(0, 1, 2)
            }

            test("re-adding an existing member adds nothing and leaves its position alone") { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.addMembers(collection, listOf(1L, 2L))

                val added = repositories.collections.addMembers(collection, listOf(1L))

                assertThat(added).isZero()
                assertThat(positions(repositories.database, collection)).containsExactly(0, 1)
            }

            test("moveMember() renumbers the whole run rather than swapping two rows") { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.addMembers(collection, listOf(1L, 2L, 3L))

                repositories.collections.moveMember(collection, fromIndex = 0, toIndex = 2)

                assertThat(
                    repositories.collections
                        .findById(collection)
                        ?.members
                        ?.map { it.id },
                ).containsExactly(2L, 3L, 1L)
                assertThat(positions(repositories.database, collection)).containsExactly(0, 1, 2)
            }

            test("moveMember() ignores an out-of-range or no-op index") { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.addMembers(collection, listOf(1L, 2L))

                repositories.collections.moveMember(collection, fromIndex = 0, toIndex = 9)
                repositories.collections.moveMember(collection, fromIndex = 1, toIndex = 1)
                repositories.collections.moveMember(collection, fromIndex = -1, toIndex = 0)

                assertThat(
                    repositories.collections
                        .findById(collection)
                        ?.members
                        ?.map { it.id },
                ).containsExactly(1L, 2L)
            }

            test("deleteById() takes the membership with it") { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.addMembers(collection, listOf(1L, 2L))

                repositories.collections.deleteById(collection)

                assertThat(repositories.collections.findById(collection)).isNull()
                assertThat(membershipRowCount(repositories.database, collection)).isZero()
            }

            test("deleteById(null) does nothing at all") { repositories ->
                val before = repositories.collections.findAll().size

                repositories.collections.deleteById(null)

                assertThat(repositories.collections.findAll()).hasSize(before)
            }

            test("a deleted remembrance drops out of the shelf and leaves the cover alone") { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.addMembers(collection, listOf(1L, 2L))
                repositories.collections.setCoverImage(collection, "cover.png")

                repositories.remembrances.deleteById(1L)

                val reloaded = repositories.collections.findById(collection)
                assertThat(reloaded?.members?.map { it.id }).containsExactly(2L)
                assertThat(reloaded?.coverImagePath)
                    .describedAs("The cover is a file of its own now, so no passage can orphan it")
                    .isEqualTo("cover.png")
            }
        }

        testSuite("cover, pin and recency") {
            test("setCoverImage() stores a name, and clearing it takes the name away") { repositories ->
                val collection = newCollection(repositories)

                repositories.collections.setCoverImage(collection, "cover.png")
                assertThat(repositories.collections.findById(collection)?.coverImagePath).isEqualTo("cover.png")

                repositories.collections.setCoverImage(collection, null)
                assertThat(repositories.collections.findById(collection)?.coverImagePath).isNull()
            }

            test("a cover whose file is gone still reads as a name, which is the fallback case") { repositories ->
                // The repository stores the name and nothing else. Whether a file still sits behind it
                // is the cover storage's business, and a name with no file is exactly what a user who
                // deleted the picture leaves behind — a normal row, not a broken one.
                val collection = newCollection(repositories)

                repositories.collections.setCoverImage(collection, "gone.png")

                assertThat(repositories.collections.findById(collection)?.coverImagePath).isEqualTo("gone.png")
            }

            test("setPinned() stamps and clears, and isPinned follows the stamp") { repositories ->
                val collection = newCollection(repositories)

                repositories.collections.setPinned(collection, true)
                assertThat(repositories.collections.findById(collection)?.isPinned).isTrue()

                repositories.collections.setPinned(collection, false)
                assertThat(repositories.collections.findById(collection)?.isPinned).isFalse()
            }

            test("setTags() replaces the labels rather than adding to them") { repositories ->
                val collection = newCollection(repositories)
                repositories.collections.setTags(collection, listOf(TagImpl(name = "Morning"), TagImpl(name = "sleep")))

                assertThat(
                    repositories.collections
                        .findById(collection)
                        ?.tags
                        ?.map { it.name },
                ).containsExactlyInAnyOrder("morning", "sleep")

                repositories.collections.setTags(collection, listOf(TagImpl(name = "travel")))

                assertThat(
                    repositories.collections
                        .findById(collection)
                        ?.tags
                        ?.map { it.name },
                ).containsExactly("travel")
            }

            test("two spellings of one label name make one link rather than a broken write") { repositories ->
                // Names fold to lower case and the folded name is the unique index, so "Sleep",
                // "sleep" and "SLEEP" resolve to the same tag row. Without de-duplicating the
                // resolved ids the composite primary key of collection_tag would be asked to hold
                // one link twice, which is a constraint violation rather than a duplicate chip.
                val collection = newCollection(repositories)

                repositories.collections.setTags(
                    collection,
                    listOf(TagImpl(name = "Sleep"), TagImpl(name = "sleep"), TagImpl(name = "SLEEP")),
                )

                assertThat(repositories.collections.findById(collection)?.tags).hasSize(1)
            }

            test("touchLastOpened() moves last_opened_at but not updated_at") { repositories ->
                val collection = newCollection(repositories)
                val before = repositories.collections.findById(collection)
                assertThat(before?.lastOpenedAt).describedAs("insert() stamps it").isNotNull()

                repositories.collections.touchLastOpened(collection)

                val after = repositories.collections.findById(collection)
                assertThat(after?.lastOpenedAt).isNotNull()
                assertThat(after?.updatedAt)
                    .describedAs("Renaming is an update; opening a shelf is not")
                    .isEqualTo(before?.updatedAt)
            }
        }

        testSuite("missing collections") {
            test("every write to an unknown id reports it rather than silently doing nothing") { repositories ->
                val absent = 9_999L

                assertThat(failureOf { repositories.collections.addMembers(absent, listOf(1L)) })
                    .isInstanceOf(CollectionNotFoundException::class.java)
                assertThat(failureOf { repositories.collections.removeMembers(absent, listOf(1L)) })
                    .isInstanceOf(CollectionNotFoundException::class.java)
                assertThat(failureOf { repositories.collections.setCoverImage(absent, "cover.png") })
                    .isInstanceOf(CollectionNotFoundException::class.java)
                assertThat(failureOf { repositories.collections.setPinned(absent, true) })
                    .isInstanceOf(CollectionNotFoundException::class.java)
                assertThat(failureOf { repositories.collections.setTags(absent, listOf(TagImpl(name = "morning"))) })
                    .isInstanceOf(CollectionNotFoundException::class.java)
                assertThat(failureOf { repositories.collections.moveMember(absent, 0, 1) })
                    .isInstanceOf(CollectionNotFoundException::class.java)
                assertThat(failureOf { repositories.collections.touchLastOpened(absent) })
                    .isInstanceOf(CollectionNotFoundException::class.java)
                assertThat(failureOf { repositories.collections.deleteById(absent) })
                    .isInstanceOf(CollectionNotFoundException::class.java)
            }

            test("the reported id is the one that was asked for") { repositories ->
                assertThat(failureOf { repositories.collections.touchLastOpened(4_242L) })
                    .isInstanceOfSatisfying(CollectionNotFoundException::class.java) { failure ->
                        assertThat(failure.collectionId).isEqualTo(4_242L)
                    }
            }

            test("findById stays quiet about absence") { repositories ->
                assertThat(repositories.collections.findById(9_999L)).isNull()
            }
        }
    }
}

/**
 * The shipped collection with this name.
 *
 * By name rather than by a catalogue key, because the seed writes the shelf once and the user may
 * rename anything on it afterwards. A key would be a handle the tests could hold only as long as
 * nobody edited the thing the key stands for.
 */
private suspend fun collectionNamed(
    repositories: ExposedRepositories,
    name: String,
): RemembranceCollection =
    repositories.collections.findAll().singleOrNull { it.name == name }
        ?: throw AssertionError("No collection named '$name' on the shelf")

private fun RemembranceCollection.requireId(): Long = requireNotNull(id) { "Unsaved collection" }

/** Creates a custom collection and returns its generated id, failing the test if there is none. */
private suspend fun newCollection(repositories: ExposedRepositories): Long =
    requireNotNull(repositories.collections.save(RemembranceCollectionImpl(name = "Mine")).id)

/**
 * The failure [block] raised, or `null` when it did not raise one.
 *
 * AssertJ's `assertThatThrownBy` takes a non-suspending lambda, so it cannot be handed a repository
 * call directly; `runCatching` is the suspending equivalent.
 */
private suspend inline fun failureOf(crossinline block: suspend () -> Unit): Throwable? =
    runCatching { block() }.exceptionOrNull()

/** The `position` values stored for [collectionId], read raw so the numbering itself is asserted. */
private fun positions(
    database: Database,
    collectionId: Long,
): List<Int> =
    transaction(database) {
        CollectionMemberTable
            .selectAll()
            .where { CollectionMemberTable.collectionId eq collectionId }
            .orderBy(CollectionMemberTable.position to SortOrder.ASC)
            .map { it[CollectionMemberTable.position] }
    }

/** The number of stored membership rows belonging to [collectionId]. */
private fun membershipRowCount(
    database: Database,
    collectionId: Long,
): Int =
    transaction(database) {
        CollectionMemberTable
            .selectAll()
            .where { CollectionMemberTable.collectionId eq collectionId }
            .count()
            .toInt()
    }
