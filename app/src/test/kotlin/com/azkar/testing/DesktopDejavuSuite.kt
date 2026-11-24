package com.azkar.testing

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThatThrownBy

@Composable
private fun TrackingProbe(
    value: Int,
    modifier: Modifier = Modifier,
) {
    Text(value.toString(), modifier)
}

@OptIn(ExperimentalTestApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val DesktopDejavuSuite by testSuite("Desktop recomposition tracker") {
    test("custom-size tests reset the tracker and detect a real recomposition twice") {
        repeat(2) {
            withDesktopDejavu {
                runSkikoComposeUiTest(size = Size(400f, 300f), density = Density(1f)) {
                    var value by mutableIntStateOf(0)
                    setTrackedContent { TrackingProbe(value, Modifier.testTag("tracking-probe")) }
                    waitForIdle()
                    resetRecompositionCounts()
                    runOnIdle { value = 1 }
                    waitForIdle()
                    onNodeWithTag(
                        "tracking-probe",
                    ).assertTrackedRecompositions(exactly = 1, expectedFunction = "TrackingProbe")
                    assertComposableRecompositions("com.azkar.testing.TrackingProbe", 1..1)
                    assertThatThrownBy { assertNoAzkarRecompositions() }.hasMessageContaining("TrackingProbe")
                    resetRecompositionCounts()
                    mainClock.advanceTimeByFrame()
                    onNodeWithTag("tracking-probe").assertTrackedStable(expectedFunction = "TrackingProbe")
                    assertComposableRecompositions("com.azkar.testing.TrackingProbe", 0..0)
                    assertNoAzkarRecompositions()
                }
            }
        }
    }
}
