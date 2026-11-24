package com.azkar.ui.library

import com.azkar.domain.model.LocalizedPair
import com.azkar.ui.library.model.TagOption
import com.azkar.ui.library.model.TranslationEntry
import com.azkar.ui.library.model.missingLanguages
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * The tags a dialog carries, now that they are chips rather than a comma-separated field.
 *
 * The behaviour that used to live here — splitting on commas and de-duplicating ignoring case — has
 * moved from parse time to press time, and that is the whole point of the picker: storage folds names
 * to lowercase before the unique index sees them, so "Sleep" and "sleep" are one tag wearing two hats,
 * and the only way to honour that is never to hold both at once.
 */
val CollectionEditorDraftSuite by testSuite("The tags a dialog carries are the ones the user pressed") {

    test("a pressed tag is submitted, and an unpressed one is not") {
        val draft =
            CollectionEditorDraft.create().apply {
                toggleTag("morning")
                toggleTag("sleep")
            }

        assertThat(draft.submittedTagNames).containsExactly("morning", "sleep")
    }

    test("pressing a lit tag again takes it back off") {
        val draft =
            CollectionEditorDraft.create().apply {
                toggleTag("morning")
                toggleTag("sleep")
                toggleTag("morning")
            }

        assertThat(draft.submittedTagNames).containsExactly("sleep")
    }

    test("two spellings of one tag never both survive") {
        val draft =
            CollectionEditorDraft.create().apply {
                toggleTag("Sleep")
                toggleTag("sleep")
            }

        assertThat(draft.submittedTagNames).isEmpty()
    }

    test("a blank press changes nothing") {
        val draft =
            CollectionEditorDraft.create().apply {
                toggleTag("morning")
                toggleTag("   ")
            }

        assertThat(draft.submittedTagNames).containsExactly("morning")
    }

    test("a tag typed around its spaces is trimmed on the way in") {
        val draft = CollectionEditorDraft.create().apply { toggleTag("  after prayer  ") }

        assertThat(draft.submittedTagNames).containsExactly("after prayer")
    }

    test("a fresh collection starts with nothing") {
        val draft = CollectionEditorDraft.create()

        assertThat(draft.submittedTagNames).isEmpty()
    }

    test("a rename carries the collection's own tags in, folded and alphabetised") {
        val collection =
            customCollection(
                id = 1L,
                name = "Morning Azkar",
                tags = taggedSet("Travel", "Sleep", "morning"),
            )

        val draft = CollectionEditorDraft.edit(collection)

        assertThat(draft.submittedTagNames).containsExactly("morning", "sleep", "travel")
    }

    test("a duplicate starts with no tags, because it inherits the name and nothing else") {
        val collection = customCollection(id = 1L, name = "Morning Azkar", tags = taggedSet("sleep"))

        val draft = CollectionEditorDraft.duplicate(collection, newName = "Morning Azkar (copy)")

        assertThat(draft.submittedTagNames).isEmpty()
    }

    test("a typed tag joins the submitted ones and clears the field") {
        val draft = CollectionEditorDraft.create().apply { updateNewTagName("commute") }

        assertThat(draft.addTag(draft.newTagName)).isTrue()
        assertThat(draft.submittedTagNames).containsExactly("commute")
        assertThat(draft.newTagName).isEmpty()
    }

    test("a tag the collection already carries is not added twice") {
        val draft =
            CollectionEditorDraft.create().apply {
                toggleTag("Sleep")
                updateNewTagName("sleep")
            }

        assertThat(draft.newTagAcceptable).isFalse()
        assertThat(draft.addTag(draft.newTagName)).isFalse()
        assertThat(draft.submittedTagNames).containsExactly("Sleep")
    }

    test("a blank name is not an acceptable tag") {
        val draft = CollectionEditorDraft.create().apply { updateNewTagName("   ") }

        assertThat(draft.newTagAcceptable).isFalse()
    }

    test("the options are every tag the library has, in the order the catalogue gave them") {
        val draft = CollectionEditorDraft.create()

        val options = draft.tagOptions(listOf("morning", "sleep", "travel").map(TagOption::of))

        assertThat(options.map { it.value }).containsExactly("morning", "sleep", "travel")
    }

    test("a query narrows the options to what matches it") {
        val draft = CollectionEditorDraft.create().apply { updateTagQuery("tra") }

        val options = draft.tagOptions(listOf("morning", "sleep", "travel").map(TagOption::of))

        assertThat(options.map { it.value }).containsExactly("travel")
    }

    test("a chosen tag the library has never heard of is still an option") {
        val draft = CollectionEditorDraft.create().apply { addTag("commute") }

        val options = draft.tagOptions(listOf("morning", "sleep").map(TagOption::of))

        assertThat(options.map { it.value }).containsExactly("morning", "sleep", "commute")
    }

    test("a query never hides a tag the user has already chosen") {
        val draft =
            CollectionEditorDraft.create().apply {
                toggleTag("morning")
                updateTagQuery("travel")
            }

        val options = draft.tagOptions(listOf("morning", "sleep", "travel").map(TagOption::of))

        assertThat(options.map { it.value }).containsExactly("morning", "travel")
    }

    test("an empty query is no filter at all") {
        val draft = CollectionEditorDraft.create().apply { updateTagQuery("  ") }

        assertThat(draft.tagOptions(listOf("morning", "sleep").map(TagOption::of)).map { it.value })
            .containsExactly("morning", "sleep")
    }

    test("a search that matches nothing offers nothing") {
        val draft = CollectionEditorDraft.create().apply { updateTagQuery("commute") }

        assertThat(draft.tagOptions(listOf("morning", "sleep").map(TagOption::of))).isEmpty()
    }

    test("an option says the tag's translated name and still means the stored one") {
        val draft = CollectionEditorDraft.create()

        val options = draft.tagOptions(listOf(TagOption(value = "morning", label = "Morning Azkar")))

        assertThat(options.map { it.label }).containsExactly("Morning Azkar")
        assertThat(options.map { it.value }).containsExactly("morning")
    }

    test("choosing a translated option stores the stored name, not the label") {
        // The whole reason the option carries two strings. The chip reads "Morning Azkar" and the
        // picker hands the draft its `value`, so what lands in the draft — and what a save writes
        // against `uq_tag__name` — is `morning`. Saving the label would create a second tag meaning
        // the same thing, which is the fork the two fields exist to prevent.
        val draft = CollectionEditorDraft.create()
        val option = TagOption(value = "morning", label = "Morning Azkar")

        draft.toggleTag(option.value)

        assertThat(draft.tags).containsExactly("morning")
        assertThat(draft.isTagSelected(option.value)).isTrue()
    }

    test("a chosen tag is selected by its stored name even when the chip shows another") {
        // The other half of the same rule, and the one that is easy to get backwards: selection is
        // tested against the value too, so an option the draft already holds still draws as chosen
        // rather than as a chip that appears to add a second copy of a tag the draft has.
        val draft = CollectionEditorDraft.create().apply { toggleTag("morning") }

        val options =
            draft.tagOptions(listOf(TagOption(value = "morning", label = "أذكار الصباح")))

        assertThat(options.map { it.value }).containsExactly("morning")
        assertThat(draft.isTagSelected(options.single().value)).isTrue()
    }

    test("a query finds a tag by the name it is shown as") {
        val draft = CollectionEditorDraft.create().apply { updateTagQuery("Azkar") }

        val options = draft.tagOptions(listOf(TagOption(value = "morning", label = "Morning Azkar")))

        assertThat(options.map { it.value }).containsExactly("morning")
    }

    test("a query finds a tag by the name it is stored as, even once translated") {
        val draft = CollectionEditorDraft.create().apply { updateTagQuery("morning") }

        val options = draft.tagOptions(listOf(TagOption(value = "morning", label = "أذكار الصباح")))

        assertThat(options.map { it.value }).containsExactly("morning")
    }

    test("two options differing only in case are one option") {
        val draft = CollectionEditorDraft.create()

        val options =
            draft.tagOptions(
                listOf(
                    TagOption(value = "Morning", label = "Morning Azkar"),
                    TagOption(value = "morning", label = "morning"),
                ),
            )

        assertThat(options).hasSize(1)
        assertThat(options.single().label).isEqualTo("Morning Azkar")
    }
}

/**
 * Tags named in the order given, with ids to match.
 *
 * The fixture's own [tag] is used rather than a second fake so that these tests exercise the same
 * folding the storage layer does — if `TagImpl` ever stopped folding, the de-duplication assertions
 * above would start failing here rather than in production.
 */
private fun taggedSet(vararg names: String): Set<com.azkar.domain.model.Tag> =
    names.mapIndexed { index, name -> tag(id = index.toLong() + 1L, name = name) }.toSet()

/**
 * The translations a dialog is carrying.
 *
 * A translation is a second language for one row, which makes it the only piece of the editor with
 * three states rather than two: absent, present but blank, and present. The blank one is the
 * dangerous middle — a user who opened an Arabic box and changed their mind has left an empty field,
 * and writing that as a translation would put a nameless row in storage that then resolves to nothing
 * on every future read.
 *
 * The rename rule is the other half. A translation is a claim about a *name*, so when the name it was
 * written against changes, a translation nobody has touched is no longer a claim about anything. The
 * rule here is that an untouched box goes and a rewritten one stays, and that it does not matter
 * which of the two the user did first.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionEditorTranslationsSuite by testSuite("The languages a dialog carries alongside the name") {
    val arabic = Locale.forLanguageTag("ar")
    val english = Locale.ENGLISH

    /** A shelf as the editor opens it: a stored name and whatever wording is already attached. */
    fun editing(
        name: String = "My shelf",
        translations: Map<Locale, LocalizedPair> = emptyMap(),
    ) = customCollection(id = 1L, name = name, description = "Mine", translations = translations)

    testSuite("adding a language") {
        test("a language nobody has named yet can be added") {
            val draft = CollectionEditorDraft.create().apply { addTranslation(arabic) }

            assertThat(draft.hasTranslationFor(arabic)).isTrue()
            assertThat(draft.translationFor(arabic)?.name).isEmpty()
        }

        test("the same language cannot be added twice") {
            val draft =
                CollectionEditorDraft.create().apply {
                    addTranslation(arabic)
                    addTranslation(arabic)
                }

            assertThat(draft.translations).hasSize(1)
        }

        test("a regional tag is refused once the language it belongs to is in") {
            val draft =
                CollectionEditorDraft.create().apply {
                    addTranslation(arabic)
                    addTranslation(Locale.forLanguageTag("ar-EG"))
                }

            // `ar-EG` and `ar` are one language as far as a reader is concerned, and two boxes for it
            // would be two answers to one question.
            assertThat(draft.translations).hasSize(1)
        }

        test("removing a language takes its box with it") {
            val draft =
                CollectionEditorDraft.create().apply {
                    addTranslation(arabic)
                    removeTranslation(arabic)
                }

            assertThat(draft.hasTranslationFor(arabic)).isFalse()
        }

        test("removing a language nobody added is harmless") {
            val draft =
                CollectionEditorDraft.create().apply {
                    addTranslation(arabic)
                    removeTranslation(english)
                }

            assertThat(draft.translations).hasSize(1)
        }
    }

    testSuite("what gets submitted") {
        test("a language that was typed into is submitted") {
            val draft =
                CollectionEditorDraft.create().apply {
                    updateName("Quiet hours")
                    addTranslation(arabic)
                    updateTranslationName(arabic, "ساعة الهدوء")
                }

            assertThat(draft.submittedTranslations)
                .containsEntry(arabic, LocalizedPair("ساعة الهدوء"))
        }

        test("a language that was opened and left empty is not submitted") {
            val draft =
                CollectionEditorDraft.create().apply {
                    updateName("Quiet hours")
                    addTranslation(arabic)
                }

            // The empty box is the state that must never reach storage: a row with no name in it
            // resolves to nothing, and would then be counted as a translation the collection has.
            assertThat(draft.submittedTranslations).isEmpty()
        }

        test("a sentence typed under a name is submitted with it") {
            val draft =
                CollectionEditorDraft.create().apply {
                    addTranslation(arabic)
                    updateTranslationName(arabic, "ساعة الهدوء")
                    updateTranslationDescription(arabic, "أذكار آخر ساعة من الليل")
                }

            assertThat(draft.submittedTranslations[arabic]?.description)
                .isEqualTo("أذكار آخر ساعة من الليل")
        }

        test("a sentence typed and then cleared is submitted as no sentence rather than a blank one") {
            val draft =
                CollectionEditorDraft.create().apply {
                    addTranslation(arabic)
                    updateTranslationName(arabic, "ساعة الهدوء")
                    updateTranslationDescription(arabic, "أذكار آخر ساعة")
                    updateTranslationDescription(arabic, "   ")
                }

            assertThat(draft.submittedTranslations[arabic]?.description).isNull()
        }

        test("both halves of a name are trimmed") {
            val draft =
                CollectionEditorDraft.create().apply {
                    addTranslation(arabic)
                    updateTranslationName(arabic, "  ساعة الهدوء  ")
                }

            assertThat(draft.submittedTranslations[arabic]?.name).isEqualTo("ساعة الهدوء")
        }
    }

    testSuite("opening an editor on a shelf that already has translations") {
        test("the wording it already carries is offered back rather than asked for again") {
            val draft =
                CollectionEditorDraft.edit(
                    editing(translations = mapOf(arabic to LocalizedPair("ساعة الهدوء", "أذكار الليل"))),
                )

            assertThat(draft.translationFor(arabic)?.name).isEqualTo("ساعة الهدوء")
            assertThat(draft.translationFor(arabic)?.description).isEqualTo("أذكار الليل")
        }

        test("a shelf with no translations opens with none") {
            assertThat(CollectionEditorDraft.edit(editing()).translations).isEmpty()
        }

        test("a shelf whose only translation carries no sentence offers the name and no sentence") {
            val draft =
                CollectionEditorDraft.edit(editing(translations = mapOf(arabic to LocalizedPair("ساعة الهدوء"))))

            assertThat(draft.translationFor(arabic)?.name).isEqualTo("ساعة الهدوء")
            assertThat(draft.translationFor(arabic)?.description).isNull()
        }

        test("a new shelf is not seeded with the wording of whatever was open before") {
            // The editor is rebuilt from the collection every time it opens, so a stale list can only
            // arrive from a draft that outlived the collection — and must not survive into a new one.
            assertThat(CollectionEditorDraft.create().translations).isEmpty()
        }
    }

    testSuite("renaming") {
        test("a translation the user rewrote survives the same rename") {
            val draft =
                CollectionEditorDraft
                    .edit(
                        editing(
                            name = "Evening light",
                            translations = mapOf(arabic to LocalizedPair("ضوء المساء")),
                        ),
                    ).apply {
                        updateName("Twilight")
                        updateTranslationName(arabic, "الغسق")
                    }

            // Rewriting the box is the user saying the translation was for the old name and is now
            // for the new one, which is the only reading under which renaming a shelf in one language
            // leaves the other language claiming the wrong word.
            assertThat(draft.submittedTranslations[arabic]?.name).isEqualTo("الغسق")
        }

        test("an untouched translation is dropped once the name it described is gone") {
            val draft =
                CollectionEditorDraft
                    .edit(
                        editing(
                            name = "Evening light",
                            translations = mapOf(arabic to LocalizedPair("ضوء المساء")),
                        ),
                    ).apply { updateName("Twilight") }

            // The name moved and the box did not. Nothing has been said about "Twilight" in this
            // language, and carrying the old wording over would be a claim nobody made — and a
            // confidently wrong one rather than a visibly absent one.
            assertThat(draft.submittedTranslations).isEmpty()
        }

        test("renaming one language's box leaves the other language's wording alone") {
            val draft =
                CollectionEditorDraft
                    .edit(
                        editing(
                            name = "Evening light",
                            translations = mapOf(arabic to LocalizedPair("ضوء المساء")),
                        ),
                    ).apply {
                        updateName("Twilight")
                        addTranslation(english)
                        updateTranslationName(english, "Twilight light")
                    }

            // The untouched Arabic box is dropped and the typed English one is kept, which is the
            // whole rule: the user has claimed the new name in one language and not in the other.
            assertThat(draft.submittedTranslations).containsOnlyKeys(english)
        }

        test("a rename that changes nothing drops nothing") {
            val draft =
                CollectionEditorDraft
                    .edit(
                        editing(
                            name = "Evening light",
                            translations = mapOf(arabic to LocalizedPair("ضوء المساء")),
                        ),
                    ).apply { updateName("  Evening light  ") }

            // Trimming is not a rename. A shelf that was merely re-saved must not lose its wording
            // because the field round-tripped through the same characters.
            assertThat(draft.submittedTranslations[arabic]?.name).isEqualTo("ضوء المساء")
        }
    }

    test("a duplicate only starts from the name and never carries the source's wording") {
        val draft =
            CollectionEditorDraft.duplicate(
                customCollection(
                    id = 2L,
                    name = "Evening light",
                    translations = mapOf(arabic to LocalizedPair("ضوء المساء")),
                ),
                newName = "Evening light copy",
            )

        // A duplicate is a new shelf under a new name. Inheriting the old one's Arabic would say
        // something about a shelf that does not exist yet, in a language the user cannot read.
        assertThat(draft.name).isEqualTo("Evening light copy")
        assertThat(draft.translations).isEmpty()
    }
}

/**
 * Which languages the translation panel offers, which is a question about the *name* and not about the
 * screen it is being edited on.
 *
 * The centre of these is the reported case: a collection named in English, opened from an Arabic
 * interface. Every version of this that asked the interface the question offered a second English box
 * for a name that was already English and offered nothing for Arabic, which is the language the reader
 * was actually looking at.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val MissingLanguagesSuite by testSuite("Which languages a name can still be written in") {
    val arabic = Locale.forLanguageTag("ar")
    val english = Locale.ENGLISH

    test("a name marked English is missing Arabic, whoever is asking") {
        val offered = missingLanguages(existing = emptyList(), uiLocale = arabic, nameLocale = english)

        assertThat(offered.map { it.language }).containsExactly("ar")
    }

    test("the interface language is the fallback when no name language is recorded") {
        assertThat(missingLanguages(emptyList(), english).map { it.language }).containsExactly("ar")
        assertThat(missingLanguages(emptyList(), arabic).map { it.language }).containsExactly("en")
    }

    test("switching the interface does not change the language recorded for a name") {
        assertThat(missingLanguages(emptyList(), english, nameLocale = english))
            .isEqualTo(missingLanguages(emptyList(), arabic, nameLocale = english))
    }

    test("a name marked Arabic is missing English, whoever is asking") {
        val offered =
            missingLanguages(existing = emptyList(), uiLocale = english, nameLocale = arabic)

        assertThat(offered.map { it.language }).containsExactly("en")
    }

    test("a language that already has a box is not offered a second one") {
        val offered =
            missingLanguages(
                existing = listOf(TranslationEntry(locale = arabic, name = "النوم")),
                uiLocale = english,
                nameLocale = english,
            )

        assertThat(offered).isEmpty()
    }

    test("a box held open but never filled still counts as a language that exists") {
        // The block is on screen, so a second field for the same language would be two boxes to keep
        // in step. Dropping it is the draft's job; this is only about not offering it again.
        val offered =
            missingLanguages(
                existing = listOf(TranslationEntry(locale = arabic)),
                uiLocale = arabic,
                nameLocale = english,
            )

        assertThat(offered).isEmpty()
    }

    test("a language is recognised by its language, not by the tag that named it") {
        val offered =
            missingLanguages(
                existing = listOf(TranslationEntry(locale = Locale.forLanguageTag("ar-EG"), name = "النوم")),
                uiLocale = english,
                nameLocale = english,
            )

        assertThat(offered).isEmpty()
    }
}
