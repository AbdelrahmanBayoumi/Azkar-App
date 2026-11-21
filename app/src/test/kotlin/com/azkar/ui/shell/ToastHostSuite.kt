package com.azkar.ui.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.azkar.ui.preview.previewAzkarAppState
import com.azkar.ui.theme.AzkarTheme
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

@OptIn(ExperimentalTestApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val ToastHostSuite by testSuite("The app toast") {
    test("clicking an actionable toast invokes its action") {
        var clicks = 0
        val state = previewAzkarAppState()
        state.toasts +=
            ToastMessage(
                id = 1L,
                title = "Update available",
                message = "Open Settings to review the update.",
                priority = NotificationPriority.HIGH,
                onClick = { clicks++ },
            )

        runSkikoComposeUiTest {
            setContent {
                AzkarTheme {
                    ToastHost(state)
                }
            }
            waitForIdle()
            onNodeWithText("Update available").assertIsDisplayed().performClick()
        }

        assertThat(clicks).isEqualTo(1)
    }
}
