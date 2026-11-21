package com.azkar.data.seed

import com.azkar.data.repo.exposed.ExposedCollectionRepository
import com.azkar.data.repo.exposed.ExposedTagRepository
import com.azkar.data.table.TagTable
import com.azkar.data.transaction.ExposedTransactionRunner
import com.azkar.domain.model.LocalizedPair
import com.azkar.domain.model.Tag
import com.azkar.domain.model.impl.RemembranceCollectionImpl
import com.azkar.domain.model.impl.TagImpl
import com.azkar.domain.model.renamed
import com.azkar.testUtils.TestDatabase
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.Locale

/**
 * The wording a shipped shelf and a shipped label carry in every language the app ships.
 *
 * Two halves that fail in opposite ways. The catalogue half is checked before a database exists at
 * all: a catalogue entry and the tag vocabulary beside it are two hand-written tables that have to
 * agree, and nothing at runtime would ever notice when they stop — the shelf simply reads its
 * canonical English forever, which is the whole complaint this feature exists to fix.
 *
 * The seeded half is checked against a real database because that is the only place the wording is
 * actually observable: that the first launch of an empty database gives every shipped shelf and every
 * shipped label its rows, and that it gives nothing at all to a label somebody typed.
 *
 * The catalogue tests are deliberately pure. They are the cheapest thing here to run and the only ones
 * that catch a mistake before it has been written to a single row.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val TranslationWordingSuite by testSuite("What the shipped shelves and labels are called in every language") {
    testSuite("the catalogue and the tag vocabulary agree") {
        test("every catalogue tag is a slug the vocabulary can word") {
            val unwordable =
                OfficialCollectionCatalogue.entries
                    .mapNotNull { it.tagName }
                    .map(Tag::fold)
                    .filterNot { it in OfficialTagVocabulary.slugs }

            // A shelf seeded from a tag nobody has wording for still loads and still renders; it just
            // stays English in an Arabic app. Nothing else in the system would raise it.
            assertThat(unwordable).describedAs("A shipped shelf whose label cannot be translated").isEmpty()
        }

        test("every shipped shelf has both a name and a sentence in every shipped language") {
            val thin =
                OfficialCollectionCatalogue.entries
                    .flatMap { entry ->
                        OfficialTagVocabulary.shippedLocales().map { locale ->
                            val pair = entry.translations[locale.toLanguageTag()]
                            if (pair == null || pair.name.isBlank() || pair.description.isNullOrBlank()) {
                                "${entry.name} in ${locale.toLanguageTag()}"
                            } else {
                                null
                            }
                        }
                    }.filterNotNull()

            // A name with no sentence is a card with a blank half, and a sentence with no name is
            // unreachable: the resolver will not show prose for a language that has no name for it.
            assertThat(thin).describedAs("A shipped shelf with an untranslated half").isEmpty()
        }

        test("no two shipped shelves are called the same thing in one language") {
            val clashes =
                OfficialTagVocabulary.shippedLocales().flatMap { locale ->
                    val tag = locale.toLanguageTag()
                    OfficialCollectionCatalogue.entries
                        .groupBy { it.translations[tag]?.name }
                        .filter { (name, entries) -> name != null && entries.size > 1 }
                        .map { (name, entries) -> "$name -> ${entries.map { it.name }}" }
                }

            // Two shelves reading identically is not a translation problem, it is a navigation one:
            // the user cannot tell them apart on the shelf at all.
            assertThat(clashes).isEmpty()
        }

        test("every slug in the vocabulary is a tag the shipped dataset actually uses") {
            // The mirror of the catalogue check, failing the other way: a slug nothing is filed under
            // is wording nobody will ever read.
            withLibrary { seeded ->
                val datasetTags = seeded.tagNames()

                assertThat(OfficialTagVocabulary.slugs)
                    .describedAs("Wording for a tag no row carries")
                    .isSubsetOf(datasetTags)
            }
        }

        test("wording a slug folds before looking it up, so casing cannot lose it") {
            assertThat(OfficialTagVocabulary.wordingFor("  Morning  "))
                .isEqualTo(OfficialTagVocabulary.wordingFor("morning"))
        }

        test("a slug the vocabulary does not carry has no wording rather than a guess") {
            assertThat(OfficialTagVocabulary.wordingFor("communion prep")).isEmpty()
        }
        test("every shipped shelf carries its wording in every shipped language") {
            withLibrary { seeded ->
                OfficialCollectionCatalogue.entries.forEach { entry ->
                    val shelf = seeded.collectionNamed(entry)
                    val expected = entry.translations

                    assertThat(shelf.translations.keys.map { it.toLanguageTag() })
                        .describedAs("${entry.name} is missing a shipped language")
                        .containsAll(expected.keys)
                    expected.forEach { (tag, pair) ->
                        assertThat(shelf.translations[Locale.forLanguageTag(tag)]).isEqualTo(pair)
                    }
                }
            }
        }

        test("every shipped label carries its wording too") {
            withLibrary { seeded ->
                OfficialTagVocabulary.slugs.forEach { slug ->
                    val tag = requireNotNull(seeded.tags.findByNameIgnoreCase(slug)) { "No tag named $slug" }

                    assertThat(tag.translations)
                        .describedAs("Wording for the '$slug' label")
                        .isEqualTo(
                            OfficialTagVocabulary
                                .wordingFor(slug)
                                .mapKeys { (tag, _) -> Locale.forLanguageTag(tag) },
                        )
                }
            }
        }

        test("a label the user typed gets no wording, because an arbitrary word has no right answer") {
            withLibrary { seeded ->
                seeded.tags.save(TagImpl(name = "Communion prep"))

                assertThat(requireNotNull(seeded.tags.findByNameIgnoreCase("Communion prep")).translations).isEmpty()
            }
        }
    }

    testSuite("renaming gives the stored wording up") {
        test("a label's wording goes when the label is renamed") {
            withLibrary { seeded ->
                val morning = requireNotNull(seeded.tags.findByNameIgnoreCase("morning"))
                assertThat(morning.translations).isNotEmpty()

                // No translations handed back: this is what the tag dialog sends after a rename, having
                // noticed the user changed the name. The words were written for the old name.
                seeded.tags.save(TagImpl(id = morning.id, name = "Dawn"))

                assertThat(requireNotNull(seeded.tags.findByNameIgnoreCase("dawn")).translations).isEmpty()
            }
        }

        test("wording handed back with the new name is written, because the user asked for it") {
            withLibrary { seeded ->
                val morning = requireNotNull(seeded.tags.findByNameIgnoreCase("morning"))

                seeded.tags.save(
                    TagImpl(
                        id = morning.id,
                        name = "Dawn",
                        translations = mapOf(Locale.forLanguageTag("ar") to "الفجر"),
                    ),
                )

                assertThat(requireNotNull(seeded.tags.findByNameIgnoreCase("dawn")).translations)
                    .isEqualTo(mapOf(Locale.forLanguageTag("ar") to "الفجر"))
            }
        }

        test("an edit that does not rename a label keeps its wording") {
            withLibrary { seeded ->
                val morning = requireNotNull(seeded.tags.findByNameIgnoreCase("morning"))
                val translations = morning.translations

                seeded.tags.save(TagImpl(id = morning.id, name = "Morning", translations = translations))

                assertThat(
                    requireNotNull(seeded.tags.findByNameIgnoreCase("morning")).translations,
                ).isEqualTo(translations)
            }
        }

        test("casing alone is not a rename, because folding is what the tag's identity is") {
            withLibrary { seeded ->
                val morning = requireNotNull(seeded.tags.findByNameIgnoreCase("morning"))
                val translations = morning.translations

                seeded.tags.save(TagImpl(id = morning.id, name = "  MORNING  ", translations = translations))

                assertThat(
                    requireNotNull(seeded.tags.findByNameIgnoreCase("morning")).translations,
                ).isEqualTo(translations)
            }
        }
    }

    testSuite("wording survives a round trip through the repository") {
        test("a shelf's translations are read back exactly as written") {
            withLibrary { seeded ->
                val written =
                    mapOf(
                        Locale.forLanguageTag("ar") to LocalizedPair("أذكار الصباح", "أذكار لبداية اليوم"),
                        Locale.forLanguageTag("fr") to LocalizedPair("Adkar du matin"),
                    )
                val id =
                    requireNotNull(
                        seeded.collections
                            .save(
                                RemembranceCollectionImpl(
                                    name = "Morning Azkar",
                                    description = "Mine",
                                    translations = written,
                                ),
                            ).id,
                    )

                val reloaded = requireNotNull(seeded.collections.findById(id))
                assertThat(reloaded.translations).isEqualTo(written)
                // A translation that stopped at the name stays that way rather than being filled in
                // with the canonical sentence, which is not a translation of anything in French.
                assertThat(reloaded.translations[Locale.forLanguageTag("fr")]?.description).isNull()
            }
        }

        test("saving with no translations clears the ones that were there") {
            withLibrary { seeded ->
                val original = seeded.collectionNamed(OfficialCollectionCatalogue.entries.first())
                assertThat(original.translations).isNotEmpty()

                // The user going back and deleting every translation has to mean they are gone, not
                // that the next read fills them in again.
                seeded.collections.save(original.renamed(original.name, translations = emptyMap()))

                assertThat(requireNotNull(seeded.collections.findById(original.id)).translations).isEmpty()
            }
        }

        test("an edit that does not rename a shelf keeps its wording") {
            withLibrary { seeded ->
                val original = seeded.collectionNamed(OfficialCollectionCatalogue.entries.first())
                val translations = original.translations

                seeded.collections.save(original.renamed(original.name, description = "Edited"))

                assertThat(
                    requireNotNull(seeded.collections.findById(original.id)).translations,
                ).isEqualTo(translations)
            }
        }

        test("a summary carries the same wording the full shelf does") {
            withLibrary { seeded ->
                val original = seeded.collectionNamed(OfficialCollectionCatalogue.entries.first())

                // The home rail reads summaries, not collections. Wording that reached only one of the
                // two would leave one shelf with two names in two places.
                val summary = requireNotNull(seeded.collections.findSummary(requireNotNull(original.id)))
                assertThat(summary.translations).isEqualTo(original.translations)
                assertThat(summary.displayName(Locale.forLanguageTag("ar")))
                    .isEqualTo(original.displayName(Locale.forLanguageTag("ar")))
            }
        }

        test("a label attached to a shelf travels with it") {
            withLibrary { seeded ->
                val entry = OfficialCollectionCatalogue.entries.first()
                val label = requireNotNull(seeded.collectionNamed(entry).tags.singleOrNull())

                // The label chips and the tag picker read labels off the collections rather than off
                // the tag table, so this is the read path that puts wording on those chips.
                assertThat(label.displayName(Locale.forLanguageTag("ar")))
                    .isEqualTo(OfficialTagVocabulary.wordingFor("morning")["ar"])
            }
        }
    }
}

/** A migrated database with the shipped dataset and shelves in it, plus repositories over it. */
private class SeededLibrary(
    val database: Database,
    val collections: ExposedCollectionRepository,
    val tags: ExposedTagRepository,
) {
    /** The shelf a catalogue entry produced, matched by its canonical name. */
    suspend fun collectionNamed(entry: OfficialCollectionCatalogue.Entry) =
        requireNotNull(collections.findAll().singleOrNull { it.name == entry.name }) {
            "The seed did not produce ${entry.name}"
        }

    /** Every tag row's folded name, which is the set a vocabulary slug can resolve against. */
    fun tagNames(): Set<String> =
        transaction(database) { TagTable.selectAll().mapTo(mutableSetOf()) { Tag.fold(it[TagTable.name]) } }
}

/**
 * Runs [block] against a database the real loader has already seeded.
 *
 * The dataset goes first for the same reason the bootstrapper loads it first: the shelves resolve
 * their membership through the labels the dataset creates, so a shelf seeded into a database with no
 * labels in it would come up permanently empty.
 */
private suspend fun withLibrary(block: suspend (SeededLibrary) -> Unit) {
    val database = TestDatabase.bootstrapWithH2()
    transaction(database) {
        val seeded = SeedDataLoader.load()
        CollectionSeed.load(seeded.remembranceIds)
    }
    val transactions = ExposedTransactionRunner(database)
    val tags = ExposedTagRepository(transactions)
    block(SeededLibrary(database, ExposedCollectionRepository(transactions, tags), tags))
}
