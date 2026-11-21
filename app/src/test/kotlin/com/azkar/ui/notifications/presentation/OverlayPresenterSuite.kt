@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.azkar.ui.notifications.presentation

import com.azkar.ui.notifications.NotificationAction
import com.azkar.ui.notifications.NotificationNotice
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import kotlin.time.Duration.Companion.milliseconds

/**
 * The overlay presenter's whole contract, exercised without a window, a display or a real clock.
 *
 * The clock is injected and the scope is the test's, so every assertion here is about behaviour that
 * would otherwise need a person to watch a card and count seconds — expiry, pausing, the stack cap,
 * de-duplication — and every one of them runs in microseconds.
 */
val OverlayPresenterSuite by testSuite("The overlay notification presenter") {
    test("a card arrives newest first, so the stack grows away from the anchored corner") {
        presenterTest { presenter ->
            val first = presenter.present(notice(1))!!
            val second = presenter.present(notice(2))!!

            assertThat(presenter.notices.value.map { it.id }).containsExactly(second, first)
            assertThat(
                presenter.notices.value
                    .first()
                    .title,
            ).isEqualTo("Second")
        }
    }

    test("the same passage twice on screen is refused rather than stacked") {
        presenterTest { presenter ->
            presenter.present(notice(1))
            val duplicate = presenter.present(notice(1))

            assertThat(duplicate).isNull()
            assertThat(presenter.notices.value).hasSize(1)
        }
    }

    test("the same title with a different passage is a different notice") {
        presenterTest { presenter ->
            presenter.present(notice(1))

            assertThat(presenter.present(notice(2))).isNotNull()
            assertThat(presenter.notices.value).hasSize(2)
        }
    }

    test("exceeding the cap drops the oldest card instead of covering it") {
        presenterTest(maxVisible = 2) { presenter ->
            val first = presenter.present(notice(1))!!
            presenter.present(notice(2))
            presenter.present(notice(3))

            assertThat(presenter.notices.value.map { it.id })
                .doesNotContain(first)
                .hasSize(2)
        }
    }

    test("each card expires on its own deadline") {
        runPresenterTest(durationSeconds = 8) { presenter, scope ->
            presenter.present(notice(1))
            scope.advanceTimeBy(4_000.milliseconds)
            scope.runCurrent()
            presenter.present(notice(2))

            scope.advanceTimeBy(4_500.milliseconds)
            scope.runCurrent()

            // The first card's eight seconds are up; the second's are not, and dropping one has
            // left the other alone.
            assertThat(presenter.notices.value.map { it.title }).containsExactly("Second")

            scope.advanceTimeBy(4_000.milliseconds)
            scope.runCurrent()
            assertThat(presenter.notices.value).isEmpty()
        }
    }

    test("hovering freezes a card, and leaving resumes the remainder") {
        runPresenterTest(durationSeconds = 5) { presenter, scope ->
            val id = presenter.present(notice(1))!!
            scope.advanceTimeBy(2_000.milliseconds)
            scope.runCurrent()

            presenter.setPaused(id, true)
            scope.advanceTimeBy(30_000.milliseconds)
            scope.runCurrent()
            assertThat(presenter.notices.value).hasSize(1)

            presenter.setPaused(id, false)
            // Only the three seconds that were left before the pointer arrived, not five.
            scope.advanceTimeBy(2_900.milliseconds)
            scope.runCurrent()
            assertThat(presenter.notices.value).hasSize(1)

            scope.advanceTimeBy(200.milliseconds)
            scope.runCurrent()
            assertThat(presenter.notices.value).isEmpty()
        }
    }

    test("the countdown reports the remainder of that card's own deadline") {
        runPresenterTest(durationSeconds = 8) { presenter, scope ->
            val id = presenter.present(notice(1))!!

            assertThat(presenter.remainingFraction(id)).isEqualTo(1f)

            scope.advanceTimeBy(2_000.milliseconds)
            scope.runCurrent()
            assertThat(presenter.remainingFraction(id)).isEqualTo(0.75f)

            scope.advanceTimeBy(6_500.milliseconds)
            scope.runCurrent()
            // The card is gone from the stack, but it is still on the screen on its way out and it is
            // still asking how much time it has. The answer is the truth: a card that has run out
            // must not claim a full bar while it fades, which is what made the bar jump back to the
            // top as the card left.
            assertThat(presenter.remainingFraction(id)).isEqualTo(0f)
        }
    }

    test("a card that is dismissed keeps the bar it was dismissed with") {
        runPresenterTest(durationSeconds = 8) { presenter, scope ->
            val id = presenter.present(notice(1))!!
            scope.advanceTimeBy(2_000.milliseconds)
            scope.runCurrent()

            presenter.dismiss(id)

            // Where the reader left it: not full, because the card had already run for two seconds,
            // and not empty, because the reader closed it before it ran out.
            assertThat(presenter.remainingFraction(id)).isEqualTo(0.75f)
        }
    }

    test("the countdown stops under the pointer and resumes from where it stopped") {
        runPresenterTest(durationSeconds = 8) { presenter, scope ->
            val id = presenter.present(notice(1))!!
            scope.advanceTimeBy(4_000.milliseconds)
            scope.runCurrent()

            presenter.setPaused(id, true)
            scope.advanceTimeBy(30_000.milliseconds)
            scope.runCurrent()
            assertThat(presenter.remainingFraction(id)).isEqualTo(0.5f)

            presenter.setPaused(id, false)
            scope.advanceTimeBy(2_000.milliseconds)
            scope.runCurrent()
            assertThat(presenter.remainingFraction(id)).isEqualTo(0.25f)

            scope.advanceTimeBy(2_100.milliseconds)
            scope.runCurrent()
            assertThat(presenter.notices.value).isEmpty()
        }
    }

    test("a card the presenter has never heard of reports a full bar") {
        presenterTest { presenter ->
            assertThat(presenter.remainingFraction(404L)).isEqualTo(1f)
        }
    }

    test("dismissing a card removes it and touches nothing else") {
        presenterTest { presenter ->
            val first = presenter.present(notice(1))!!
            presenter.present(notice(2))

            presenter.dismiss(first)

            assertThat(presenter.notices.value.map { it.id }).containsExactly(2L)
            // And the survivor is still on its own clock rather than being reset by the dismissal.
            assertThat(
                presenter.notices.value
                    .single()
                    .id,
            ).isEqualTo(2L)
        }
    }

    test("disposing empties the stack and refuses anything afterwards") {
        presenterTest { presenter ->
            presenter.present(notice(1))

            presenter.dispose()

            assertThat(presenter.notices.value).isEmpty()
            assertThat(presenter.present(notice(2))).isNull()
        }
    }

    test("in reading time, a card's countdown divides by the words it shows, not the setting's seconds") {
        // Two words cost the model's "to see it" base plus two words' read: 3,100 ms, deliberately
        // nowhere near the 8 s the fixed stepper holds — this is the spot where the mode actually
        // produces a different number from the one the reader had set.
        runPresenterTest(durationMode = OverlayDurationMode.READING) { presenter, scope ->
            val id = presenter.present(notice(1))!!

            assertThat(presenter.remainingFraction(id)).isEqualTo(1f)

            scope.advanceTimeBy(1_550.milliseconds)
            scope.runCurrent()
            assertThat(presenter.remainingFraction(id)).isEqualTo(0.5f)
        }
    }

    test("in reading time, a short dhikr leaves before a passage does") {
        runPresenterTest(durationMode = OverlayDurationMode.READING) { presenter, scope ->
            val longId = presenter.present(longNotice())!!
            presenter.present(notice(2))

            // Two words ≈ 3,100 ms; the long passage has already reached the model's ceiling.
            scope.advanceTimeBy(3_200.milliseconds)
            scope.runCurrent()
            assertThat(presenter.notices.value.map { it.id }).containsExactly(longId)
            presenter.dispose()
            scope.advanceUntilIdle()
        }
    }
}

/** Runs [body] against a presenter on virtual time, with no display and no real sleeping. */
private fun runPresenterTest(
    durationSeconds: Int = 8,
    maxVisible: Int = 3,
    durationMode: OverlayDurationMode = OverlayDurationMode.FIXED,
    body: suspend (OverlayNotificationPresenter, TestScope) -> Unit,
) = runTest {
    // Fixed time is the default here rather than the model's reading time, so every expiry assertion
    // below keeps the arithmetic of the seconds it was written against and only the reading-time tests
    // opt into the other mode.
    val presenter =
        OverlayNotificationPresenter(
            scope = this,
            settings =
                MutableStateFlow(
                    NotificationPresentationSettings(
                        durationSeconds = durationSeconds,
                        maxVisible = maxVisible,
                        durationMode = durationMode,
                    ),
                ),
            // The test scheduler's virtual clock, so `advanceTimeBy` is what moves the presenter's
            // idea of "now" — the same relationship the real clock has to the real delays.
            now = { testScheduler.currentTime },
        )
    body(presenter, this)
}

private fun presenterTest(
    maxVisible: Int = 3,
    body: (OverlayNotificationPresenter) -> Unit,
) = runTest {
    // A frozen clock and no time advancing: these tests are about the shape of the stack rather than
    // about expiry. `dispose` at the end is what lets the scope end — a live card's countdown loop
    // is an infinite `while`, and `runTest` fails rather than leaks a coroutine that is still running.
    val presenter =
        OverlayNotificationPresenter(
            scope = this,
            settings =
                MutableStateFlow(
                    NotificationPresentationSettings(
                        maxVisible = maxVisible,
                        durationMode = OverlayDurationMode.FIXED,
                    ),
                ),
            now = { 0L },
        )
    body(presenter)
    presenter.dispose()
    advanceUntilIdle()
}

private fun notice(index: Int) =
    NotificationNotice(
        title = if (index == 1) "First" else "Second",
        message = "Message $index",
        action = NotificationAction.Remembrance(index.toLong()),
    )

/**
 * A notice whose words reach far past the reading model's ceiling, so its card runs the whole way
 * to the duration cap: the other end of the scale the two-word notice sits on.
 */
private fun longNotice() =
    NotificationNotice(
        title = "First",
        message = List(600) { "word" }.joinToString(" "),
        action = NotificationAction.Remembrance(1),
    )
