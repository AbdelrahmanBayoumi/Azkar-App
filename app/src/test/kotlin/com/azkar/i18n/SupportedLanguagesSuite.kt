package com.azkar.i18n

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * The locale vocabulary the rest of the app is written against.
 *
 * Two things here are load-bearing enough to pin. [Locale.isSameLanguageAs] is what stops a user
 * re-picking the language they are already on from rebuilding the tree, so its "language only, not
 * the region" rule is a deliberate decision rather than a shortcut. The language profiles also
 * supply layout direction and typeface choices, with English as the fallback for unsupported languages.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val SupportedLanguagesSuite by testSuite("The supported languages") {
    test("Arabic runs right to left and English does not") {
        assertThat(ARABIC.isRightToLeft).isTrue()
        assertThat(ENGLISH.isRightToLeft).isFalse()
        assertThat(Locale.forLanguageTag("ar").isRightToLeft).isTrue()
    }

    test("a tag for a language the app does not ship is treated as English") {
        // Anything unrecognized lands in the English arm of every question asked of a locale, so the
        // layout is left-to-right and the face is Inter. A desktop OS set to a language the app has
        // no strings for gets a working window rather than a mirror it has nothing to mirror.
        assertThat(Locale.forLanguageTag("fr").usesArabicTypeface).isFalse()
        assertThat(Locale.forLanguageTag("he").usesArabicTypeface).isFalse()
        assertThat(Locale.forLanguageTag("ja").usesArabicTypeface).isFalse()
    }

    test("languages compare by language alone, ignoring case and region") {
        assertThat(Locale.forLanguageTag("ar").isSameLanguageAs(Locale.forLanguageTag("AR"))).isTrue()
        // Different region, same language: still the same language to a user choosing between them,
        // and still Arabic, so still right to left.
        assertThat(Locale.forLanguageTag("ar").isSameLanguageAs(Locale.forLanguageTag("ar-EG"))).isTrue()
        assertThat(Locale.forLanguageTag("ar-EG").isRightToLeft).isTrue()
        assertThat(Locale.forLanguageTag("en").isSameLanguageAs(Locale.forLanguageTag("en-GB"))).isTrue()
        assertThat(Locale.forLanguageTag("en").isSameLanguageAs(Locale.forLanguageTag("ar"))).isFalse()
    }

    test("each language is shown in its own script, not the reader's") {
        val arabicNameInArabic = ARABIC.displayNameIn(ARABIC)
        val arabicNameInEnglish = ARABIC.displayNameIn(ENGLISH)

        assertThat(arabicNameInArabic).isEqualTo("العربية")
        assertThat(arabicNameInEnglish).isEqualTo("Arabic")
    }

    test("the suggested locales are the two the language dialog offers") {
        assertThat(supportedLocales).containsExactly(Locale.ENGLISH, Locale.forLanguageTag("ar"))
    }

    test("an unsupported language does not select the Arabic typeface") {
        // Unsupported languages use the English profile until the app supplies their own profile.
        assertThat(Locale.forLanguageTag("he").usesArabicTypeface).isFalse()
    }
}
