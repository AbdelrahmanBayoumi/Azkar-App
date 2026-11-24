package com.azkar.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.azkar.i18n.ENGLISH
import com.azkar.testing.assertTrackedRecompositions
import com.azkar.testing.withDesktopDejavu
import com.azkar.ui.theme.AzkarTheme
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThat

/**
 * Where the header's controls land, which is a question about layout rather than about colour.
 *
 * The header is a `Box` with `contentAlignment = Center` holding three things at three alignments:
 * the brand at `CenterStart`, the tabs dead centre, and the settings gear at `CenterEnd`. Anything
 * that loses its `align` therefore does not vanish or fail a test — it slides to the middle and
 * sits on top of the tabs, which is exactly how the gear ended up printed across "Azkar Library"
 * once it picked up a tooltip.
 *
 * That is the reason for a geometry assertion in a suite whose other tests are about state. Every
 * earlier test in this repo passed while the header was visibly broken, because the gear was still
 * present, still enabled, still clickable and still named "Settings" — it was only in the wrong
 * place, and no amount of asserting on *what* it is can notice that.
 */
private val HEADER_SIZE = Size(1440f, 120f)

private const val SETTINGS = "Settings"
private const val LIBRARY_TAB = "Azkar Library"

@OptIn(ExperimentalTestApi::class)
private fun trackedHeaderTest(block: SkikoComposeUiTest.() -> Unit) {
    withDesktopDejavu {
        runSkikoComposeUiTest(size = HEADER_SIZE, density = Density(1f)) {
            resetRecompositionCounts()
            block()
        }
    }
}

@OptIn(ExperimentalTestApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val HeaderLayoutSuite by testSuite("The header's controls land where it says they do") {
    test("the settings gear sits at the far end, clear of the centred tabs") {
        trackedHeaderTest {
            val activeTab = mutableStateOf<AzkarDestination>(AzkarDestination.Home)
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = ENGLISH) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White),
                    ) {
                        MainScreenHeader(
                            activeTab = activeTab.value,
                            modifier = Modifier.testTag("header"),
                            callbacks =
                                HeaderCallbacks(
                                    onHome = {},
                                    onLibrary = {},
                                    onSettings = {},
                                ),
                        )
                    }
                }
            }
            waitForIdle()
            resetRecompositionCounts()

            val gear = onNodeWithContentDescription(SETTINGS).fetchSemanticsNode().boundsInRoot
            val tabs = onNodeWithText(LIBRARY_TAB).fetchSemanticsNode().boundsInRoot

            // The end of the header, in the only sense a test can measure: past the tabs, with the
            // header's own padding to spare. Asserting the gear is merely "not overlapping" would
            // pass while it sat one pixel clear of the tabs in the middle of the bar.
            assertThat(gear.left).isGreaterThan(tabs.right)
            runOnIdle { activeTab.value = AzkarDestination.Library }
            waitForIdle()
            onNodeWithTag("header").assertTrackedRecompositions(exactly = 1, expectedFunction = "MainScreenHeader")
        }
    }

    test("the gear is still to the end of the bar, not merely to the right of the tabs") {
        trackedHeaderTest {
            val activeTab = mutableStateOf<AzkarDestination>(AzkarDestination.Home)
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = ENGLISH) {
                    Box(
                        Modifier
                            .size(HEADER_SIZE.width.dp, HEADER_SIZE.height.dp)
                            .background(Color.White),
                    ) {
                        MainScreenHeader(
                            activeTab = activeTab.value,
                            modifier = Modifier.testTag("header"),
                            callbacks =
                                HeaderCallbacks(
                                    onHome = {},
                                    onLibrary = {},
                                    onSettings = {},
                                ),
                        )
                    }
                }
            }
            waitForIdle()
            resetRecompositionCounts()

            val gear = onNodeWithContentDescription(SETTINGS).fetchSemanticsNode().boundsInRoot

            // Centred content on a 1440px bar lands around 720; the gear belongs at the far end. The
            // margin is loose on purpose — this is a "which third of the bar is it in" assertion, not
            // a pixel test, and it is here to catch the specific regression of an `align` being read
            // by the wrong box and silently falling back to `contentAlignment`.
            assertThat(gear.left).isGreaterThan(HEADER_SIZE.width * 0.75f)
            runOnIdle { activeTab.value = AzkarDestination.Library }
            waitForIdle()
            onNodeWithTag("header").assertTrackedRecompositions(exactly = 1, expectedFunction = "MainScreenHeader")
        }
    }
}
