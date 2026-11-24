package com.azkar.ui.library

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.azkar.testing.assertTrackedStable
import com.azkar.testing.withDesktopDejavu
import com.azkar.ui.library.data.CollectionStats
import com.azkar.ui.library.model.CollectionMemberSort
import com.azkar.ui.library.model.CollectionMemberView
import com.azkar.ui.preview.previewCollectionStats
import com.azkar.ui.theme.AzkarThemePreview
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThat

private const val TOOLBAR_TAG = "members-toolbar"

@OptIn(ExperimentalTestApi::class)
private fun trackedToolbarTest(block: SkikoComposeUiTest.() -> Unit) {
    withDesktopDejavu {
        runSkikoComposeUiTest(size = Size(1584f, 900f), density = Density(1f)) {
            resetRecompositionCounts()
            block()
            mainClock.advanceTimeBy(100)
            waitForIdle()
            onNodeWithTag(TOOLBAR_TAG).assertTrackedStable(expectedFunction = "CollectionMemberToolbar")
        }
    }
}

/**
 * That the row above the members still says everything it is responsible for saying, and still offers
 * everything it is responsible for offering.
 *
 * This row has been rewritten three times in one sitting and every rewrite moved something between the
 * band, this row, and a card of their own. It carried the category chips; when those came out their `⋯`
 * menu's two actions became icon buttons here. Then progress, which used to be a bordered card between
 * this row and the members, moved onto this row, and the two view toggles were joined into one control.
 * Then the sort picker, which was an `OutlinedTextField` with a floor on its height and so stood at
 * nearly twice the height of everything beside it, was redrawn at the row's own size. The questions
 * below are the ones that move with those changes rather than the ones that were always true: that
 * progress is on the row, that the two view options are one control rather than two buttons, that the
 * sort is the same height as its neighbours, and that the reset is *absent* rather than inert when
 * there is nothing to undo.
 *
 * The joined toggle is the interesting one. "Are these two buttons?" is not a question any assertion
 * about presence can answer — both options were present before and both are present now, and the
 * difference is the four pixels of air between them. So it is asked as geometry: the two labels must
 * be separated by the seam and nothing else. A future change that takes the seam out and puts the
 * ordinary gap back fails here even though every node is still on the row. The sort height is the same
 * kind of question and is answered the same way, against the control beside it rather than against a
 * number copied out of the component.
 *
 * The absence assertions matter for the same reason [HeroLayoutSuite] exists for the band: a control
 * that is present but wrong is invisible to every other test in the app.
 */
@OptIn(ExperimentalTestApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val MemberToolbarSuite by testSuite("the members toolbar") {
    /**
     * The row's labels, as they are written in `values/strings.xml` rather than as they sound.
     *
     * Taken from the resource file rather than from the composable's arguments on purpose: a test that
     * names the string the code just passed in asserts that the argument arrived, and not that the
     * control says anything. The view toggles and the sort are looked up as text because that is how
     * they reach the semantics tree; only the reset carries a content description, because the
     * tooltip doubles as the label there.
     */
    val resetProgressLabel = "Reset progress"
    val startLabel = "Start"
    val gridLabel = "Grid"
    val listLabel = "List"
    val sortLabel = "Sort by"

    /**
     * What each click on the row reached.
     *
     * Recorded rather than asserted inside the click so that the assertion can name which control was
     * pressed, rather than a single "something happened" that any of the row's controls would satisfy.
     */
    var pressed: String? = null

    /**
     * The figures the row prints.
     *
     * Written out rather than taken from the fixture's defaults, because the count is what the progress
     * line says and a default that changed underneath this suite would silently change the assertion
     * along with it.
     */
    val underWay = previewCollectionStats()

    /** Lays the toolbar out on its own, at the width a real window gives it. */
    fun SkikoComposeUiTest.showToolbar(
        stats: CollectionStats = underWay,
        view: CollectionMemberView = CollectionMemberView.GRID,
        canResetProgress: Boolean,
        canStart: Boolean = true,
    ) {
        setTrackedContent {
            AzkarThemePreview {
                CollectionMemberToolbar(
                    stats = stats,
                    sort = CollectionMemberSort.SEQUENCE,
                    view = view,
                    gates = ToolbarGates(canStart = canStart, canResetProgress = canResetProgress),
                    actions =
                        MemberToolbarActions(
                            onStart = { pressed = startLabel },
                            onRequestReset = { pressed = resetProgressLabel },
                            onSortSelected = { pressed = it.name },
                            onViewSelected = { pressed = it.name },
                        ),
                    modifier = Modifier.width(1200.dp).testTag(TOOLBAR_TAG),
                )
            }
        }
        waitForIdle()
        resetRecompositionCounts()
    }

    test("how far through the collection you are is on the row") {
        trackedToolbarTest {
            showToolbar(canResetProgress = false)
            onNodeWithText("31 of 54 recited", substring = true).assertIsDisplayed()
        }
    }

    test("a collection with no members prints no progress at all") {
        trackedToolbarTest {
            showToolbar(
                stats = previewCollectionStats(totalMembers = 0, completedMembers = 0, remainingRepeats = 0),
                canResetProgress = false,
            )
            onNodeWithText("recited", substring = true).assertDoesNotExist()
        }
    }

    test("clearing progress is not offered before anything has been recited") {
        trackedToolbarTest {
            showToolbar(canResetProgress = false)
            onNodeWithContentDescription(resetProgressLabel).assertDoesNotExist()
        }
    }

    test("clearing progress reaches its own callback") {
        trackedToolbarTest {
            showToolbar(canResetProgress = true)
            resetRecompositionCounts()
            onNodeWithContentDescription(resetProgressLabel).performClick()
            assertThat(pressed).describedAs("what the reset button reached").isEqualTo(resetProgressLabel)
            onNodeWithTag(TOOLBAR_TAG).assertTrackedStable(expectedFunction = "CollectionMemberToolbar")
        }
    }

    test("the view toggle and the sort are still on the row") {
        trackedToolbarTest {
            showToolbar(canResetProgress = true)

            onNodeWithText(gridLabel).assertIsDisplayed()
            onNodeWithText(listLabel).assertIsDisplayed()
            onNodeWithText(sortLabel).assertIsDisplayed()
        }
    }

    test("the two view options are halves of one control, not two buttons") {
        trackedToolbarTest {
            showToolbar(canResetProgress = true)

            val grid = onNodeWithText(gridLabel).fetchSemanticsNode().boundsInRoot
            val list = onNodeWithText(listLabel).fetchSemanticsNode().boundsInRoot

            // The seam, and nothing else. This is the whole assertion: a pair of chips had the
            // ordinary inter-control gap here, and a pair that does is exactly what the reader was
            // being asked to guess at.
            assertThat(list.left - grid.right)
                .describedAs("the air between the two halves of the view control")
                .isEqualTo(TWO_HALF_SEAM_PX)
        }
    }

    test("choosing the list reaches the row's own callback") {
        trackedToolbarTest {
            showToolbar(canResetProgress = true)
            resetRecompositionCounts()
            onNodeWithText(listLabel).performClick()
            assertThat(pressed).describedAs("what the list half reached").isEqualTo(CollectionMemberView.LIST.name)
            onNodeWithTag(TOOLBAR_TAG).assertTrackedStable(expectedFunction = "CollectionMemberToolbar")
        }
    }

    test("the sort is the height of the rest of the row, not a field") {
        trackedToolbarTest {
            showToolbar(canResetProgress = true)

            val sort = onNodeWithText("In order").fetchSemanticsNode().boundsInRoot
            val view = onNodeWithText(gridLabel).fetchSemanticsNode().boundsInRoot

            // The symptom, and the reason it is asked as an equality between the two rather than as a
            // number: this was an `OutlinedTextField`, which has a minimum height it will not go below,
            // so the picker stood nearly twice the height of everything beside it with its label lost
            // in the middle. Any control that keeps that floor fails here, whatever else it looks like.
            assertThat(sort.height)
                .describedAs("the height of the sort beside a ${view.height}dp view toggle")
                .isEqualTo(view.height)
        }
    }

    test("choosing an order from the menu reaches the row's own callback") {
        trackedToolbarTest {
            showToolbar(canResetProgress = true)
            onNodeWithText("In order").performClick()
            onNodeWithText("By title").performClick()
            assertThat(pressed)
                .describedAs("what the sort menu reached")
                .isEqualTo(CollectionMemberSort.TITLE.name)
        }
    }
}

/**
 * The width of the rule drawn between the two halves of the view control, in pixels at the 1x density
 * every suite in this file lays itself out at.
 *
 * Written down here rather than imported so that the test states the rule it is checking instead of
 * agreeing with whatever the component happens to use today — the day the seam is drawn 2dp wide for
 * legibility, this is the line somebody has to come here and change.
 */
private const val TWO_HALF_SEAM_PX = 1f
