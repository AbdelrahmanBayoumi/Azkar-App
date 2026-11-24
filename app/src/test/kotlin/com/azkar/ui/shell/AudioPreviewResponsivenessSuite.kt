package com.azkar.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import com.azkar.testing.assertTrackedStable
import com.azkar.testing.withDesktopDejavu
import com.azkar.ui.theme.AzkarTheme
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.setTrackedContent
import kotlinx.coroutines.CompletableDeferred
import org.assertj.core.api.Assertions.assertThat
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalTestApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val AudioPreviewResponsivenessSuite by testSuite("Audio preview UI responsiveness") {
    test("an asynchronous playback error is shown in the settings card") {
        var reportError: ((Throwable) -> Unit)? = null
        runSkikoComposeUiTest {
            setContent {
                AzkarTheme {
                    AudioChoice(
                        choice =
                            AudioChoiceModel(
                                title = "Prayer sounds",
                                pathLabel = "Audio file",
                                enabled = true,
                                path = "/audio/test.mp3",
                                volume = 0.7f,
                                onEnabled = {},
                                onPath = {},
                                onVolume = {},
                                onPreview = { reportError = it },
                                onChoose = {},
                                onStop = null,
                                stopLabel = null,
                            ),
                    )
                }
            }
            onNodeWithContentDescription("Preview sound").performClick()
            waitForIdle()
            runOnIdle { checkNotNull(reportError)(IllegalStateException("decode failed")) }
            onNodeWithText("Unable to play this audio file. Check that it exists and uses a supported audio format.")
                .assertIsDisplayed()
        }
    }

    test("a suspended audio preview does not block other UI interactions") {
        val previewStarted = CompletableDeferred<Unit>()
        val finishPreview = CompletableDeferred<Unit>()
        val otherActionCount = AtomicInteger()

        runSkikoComposeUiTest {
            setContent {
                AzkarTheme {
                    Column {
                        AudioChoice(
                            choice =
                                AudioChoiceModel(
                                    title = "Prayer sounds",
                                    pathLabel = "Audio file",
                                    enabled = true,
                                    path = "/audio/test.mp3",
                                    volume = 0.7f,
                                    onEnabled = {},
                                    onPath = {},
                                    onVolume = {},
                                    onPreview = {
                                        previewStarted.complete(Unit)
                                        finishPreview.await()
                                    },
                                    onChoose = {},
                                    onStop = null,
                                    stopLabel = null,
                                ),
                        )
                        Button(onClick = { otherActionCount.incrementAndGet() }) {
                            Text("Still responsive")
                        }
                    }
                }
            }

            onNodeWithContentDescription("Preview sound").performClick()
            previewStarted.await()

            onNodeWithText("Still responsive").assertIsDisplayed().performClick()
            assertThat(otherActionCount.get())
                .describedAs("a second UI action should run while audio playback is suspended")
                .isEqualTo(1)

            finishPreview.complete(Unit)
        }
    }

    test("starting preview does not recompose its audio settings card") {
        val previewStarted = CompletableDeferred<Unit>()
        val finishPreview = CompletableDeferred<Unit>()

        withDesktopDejavu {
            runSkikoComposeUiTest {
                setTrackedContent {
                    AzkarTheme {
                        AudioChoice(
                            modifier = Modifier.width(520.dp).then(Modifier.testTag("audio-choice-card")),
                            choice =
                                AudioChoiceModel(
                                    title = "Prayer sounds",
                                    pathLabel = "Audio file",
                                    enabled = true,
                                    path = "/audio/test.mp3",
                                    volume = 0.7f,
                                    onEnabled = {},
                                    onPath = {},
                                    onVolume = {},
                                    onPreview = {
                                        previewStarted.complete(Unit)
                                        finishPreview.await()
                                    },
                                    onChoose = {},
                                    onStop = null,
                                    stopLabel = null,
                                ),
                        )
                    }
                }

                waitForIdle()
                resetRecompositionCounts()
                onNodeWithContentDescription("Preview sound").performClick()
                previewStarted.await()
                waitForIdle()
                onNodeWithTag("audio-choice-card").assertTrackedStable(expectedFunction = "AzkarCard")
                finishPreview.complete(Unit)
            }
        }
    }
}
