package com.azkar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import com.azkar.testing.assertTrackedRecompositions
import com.azkar.testing.assertTrackedStable
import com.azkar.ui.theme.AzkarTheme
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.runRecompositionTrackingUiTest
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * Applies [pressFeedback] the way the app does, which is through [pressableClick] — it is the larger
 * of the two call-site populations (36 of roughly 51) and, more to the point, [pressFeedback] on its
 * own receives nothing: the interaction source that emits press events belongs to the `clickable`
 * that owns the press.
 *
 * [onClick] is there to prove the gesture landed. A callback either fires or it does not, which is
 * the one unambiguous signal available: without it, a "zero recompositions" assertion would pass just
 * as happily if the injected press had never reached the node at all.
 */
@Composable
private fun PressSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(120.dp)
            .background(Color.White)
            .pressableClick(onClick = onClick),
    )
}

/**
 * Every control here used to read its hover and press animation through a `by` delegate, which reads
 * at composition — so each animation frame recomposed the control, and after the move to Material 3
 * Expressive that meant recomposing a whole `Surface`/`IconButton` subtree per frame. They now drive
 * [androidx.compose.animation.core.Animatable] from the interaction stream and read it inside a
 * deferred draw-phase block.
 *
 * These tests are the regression net for that. The budgets are **zero by intent**, not zero by
 * measurement luck: a control whose feedback is expressed declaratively will recompose once per
 * animation frame, so any non-zero count here is the old defect returning rather than noise. The
 * grid case checks the aggregate count across multiple instances of the same control.
 *
 * The failure this guards against is not hypothetical. Screenshot-based checking cannot see it (the
 * hover fill is invisible in a still), and it was found by counting compositions, not by looking.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val RecompositionSuite by testSuite("Control recomposition budgets") {
    listOf(false, true).forEach { dark ->
        test("nav selection and press keep their visible feedback in ${if (dark) "dark" else "light"} theme") {
            runRecompositionTrackingUiTest {
                mainClock.autoAdvance = false
                val selected = mutableStateOf(false)
                var highlight = Color.Unspecified
                setTrackedContent {
                    AzkarTheme(darkTheme = dark, uiLocale = Locale.ENGLISH) {
                        highlight = AzkarTheme.colors.highlight
                        Box(Modifier.background(AzkarTheme.colors.background)) {
                            NavButton(
                                text = "Home",
                                selected = selected.value,
                                onClick = {},
                                modifier = Modifier.testTag("nav-visual"),
                            )
                        }
                    }
                }
                mainClock.advanceTimeBy(1_000)
                waitForIdle()
                runOnIdle { selected.value = true }
                mainClock.advanceTimeBy(1_000)
                waitForIdle()
                val node = onNodeWithTag("nav-visual")
                val selectedPixels = node.captureToImage().toPixelMap()
                val highlightPixels =
                    (0 until selectedPixels.height).sumOf { y ->
                        (0 until selectedPixels.width).count { x ->
                            selectedPixels[x, y].toArgb() == highlight.toArgb()
                        }
                    }
                assertThat(highlightPixels).describedAs("selected navigation fill").isGreaterThan(100)

                node.performMouseInput { moveTo(center) }
                mainClock.advanceTimeBy(500)
                val hovered = node.captureToImage().toPixelMap()
                resetRecompositionCounts()
                node.performMouseInput { press() }
                mainClock.advanceTimeBy(150)
                val pressed = node.captureToImage().toPixelMap()
                val changedPixels =
                    (0 until pressed.height).sumOf { y ->
                        (0 until pressed.width).count { x -> pressed[x, y].toArgb() != hovered[x, y].toArgb() }
                    }
                assertThat(changedPixels).describedAs("navigation press feedback").isGreaterThan(50)
                node.assertTrackedStable(expectedFunction = "NavButton")
                node.performMouseInput { release() }
            }
        }
    }

    test("changing the selected nav button does not recompose on every animation frame") {
        runRecompositionTrackingUiTest {
            mainClock.autoAdvance = false
            val selected = mutableStateOf(false)
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
                    NavButton(
                        text = "Home",
                        selected = selected.value,
                        onClick = {},
                        modifier = Modifier.testTag("nav-selection"),
                    )
                }
            }
            mainClock.advanceTimeBy(1_000)
            waitForIdle()
            resetRecompositionCounts()
            runOnIdle { selected.value = true }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("nav-selection").assertTrackedRecompositions(atLeast = 1, expectedFunction = "NavButton")
            resetRecompositionCounts()
            repeat(40) { mainClock.advanceTimeBy(16) }
            onNodeWithTag("nav-selection").assertTrackedStable(expectedFunction = "NavButton")
        }
    }
    test("hovering a circle icon button recomposes nothing") {
        runRecompositionTrackingUiTest {
            mainClock.autoAdvance = false
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
                    Box(Modifier.size(300.dp).background(Color.White)) {
                        CircleIconButton(
                            icon = AzkarIcons.Star,
                            contentDescription = "favourite",
                            onClick = {},
                            modifier = Modifier.testTag("control"),
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            waitForIdle()
            resetRecompositionCounts()
            onNodeWithContentDescription("favourite").performMouseInput { moveTo(center) }
            repeat(40) { mainClock.advanceTimeBy(16) }
            onNodeWithTag("control").assertTrackedStable(expectedFunction = "CircleIconButton")
        }
    }

    test("hovering an icon action button recomposes nothing") {
        runRecompositionTrackingUiTest {
            mainClock.autoAdvance = false
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
                    Box(Modifier.size(300.dp).background(Color.White)) {
                        IconActionButton(
                            icon = ActionIcons.favorite(on = true),
                            contentDescription = "favourite",
                            onClick = {},
                            modifier = Modifier.testTag("control"),
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            waitForIdle()
            resetRecompositionCounts()
            onNodeWithContentDescription("favourite").performMouseInput { moveTo(center) }
            repeat(40) { mainClock.advanceTimeBy(16) }
            onNodeWithTag("control").assertTrackedStable(expectedFunction = "IconActionButton")
        }
    }

    /**
     * [pressFeedback] is the highest-leverage of these: it is applied by roughly fifty call sites, so
     * a per-frame composition here was costing the whole app.
     *
     * The click count is the part that makes the zero meaningful. It is asserted first and on the
     * same node, so "zero recompositions" can only be read alongside "the press demonstrably landed
     * here and still cost nothing" — not as "the press never arrived".
     */
    test("pressing a press-feedback surface recomposes nothing") {
        runRecompositionTrackingUiTest {
            mainClock.autoAdvance = false
            var clicks = 0
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
                    PressSurface(
                        onClick = { clicks++ },
                        modifier = Modifier.testTag("press-surface"),
                    )
                }
            }
            mainClock.advanceTimeBy(1_000)
            waitForIdle()
            resetRecompositionCounts()
            onNodeWithTag("press-surface").performMouseInput {
                moveTo(center)
                press()
                repeat(40) { mainClock.advanceTimeBy(16) }
                release()
                repeat(40) { mainClock.advanceTimeBy(16) }
            }
            assertThat(clicks).describedAs("the press must actually land on this node").isEqualTo(1)
            onNodeWithTag("press-surface").assertTrackedStable(expectedFunction = "PressSurface")
        }
    }

    /**
     * Every icon-only control in the app now ships inside a tooltip, so the hover path the budgets
     * above guard is no longer the one that runs.
     *
     * Showing a tooltip is a genuine state change — Material reads `TooltipState.isVisible` at
     * composition — so the appearance itself is allowed to cost something. What must not happen is
     * the failure this suite exists for: a *per-frame* cost while the chip is simply sitting there.
     * So the count is taken once the chip has appeared and settled, and only the frames after that
     * are asserted. A budget of zero across the settle would be a stronger claim than is true and
     * would eventually fail for reasons that are not a defect.
     */
    test("a tooltip button does not recompose per frame while its tooltip is on screen") {
        runRecompositionTrackingUiTest {
            mainClock.autoAdvance = false
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
                    Box(Modifier.size(300.dp).background(Color.White)) {
                        TooltipIconButton(
                            icon = AzkarIcons.Star,
                            tooltip = "Add to favourites",
                            onClick = {},
                            modifier = Modifier.testTag("control"),
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            onNodeWithContentDescription("Add to favourites").performMouseInput { moveTo(center) }
            // Long enough for the popup to compose and its scale/fade transition to finish.
            repeat(60) { mainClock.advanceTimeBy(16) }
            waitForIdle()
            onNodeWithText("Add to favourites").assertIsDisplayed()
            resetRecompositionCounts()
            repeat(40) { mainClock.advanceTimeBy(16) }
            onNodeWithTag("control").assertTrackedStable()
        }
    }

    /** Hovering the unselected button must not create frame-by-frame compositions. */
    test("hovering a nav button does not recompose per frame") {
        runRecompositionTrackingUiTest {
            mainClock.autoAdvance = false
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
                    Box(Modifier.size(600.dp).background(Color.White)) {
                        NavButton(
                            text = "Home",
                            selected = false,
                            onClick = {},
                            leadingIcon = AzkarIcons.Home,
                            modifier = Modifier.testTag("control"),
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            waitForIdle()
            resetRecompositionCounts()
            onNodeWithText("Home").performMouseInput { moveTo(center) }
            repeat(40) { mainClock.advanceTimeBy(16) }
            onNodeWithTag("control").assertTrackedStable(expectedFunction = "NavButton")
        }
    }

    /** One tag makes Dejavu count the whole function across all grid instances. */
    test("sweeping 24 circle icon buttons does not recompose the buttons") {
        runRecompositionTrackingUiTest {
            mainClock.autoAdvance = false
            val favorite = mutableStateOf(false)
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
                    Box(Modifier.size(600.dp).background(Color.White)) {
                        Column {
                            repeat(6) { row ->
                                Row {
                                    repeat(4) { col ->
                                        CircleIconButton(
                                            icon = AzkarIcons.Star,
                                            contentDescription = "grid-$row-$col",
                                            onClick = {},
                                            buttonSize = 40.dp,
                                            favorite = row == 0 && col == 1 && favorite.value,
                                            modifier =
                                                if (row == 0 &&
                                                    col == 0
                                                ) {
                                                    Modifier.testTag("grid-probe")
                                                } else {
                                                    Modifier
                                                },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            waitForIdle()
            resetRecompositionCounts()
            // Change an untagged instance: the tagged function must report that composition too.
            runOnIdle { favorite.value = true }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("grid-probe").assertTrackedRecompositions(atLeast = 1, expectedFunction = "CircleIconButton")
            runOnIdle { favorite.value = false }
            mainClock.advanceTimeBy(1_000)
            waitForIdle()
            resetRecompositionCounts()
            for (row in 0 until 6) {
                for (col in 0 until 4) {
                    onNodeWithContentDescription("grid-$row-$col").performMouseInput { moveTo(center) }
                    repeat(8) { mainClock.advanceTimeBy(16) }
                }
            }
            onNodeWithTag("grid-probe").assertTrackedStable(expectedFunction = "CircleIconButton")
        }
    }
}
