package com.azkar.ui.notifications.presentation

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.azkar.i18n.ARABIC
import com.azkar.i18n.ENGLISH
import com.azkar.testing.assertTrackedRecompositions
import com.azkar.testing.assertTrackedStable
import com.azkar.ui.theme.AzkarTheme
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.runRecompositionTrackingUiTest
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThat
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val CountdownRecompositionSuite by testSuite("Notification countdown recomposition") {
    test("sampling a draining countdown does not recompose the rule") {
        runRecompositionTrackingUiTest {
            mainClock.autoAdvance = false
            val samples = AtomicInteger()
            var revision by mutableIntStateOf(0)
            setTrackedContent {
                AzkarTheme(darkTheme = false, uiLocale = ENGLISH) {
                    val remaining: () -> Float =
                        remember(revision) {
                            val currentRevision = revision
                            val sample: () -> Float = { 1f / (samples.incrementAndGet() + currentRevision + 1) }
                            sample
                        }
                    CountdownRule(
                        remaining = remaining,
                        modifier = Modifier.testTag("countdown-rule"),
                    )
                }
            }
            mainClock.advanceTimeBy(100)
            waitForIdle()
            assertThat(samples.get()).isGreaterThanOrEqualTo(2)
            resetRecompositionCounts()
            runOnIdle { revision++ }
            mainClock.advanceTimeByFrame()
            waitForIdle()
            onNodeWithTag("countdown-rule")
                .assertTrackedRecompositions(atLeast = 1, expectedFunction = "CountdownRule")
            resetRecompositionCounts()
            val baselineSamples = samples.get()
            mainClock.advanceTimeBy(165)
            waitForIdle()
            assertThat(samples.get()).isGreaterThanOrEqualTo(baselineSamples + 4)
            onNodeWithTag("countdown-rule").assertTrackedStable(expectedFunction = "CountdownRule")
        }
    }

    listOf(ENGLISH, ARABIC).forEach { locale ->
        test("the countdown drains from the physical right in ${locale.language}") {
            runRecompositionTrackingUiTest {
                mainClock.autoAdvance = false
                val remaining = AtomicReference(1f)
                setTrackedContent {
                    AzkarTheme(darkTheme = false, uiLocale = locale) {
                        CountdownRule(
                            remaining = { remaining.get() },
                            modifier = Modifier.width(120.dp).testTag("countdown-rule"),
                        )
                    }
                }
                mainClock.advanceTimeBy(100)
                waitForIdle()
                val full = onNodeWithTag("countdown-rule").captureToImage().toPixelMap()
                val left = full.width / 10
                val right = full.width * 9 / 10
                val middle = full.height / 2
                val fullLeft = full[left, middle]
                val fullRight = full[right, middle]

                resetRecompositionCounts()
                remaining.set(0.25f)
                mainClock.advanceTimeBy(330)
                waitForIdle()
                val drained = onNodeWithTag("countdown-rule").captureToImage().toPixelMap()
                assertThat(drained[left, middle]).isEqualTo(fullLeft)
                assertThat(drained[right, middle]).isNotEqualTo(fullRight)
                onNodeWithTag("countdown-rule").assertTrackedStable(expectedFunction = "CountdownRule")

                remaining.set(1f)
                mainClock.advanceTimeBy(330)
                waitForIdle()
                val afterLargerSample = onNodeWithTag("countdown-rule").captureToImage().toPixelMap()
                assertThat(afterLargerSample[right, middle]).isEqualTo(drained[right, middle])
                onNodeWithTag("countdown-rule").assertTrackedStable(expectedFunction = "CountdownRule")
            }
        }
    }
}
