package com.azkar.ui.library

import com.azkar.i18n.ENGLISH
import com.azkar.ui.library.model.TagOption
import com.azkar.ui.library.model.missingRemembranceLanguages
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

private val ARABIC: Locale = Locale.forLanguageTag("ar")

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val RemembranceCreateDraftSuite by testSuite("A new remembrance carries what the form collected") {
    test("the Arabic passage is required and the rejection clears when it is corrected") {
        val draft = RemembranceCreateDraft(textLocale = ARABIC)

        assertThat(draft.submit()).isFalse()
        assertThat(draft.textRejected).isTrue()

        draft.updateText("  الحمد لله  ")

        assertThat(draft.textRejected).isFalse()
        assertThat(draft.submit()).isTrue()
        assertThat(draft.submittedText).isEqualTo("الحمد لله")
    }

    test("the original language is included with an empty explanation") {
        val draft = RemembranceCreateDraft(textLocale = ARABIC).apply { updateText("الحمد لله") }

        val original = draft.submittedTranslations.getValue(ARABIC)

        assertThat(original.translationPair.text).isEqualTo("الحمد لله")
        assertThat(original.explanationPair.text).isEmpty()
    }

    test("completed translations are trimmed and abandoned ones are omitted") {
        val draft =
            RemembranceCreateDraft(textLocale = ARABIC).apply {
                updateText("الحمد لله")
                addTranslation(ENGLISH)
                updateTranslationText(ENGLISH, "  All praise is for God.  ")
                updateExplanation(ENGLISH, "  A concise explanation.  ")
                addTranslation(Locale.FRENCH)
            }

        val submitted = draft.submittedTranslations

        assertThat(submitted).containsKeys(ARABIC, ENGLISH).doesNotContainKey(Locale.FRENCH)
        assertThat(submitted.getValue(ENGLISH).translationPair.text).isEqualTo("All praise is for God.")
        assertThat(submitted.getValue(ENGLISH).explanationPair.text).isEqualTo("A concise explanation.")
    }

    test("one language cannot be added twice under another regional tag") {
        val draft = RemembranceCreateDraft(textLocale = ARABIC)

        assertThat(draft.addTranslation(ENGLISH)).isTrue()
        assertThat(draft.addTranslation(Locale.UK)).isFalse()
        assertThat(draft.translations).hasSize(1)
    }

    test("the language list excludes the passage language and versions already added") {
        val draft = RemembranceCreateDraft(textLocale = ARABIC).apply { addTranslation(ENGLISH) }

        assertThat(missingRemembranceLanguages(draft.translations, ARABIC, ENGLISH)).isEmpty()
    }

    test("tags are unique ignoring case, and Clear all resets the selection") {
        val draft =
            RemembranceCreateDraft(textLocale = ARABIC).apply {
                addTag("Morning")
                addTag("morning")
                toggleTag("sleep")
            }

        assertThat(draft.tags).containsExactly("Morning", "sleep")
        assertThat(draft.tagOptions(listOf("travel").map(TagOption::of)).map { it.value })
            .containsExactly("travel", "Morning", "sleep")

        draft.clearTags()

        assertThat(draft.tags).isEmpty()
    }
}
