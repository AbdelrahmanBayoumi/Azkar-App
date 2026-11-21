package com.azkar.ui.library

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import com.azkar.i18n.ENGLISH
import com.azkar.testing.assertComposableRecompositions
import com.azkar.testing.assertTrackedRecompositions
import com.azkar.testing.withDesktopDejavu
import com.azkar.ui.library.model.TagOption
import com.azkar.ui.theme.AzkarTheme
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

private const val TITLE = "Create New Remembrance"
private const val STEP_TEXT = "Write the remembrance"
private const val STEP_TRANSLATIONS = "Add translations and explanations"
private const val STEP_TAGS = "Choose tags to find it later"
private const val TEXT_LABEL = "Remembrance text"
private const val TEXT_PLACEHOLDER = "Write the remembrance in Arabic…"
private const val TEXT_REQUIRED = "Enter the remembrance text to continue."
private const val ADDITIONAL_INFO = "Additional information"
private const val TRANSLATION_HINT = "Translations and explanations are optional."
private const val ADD_TRANSLATION = "Add Translation"
private const val TRANSLATION_PLACEHOLDER = "Write the translated remembrance…"
private const val EXPLANATION_PLACEHOLDER = "Explain the meaning (optional)…"
private const val NEW_TAG_PLACEHOLDER = "New tag name"
private const val ADD_TAG = "Add tag"
private const val NEXT = "Next"
private const val BACK = "Back"
private const val CANCEL = "Cancel"
private const val CREATE = "Create Remembrance"
private const val ENGLISH_LABEL = "English"

private class RemembranceCreateReport {
    var dismissed = false
    var confirmedText: String? = null
    var confirmedSource: String? = null
    var confirmedTags: List<String> = emptyList()
    var confirmedTranslations: Map<Locale, com.azkar.domain.model.Remembrance.Translations> = emptyMap()
}

@OptIn(ExperimentalTestApi::class)
private fun converse(
    pageRecompositions: IntRange = 0..0,
    textRecompositions: IntRange = 0..0,
    body: SkikoComposeUiTest.() -> Unit,
): RemembranceCreateReport {
    val report = RemembranceCreateReport()
    withDesktopDejavu {
        runSkikoComposeUiTest(size = Size(1440f, 1200f), density = Density(1f)) {
            resetRecompositionCounts()
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = ENGLISH) {
                    val draft = RemembranceCreateDraft(textLocale = Locale.forLanguageTag("ar"))
                    RemembranceCreateDialog(
                        draft = draft,
                        availableTags = listOf("morning", "sleep").map(TagOption::of),
                        onDismiss = { report.dismissed = true },
                        onConfirm = { submitted ->
                            report.confirmedText = submitted.submittedText
                            report.confirmedSource = submitted.submittedSource
                            report.confirmedTags = submitted.tags
                            report.confirmedTranslations = submitted.submittedTranslations
                        },
                    )
                }
            }
            waitForIdle()
            resetRecompositionCounts()
            body()
            waitForIdle()
            onNodeWithTag(
                REMEMBRANCE_CREATE_DIALOG_TAG,
            ).assertTrackedRecompositions(atMost = 0, expectedFunction = "AzkarDialogSurface")
            assertComposableRecompositions("com.azkar.ui.library.RemembranceCreateDialog", pageRecompositions)
            assertComposableRecompositions("com.azkar.ui.library.RemembranceTextStep", textRecompositions)
        }
    }
    return report
}

private const val REMEMBRANCE_CREATE_DIALOG_TAG = "remembrance-create-dialog"

@OptIn(ExperimentalTestApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val RemembranceCreateDialogSuite by testSuite("The create-remembrance dialog") {
    test("the first page asks for the passage and keeps additional information collapsed") {
        converse {
            onNodeWithText(TITLE).assertIsDisplayed()
            onNodeWithText(STEP_TEXT).assertIsDisplayed()
            onNodeWithText(TEXT_LABEL).assertIsDisplayed()
            onNodeWithText(ADDITIONAL_INFO).assertIsDisplayed()
            onNodeWithText("Grade").assertDoesNotExist()
            onNodeWithText(TRANSLATION_HINT).assertDoesNotExist()
        }
    }

    test("a blank passage cannot advance and the form explains what is missing") {
        converse(textRecompositions = 1..2) {
            onNodeWithText(NEXT).performClick()

            onNodeWithText(TEXT_REQUIRED).assertIsDisplayed()
            onNodeWithText(STEP_TEXT).assertIsDisplayed()
            onNodeWithText(TRANSLATION_HINT).assertDoesNotExist()
        }
    }

    test("Back and Cancel remain available as the user moves through the wizard") {
        converse(pageRecompositions = 2..2, textRecompositions = 1..3) {
            onNodeWithText(TEXT_PLACEHOLDER).performTextReplacement("الحمد لله")
            onNodeWithText(NEXT).performClick()
            onNodeWithText(STEP_TRANSLATIONS).assertIsDisplayed()
            onNodeWithText(BACK).performClick()
            onNodeWithText(STEP_TEXT).assertIsDisplayed()
            onNodeWithText(CANCEL).performClick()
        }.also { report -> assertThat(report.dismissed).isTrue() }
    }

    test("text, translation, explanation, and a new tag reach confirmation") {
        val report =
            converse(pageRecompositions = 2..2, textRecompositions = 1..2) {
                onNodeWithText(TEXT_PLACEHOLDER).performTextReplacement("الحمد لله")
                onNodeWithText(ADDITIONAL_INFO).performClick()
                onNodeWithText("Book, reference, or source…").performTextReplacement("  Book 1  ")
                onNodeWithText(NEXT).performClick()

                onNodeWithText(ENGLISH_LABEL).assertIsDisplayed()
                onNodeWithText(ADD_TRANSLATION).performClick()
                onNodeWithText(TRANSLATION_PLACEHOLDER).performTextReplacement("All praise is for God")
                onNodeWithText(EXPLANATION_PLACEHOLDER).performTextReplacement("A short explanation")
                onNodeWithText(NEXT).performClick()

                onNodeWithText(NEW_TAG_PLACEHOLDER).performTextReplacement("commute")
                onNodeWithText(ADD_TAG).performClick()
                onNodeWithText(STEP_TAGS).assertIsDisplayed()
                onNodeWithText(CREATE).performClick()
            }

        assertThat(report.confirmedText).isEqualTo("الحمد لله")
        assertThat(report.confirmedSource).isEqualTo("Book 1")
        assertThat(report.confirmedTags).containsExactly("commute")
        assertThat(
            report.confirmedTranslations
                .getValue(Locale.forLanguageTag("ar"))
                .translationPair.text,
        ).isEqualTo("الحمد لله")
        assertThat(
            report.confirmedTranslations
                .getValue(ENGLISH)
                .translationPair.text,
        ).isEqualTo("All praise is for God")
        assertThat(
            report.confirmedTranslations
                .getValue(ENGLISH)
                .explanationPair.text,
        ).isEqualTo("A short explanation")
    }
}
