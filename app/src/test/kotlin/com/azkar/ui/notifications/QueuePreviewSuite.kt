package com.azkar.ui.notifications

import com.azkar.ui.library.remembrance
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * What the queue will say next, as the library's look-ahead shows it.
 *
 * The look-ahead is a promise about the future, and a promise the library cannot keep is worse than
 * no promise. These are the rules that keep it honest: the runtime's own cycle is asked first, the
 * library's order is the fallback, and a stale answer is never shown.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val QueuePreviewSuite by testSuite("Peeking at what the queue will say") {
    val morning = remembrance(id = 1L, text = "Whoever says good morning")
    val evening = remembrance(id = 2L, text = "Whoever says good evening")
    val travel = remembrance(id = 3L, text = "The traveller's supplication")

    val reading =
        QueueReading(
            orderedIds = listOf(1L, 2L, 3L),
            remembrances = listOf(morning, evening, travel),
        )

    test("a queue that has never run shows the library's own order") {
        val upcoming =
            upcomingPassages(
                progress = ReminderQueueProgress(),
                reading = reading,
                count = 2,
            )

        assertThat(upcoming.map { it.id }).containsExactly(1L, 2L)
    }

    test("the runtime's own cycle is asked first, from where it got to") {
        val progress =
            ReminderQueueProgress(
                cycleOrder = listOf(3L, 1L, 2L),
                nextIndex = 1,
            )

        val upcoming = upcomingPassages(progress = progress, reading = reading, count = 2)

        assertThat(upcoming.map { it.id }).containsExactly(1L, 2L)
    }

    test("a cycle left over from other sources is not shown") {
        val progress =
            ReminderQueueProgress(
                cycleOrder = listOf(7L, 8L),
                nextIndex = 0,
            )

        val upcoming = upcomingPassages(progress = progress, reading = reading, count = 2)

        assertThat(upcoming.map { it.id }).containsExactly(1L, 2L)
    }

    test("the list starts where the queue is, and never repeats itself") {
        val progress =
            ReminderQueueProgress(
                cycleOrder = listOf(2L, 3L),
                nextIndex = 1,
            )

        val upcoming = upcomingPassages(progress = progress, reading = reading, count = 4)

        // The queue is on its second passage, so the list starts there — and it does not pad itself
        // with the two it has already shown, because the queue will not say them twice in a row.
        assertThat(upcoming.map { it.id }).containsExactly(3L, 2L)
    }

    test("a queue with nothing to say says nothing") {
        val upcoming =
            upcomingPassages(
                progress = ReminderQueueProgress(),
                reading = QueueReading(orderedIds = emptyList(), remembrances = emptyList()),
                count = 5,
            )

        assertThat(upcoming).isEmpty()
    }
}
