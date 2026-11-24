package com.azkar

import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

/**
 * The window's database warm-up lifecycle: starts off the caller's thread, releases the pool when
 * it got far enough, and cancels itself when it did not.
 *
 * Everything runs on a `StandardTestDispatcher` over the virtual clock, so "the warm-up is still
 * in flight" is a state a test can put it in and hold, rather than something a real 180 ms wait
 * or a real H2 file would have to be raced against.
 */
@OptIn(ExperimentalCoroutinesApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val DatabaseWarmUpSuite by testSuite("The database warm-up") {
    test("the warm-up runs on the injected dispatcher, not on the caller") {
        runTest {
            var warm = false
            val warmUp =
                DatabaseWarmUp(
                    dispatcher = StandardTestDispatcher(testScheduler),
                    warmUp = { warm = true },
                    release = {},
                )

            warmUp.start()
            assertThat(warm).isFalse()

            runCurrent()
            assertThat(warm).isTrue()
        }
    }

    test("closing the window once the warm-up finished releases the pool") {
        runTest {
            var releases = 0
            val warmUp =
                DatabaseWarmUp(
                    dispatcher = StandardTestDispatcher(testScheduler),
                    warmUp = {},
                    release = { releases++ },
                )

            warmUp.start()
            runCurrent()
            warmUp.close()

            assertThat(releases).isOne()
        }
    }

    test("closing the window mid-warm-up cancels the coroutine and releases nothing") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            var finished = false
            var releases = 0
            val warmUp =
                DatabaseWarmUp(
                    dispatcher = StandardTestDispatcher(testScheduler),
                    warmUp = {
                        gate.await()
                        finished = true
                    },
                    release = { releases++ },
                )

            warmUp.start()
            runCurrent()
            warmUp.close()

            assertThat(finished).isFalse()
            assertThat(releases).isZero()

            // Still cancelled afterwards: letting the warm-up finish retroactively must not
            // resurrect a scope the window already gave up on.
            gate.complete(Unit)
            runCurrent()
            assertThat(finished).isFalse()
            assertThat(releases).isZero()
        }
    }

    test("a failing warm-up stays inside the job, so the window still opens and closes") {
        runTest {
            var releases = 0
            val warmUp =
                DatabaseWarmUp(
                    dispatcher = StandardTestDispatcher(testScheduler),
                    warmUp = { throw IllegalStateException("db down") },
                    release = { releases++ },
                )

            warmUp.start()
            // An exception escaping the coroutine here would fail the test rather than the
            // application, which is the whole point of wrapping it.
            runCurrent()
            warmUp.close()

            assertThat(releases).isOne()
        }
    }

    test("a failing warm-up is reported with the failure itself attached") {
        val failure = IllegalStateException("db down")
        var reported: Throwable? = null
        DatabaseWarmUp(
            // Unconfined so `start()` runs the coroutine body to completion on this thread,
            // which keeps the whole thing out of `runTest`'s virtual clock.
            dispatcher = TestDispatchers.unconfined,
            warmUp = { throw failure },
            release = {},
            onFailure = { reported = it },
        ).start()

        // The job swallows the failure so the window still opens, which is exactly why the caller
        // needs a channel of its own: without `onFailure` the failure would exist only as a line
        // in a log. The throwable itself has to ride along, because "the warm-up failed" on its
        // own is not a diagnosable report.
        assertThat(reported).isSameAs(failure)
    }

    test("closing before starting is a no-op rather than a crash") {
        var releases = 0
        val warmUp =
            DatabaseWarmUp(
                dispatcher = TestDispatchers.unconfined,
                warmUp = {},
                release = { releases++ },
            )

        warmUp.close()

        assertThat(releases).isZero()
    }

    test("starting twice is rejected instead of leaking the first job") {
        val warmUp = DatabaseWarmUp(dispatcher = TestDispatchers.unconfined, warmUp = {}, release = {})

        warmUp.start()

        assertThatThrownBy { warmUp.start() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("already been started")
    }
}
