package com.azkar.domain.model

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.util.Locale

/**
 * Reading a name or a tag out in the reader's language.
 *
 * These three resolvers are the contract the whole feature rests on, so the tests are shaped around
 * the promises rather than around the code: a translation for the reader's language is found, a
 * translation for a neighbouring *region* of it is found, and — the promise that costs the most and
 * is easiest to break — when nothing matches, what comes back is the row's own text and never a
 * language the reader did not ask for.
 *
 * The last one is what keeps the app honest. A fallback that borrowed from any other available
 * language would show a reader Arabic words they did not write, under a name they cannot edit from
 * that screen, and would look like a mistranslation rather than like the absence of one.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val LocalizedPairSuite by testSuite("Reading a name out in the reader's language") {
    val english = Locale.ENGLISH
    val arabic = Locale.forLanguageTag("ar")

    // A shelf the user named themselves. Nothing but the canonical text, in one language.
    val canonicalOnly =
        mapOf<Locale, LocalizedPair>(
            english to LocalizedPair("Morning Azkar", "Remembrance for the start of the day"),
        )

    // The same shelf, now carried in both languages.
    val bilingual =
        mapOf<Locale, LocalizedPair>(
            english to LocalizedPair("Morning Azkar", "Remembrance for the start of the day"),
            arabic to LocalizedPair("أذكار الصباح", "أذكار لبداية اليوم"),
        )

    testSuite("the model insists on") {
        test("a translated name is not blank") {
            assertThatThrownBy { LocalizedPair("   ") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("blank")
        }

        test("a description is optional, because a translator may stop at the name") {
            assertThat(LocalizedPair("Nature").description).isNull()
        }
    }

    testSuite("finding the name") {
        test("an exact language match is used") {
            assertThat(bilingual.displayName(english, "fallback")).isEqualTo("Morning Azkar")
            assertThat(bilingual.displayName(arabic, "fallback")).isEqualTo("أذكار الصباح")
        }

        test("a neighbouring country of the same language is close enough") {
            val egypt = Locale.forLanguageTag("ar-EG")
            assertThat(bilingual.displayName(egypt, "fallback")).isEqualTo("أذكار الصباح")
        }

        test("the language check ignores case") {
            val shouted = Locale.forLanguageTag("AR")
            assertThat(bilingual.displayName(shouted, "fallback")).isEqualTo("أذكار الصباح")
        }
    }

    testSuite("what happens when nothing matches") {
        test("an untranslated row reads the same in every language") {
            // The whole point for a shelf the user built: their own words, unaltered.
            assertThat(canonicalOnly.displayName(arabic, "Morning Azkar")).isEqualTo("Morning Azkar")
        }

        test("no translation at all still yields the row's own name") {
            assertThat(emptyMap<Locale, LocalizedPair>().displayName(english, "Morning Azkar"))
                .isEqualTo("Morning Azkar")
        }

        test("a row translated only into another language does not leak it into this one") {
            val frenchReader = Locale.FRENCH
            assertThat(canonicalOnly.displayName(frenchReader, "Morning Azkar"))
                .isEqualTo("Morning Azkar")
        }

        test("a translated row in the wrong language falls back rather than borrowing") {
            // Arabic reader, only-Arabic data is impossible here; this is the mirror: an
            // English reader must not be handed the Arabic name when only the Arabic exists.
            val arabicOnly = mapOf<Locale, LocalizedPair>(arabic to LocalizedPair("أذكار الصباح"))
            assertThat(arabicOnly.displayName(english, "Morning Azkar")).isEqualTo("Morning Azkar")
        }

        test("a blank translated name cannot be constructed, so it cannot reach the floor") {
            // Pinned at the type rather than through the resolver: the guard in `displayName` is
            // unreachable, and a test that built a blank pair to exercise it would be a lie.
            assertThatThrownBy { LocalizedPair("") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    testSuite("finding the prose under it") {
        test("it follows the same chain as the name") {
            assertThat(
                bilingual.displayDescription(english, "canonical"),
            ).isEqualTo("Remembrance for the start of the day")
            assertThat(bilingual.displayDescription(arabic, "canonical")).isEqualTo("أذكار لبداية اليوم")
        }

        test("a neighbouring country is close enough") {
            assertThat(bilingual.displayDescription(Locale.forLanguageTag("ar-EG"), "canonical"))
                .isEqualTo("أذكار لبداية اليوم")
        }

        test("a name translated without prose leaves nothing under it, rather than the canonical prose") {
            // Printing English prose under an Arabic name reads as a broken translation. Leaving the
            // space out reads as a translator who stopped at the name, which is what happened.
            val nameOnly = mapOf<Locale, LocalizedPair>(arabic to LocalizedPair("أذكار الصباح"))
            assertThat(nameOnly.displayDescription(arabic, "Remembrance for the start of the day")).isNull()
        }

        test("a row translated into another language only keeps its own prose for that language") {
            // The mirror of the case above: the same Arabic-only row read in English has no English
            // translation, so it is an untranslated row and falls back to the canonical prose. This is
            // the asymmetry between "translated, and stopped early" and "not translated at all".
            val nameOnly = mapOf<Locale, LocalizedPair>(arabic to LocalizedPair("أذكار الصباح"))
            assertThat(nameOnly.displayDescription(english, "Remembrance for the start of the day"))
                .isEqualTo("Remembrance for the start of the day")
        }

        test("an untranslated row falls back to its own prose") {
            assertThat(canonicalOnly.displayDescription(arabic, "Remembrance for the start of the day"))
                .isEqualTo("Remembrance for the start of the day")
        }

        test("a whitespace-only description counts as no description, not as the canonical one") {
            val blank = mapOf<Locale, LocalizedPair>(arabic to LocalizedPair("أذكار الصباح", "   "))
            assertThat(blank.displayDescription(arabic, "canonical")).isNull()
        }

        test("a row with no prose anywhere resolves to nothing") {
            assertThat(emptyMap<Locale, LocalizedPair>().displayDescription(english, null)).isNull()
            assertThat(emptyMap<Locale, LocalizedPair>().displayDescription(english, "   ")).isNull()
        }
    }

    testSuite("reading a tag") {
        // Tags are the one case where the floor is not the raw column: `uq_tag__name` is a plain
        // unique index, so the stored name is folded and has to be un-folded on the way out.
        val folded = "food & drink"

        test("an exact match wins") {
            val tags = mapOf<Locale, String>(arabic to "الطعام والشراب")
            assertThat(tags.displayTagName(arabic, folded)).isEqualTo("الطعام والشراب")
        }

        test("the floor capitalises, because the stored name is folded") {
            assertThat(emptyMap<Locale, String>().displayTagName(english, folded)).isEqualTo("Food & drink")
        }

        test("the floor does not disturb a script without case") {
            assertThat(emptyMap<Locale, String>().displayTagName(arabic, "الطعام والشراب"))
                .isEqualTo("الطعام والشراب")
        }

        test("a neighbouring country is close enough") {
            val tags = mapOf<Locale, String>(arabic to "الطعام والشراب")
            assertThat(tags.displayTagName(Locale.forLanguageTag("ar-MA"), folded)).isEqualTo("الطعام والشراب")
        }

        test("a blank translation is no translation") {
            val tags = mapOf<Locale, String>(arabic to "  ")
            assertThat(tags.displayTagName(arabic, folded)).isEqualTo("Food & drink")
        }

        test("an untranslated tag reads its own name in every language") {
            assertThat(canonicalOnlyTags().displayTagName(arabic, folded)).isEqualTo("Food & drink")
        }
    }
}

/** An English-only tag table, the shape a user-created tag leaves behind. */
private fun canonicalOnlyTags(): Map<Locale, String> = mapOf(Locale.ENGLISH to "Food & Drink")
