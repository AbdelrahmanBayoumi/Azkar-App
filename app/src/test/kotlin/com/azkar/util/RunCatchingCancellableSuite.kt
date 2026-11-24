package com.azkar.util

import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CancellationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val RunCatchingCancellableSuite by testSuite("Cancellable result handling") {
    test("returns values and captures ordinary failures") {
        val failure = IllegalStateException("failed")
        assertThat(runCatchingCancellable { 42 }.getOrThrow()).isEqualTo(42)
        assertThat(runCatchingCancellable<Int> { throw failure }.exceptionOrNull()).isSameAs(failure)
    }

    test("propagates cancellation instead of capturing it") {
        val cancellation = CancellationException("cancelled")
        assertThatThrownBy { runCatchingCancellable { throw cancellation } }.isSameAs(cancellation)
    }
}
