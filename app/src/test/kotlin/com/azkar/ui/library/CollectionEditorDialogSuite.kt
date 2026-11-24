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

/**
 * The walk through the editor's pages, as composed — [CollectionEditorDraftSuite] covers the state.
 *
 * Every string here is pinned in English on purpose, exactly as [RemembranceEditorSuite] pins its own.
 * A test that reached for `stringResource` would be asserting that a resource exists; this one is
 * asserting that the second page says "Tags are how the shelf finds it again", which is a different
 * claim and the only one worth making from outside.
 *
 * The page subtitles are the load-bearing constants. They are the only thing on screen that says
 * which page you are on other than the bar, and asserting them is how "the second page shows the tag
 * picker" survives someone reordering the pages.
 */
private const val TITLE = "New Collection"
private const val STEP_DETAILS = "What to call it, and what it is for"
private const val STEP_TAGS = "Tags are how the shelf finds it again"
private const val STEP_COVER = "Give it a picture, or let the app draw one"
private const val NAME_LABEL = "Collection name"
private const val NAME_PLACEHOLDER = "e.g. Morning Azkar"
private const val DESCRIPTION_PLACEHOLDER = "What is this collection for? (optional)"
private const val TAG_SEARCH = "Search tags…"
private const val NEW_TAG_PLACEHOLDER = "New tag name"
private const val ADD_TAG = "Add tag"
private const val COVER_NONE = "No cover selected"
private const val CHOOSE_COVER = "Choose image…"
private const val BACK = "Back"
private const val NEXT = "Next"
private const val CANCEL = "Cancel"
private const val CREATE = "Create collection"
private const val MORNING = "morning"
private const val REQUIRED_NOTE = "Give the collection a name."

/** What one opening of the dialog reported back, so a test can assert on how it closed. */
private class EditorReported {
    val confirmations = mutableListOf<List<String>>()
    var dismissed = false
    var description: String? = null
    var tags: List<String> = emptyList()
}

/**
 * Opens the create dialog over a library that already knows two tags, runs [body], reports what happened.
 *
 * The window is taller than the default on purpose. The dialog's body scrolls — the header and the
 * footer stay put — so on a short window the create-a-tag row is legitimately below the fold, and
 * asserting it is *displayed* there would be testing the test window rather than the dialog.
 */
@OptIn(ExperimentalTestApi::class)
private fun converse(
    pageRecompositions: IntRange = 0..0,
    nameRecompositions: IntRange = 0..0,
    body: SkikoComposeUiTest.() -> Unit,
): EditorReported {
    val reported = EditorReported()
    withDesktopDejavu {
        runSkikoComposeUiTest(size = Size(1440f, 1200f), density = Density(1f)) {
            resetRecompositionCounts()
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = ENGLISH) {
                    CollectionEditorDialog(
                        draft = CollectionEditorDraft.create(),
                        environment =
                            CollectionEditorEnvironment(
                                covers = null,
                                availableTags = listOf(MORNING, "sleep").map(TagOption::of),
                                onPickCover = { _ -> null },
                            ),
                        onDismiss = { reported.dismissed = true },
                        onConfirm = { _, description, tagNames, _, _ ->
                            reported.description = description
                            reported.tags = tagNames
                            reported.confirmations += tagNames
                        },
                    )
                }
            }
            waitForIdle()
            resetRecompositionCounts()
            body()
            waitForIdle()
            onNodeWithTag(
                COLLECTION_EDITOR_DIALOG_TAG,
            ).assertTrackedRecompositions(atMost = 0, expectedFunction = "AzkarDialogSurface")
            assertComposableRecompositions("com.azkar.ui.library.CollectionEditorDialog", pageRecompositions)
            assertComposableRecompositions("com.azkar.ui.library.CollectionNameField", nameRecompositions)
        }
    }
    return reported
}

private const val COLLECTION_EDITOR_DIALOG_TAG = "collection-editor-dialog"

/** Names the collection, which every test that gets past the first page has to do first. */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.nameTheCollection(name: String = "Morning Azkar") {
    onNodeWithText(NAME_PLACEHOLDER).performTextReplacement(name)
}

/** Walks the pointer to the page after the one it is on. */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.advance() {
    onNodeWithText(NEXT).performClick()
}

@OptIn(ExperimentalTestApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val CollectionEditorDialogSuite by testSuite("The create-collection dialog") {

    test("the first page is the name and nothing else") {
        converse {
            onNodeWithText(TITLE).assertIsDisplayed()
            onNodeWithText(STEP_DETAILS).assertIsDisplayed()
            onNodeWithText(NAME_LABEL).assertIsDisplayed()
            onNodeWithText(DESCRIPTION_PLACEHOLDER).assertIsDisplayed()
            onNodeWithText(TAG_SEARCH).assertDoesNotExist()
            onNodeWithText(COVER_NONE).assertDoesNotExist()
        }
    }

    test("the second page is the tag picker, reached by Next") {
        converse(pageRecompositions = 1..1, nameRecompositions = 1..2) {
            nameTheCollection()
            advance()

            onNodeWithText(STEP_TAGS).assertIsDisplayed()
            onNodeWithText(TAG_SEARCH).assertIsDisplayed()
            onNodeWithText(MORNING).assertIsDisplayed()
            onNodeWithText(COVER_NONE).assertDoesNotExist()
        }
    }

    test("the last page is the cover, and the primary button finishes rather than advances") {
        converse(pageRecompositions = 2..2, nameRecompositions = 1..2) {
            nameTheCollection()
            advance()
            advance()

            onNodeWithText(STEP_COVER).assertIsDisplayed()
            onNodeWithText(COVER_NONE).assertIsDisplayed()
            onNodeWithText(CHOOSE_COVER).assertIsDisplayed()
            onNodeWithText(NEXT).assertDoesNotExist()
            onNodeWithText(CREATE).assertIsDisplayed()
        }
    }

    test("Back walks backwards through the pages it walked") {
        converse(pageRecompositions = 4..4, nameRecompositions = 1..4) {
            nameTheCollection()
            advance()
            advance()
            onNodeWithText(BACK).performClick()

            onNodeWithText(STEP_TAGS).assertIsDisplayed()
            onNodeWithText(BACK).performClick()
            onNodeWithText(STEP_DETAILS).assertIsDisplayed()
            onNodeWithText(NAME_LABEL).assertIsDisplayed()
            onNodeWithText(BACK).assertDoesNotExist()
        }
    }

    test("what was typed on an earlier page is still there on a later one") {
        val reported =
            converse(pageRecompositions = 2..2, nameRecompositions = 1..2) {
                nameTheCollection()
                onNodeWithText(DESCRIPTION_PLACEHOLDER).performTextReplacement("Said before sunrise.")
                advance()
                onNodeWithText(MORNING).performClick()
                advance()
                onNodeWithText(CREATE).performClick()
            }
        // Neither the description nor the tag is on screen on the last page, so the only place their
        // survival can be seen is the value the dialog finally hands over.
        assertThat(reported.description).isEqualTo("Said before sunrise.")
        assertThat(reported.tags).containsExactly(MORNING)
    }

    test("a nameless collection cannot leave the first page, and is told why") {
        converse(nameRecompositions = 1..2) {
            advance()

            onNodeWithText(REQUIRED_NOTE).assertExists()
            onNodeWithText(STEP_DETAILS).assertIsDisplayed()
            onNodeWithText(TAG_SEARCH).assertDoesNotExist()
        }
    }

    test("cancel is offered on every page, and leaves without creating anything") {
        val reported =
            converse(pageRecompositions = 1..1, nameRecompositions = 1..2) {
                nameTheCollection()
                advance()
                onNodeWithText(CANCEL).performClick()
            }

        assertThat(reported.confirmations).isEmpty()
        assertThat(reported.dismissed).isTrue()
    }

    test("a tag typed into the create row reaches the collection too") {
        val reported =
            converse(pageRecompositions = 2..2, nameRecompositions = 1..2) {
                nameTheCollection()
                advance()
                onNodeWithText(NEW_TAG_PLACEHOLDER).performTextReplacement("commute")
                onNodeWithText(ADD_TAG).performClick()
                advance()
                onNodeWithText(CREATE).performClick()
            }

        assertThat(reported.confirmations).containsExactly(listOf("commute"))
    }
}
