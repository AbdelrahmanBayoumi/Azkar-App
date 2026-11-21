package com.azkar.updates

import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val UpdateCheckScheduleSuite by testSuite("Automatic update checks") {
    test("checks immediately and then at the recurring interval") {
        runTest {
            var checks = 0
            val job =
                launch {
                    runUpdateCheckSchedule(
                        check = { checks++ },
                        intervalMillis = 60_000,
                    )
                }

            runCurrent()
            assertThat(checks).isEqualTo(1)
            advanceTimeBy(59_999.milliseconds)
            runCurrent()
            assertThat(checks).isEqualTo(1)
            advanceTimeBy(1.milliseconds)
            runCurrent()
            assertThat(checks).isEqualTo(2)
            job.cancel()
        }
    }

    test("each update stage is toasted once per version and session") {
        val gate = UpdateToastGate()

        assertThat(gate.shouldToast("available:1.2.0")).isTrue
        assertThat(gate.shouldToast("available:1.2.0")).isFalse
        assertThat(gate.shouldToast("downloading:1.2.0")).isTrue
        assertThat(gate.shouldToast("downloading:1.2.0")).isFalse
        assertThat(gate.shouldToast("ready:1.2.0")).isTrue
        assertThat(gate.shouldToast("available:1.3.0")).isTrue
    }
}
