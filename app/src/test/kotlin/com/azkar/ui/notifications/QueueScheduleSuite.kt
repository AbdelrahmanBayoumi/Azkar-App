package com.azkar.ui.notifications

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDateTime
import kotlin.random.Random

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val QueueScheduleSuite by testSuite("The shared reminder queue schedule") {
    test("emits the latest elapsed slot inside a daily window") {
        val slot = QueueSchedule.dueSlot(LocalDateTime.parse("2026-10-14T10:37:00"), 8 * 60, 22 * 60, 60)

        assertThat(slot).isEqualTo(LocalDateTime.parse("2026-10-14T10:00:00"))
        assertThat(QueueSchedule.dueSlot(LocalDateTime.parse("2026-10-14T07:59:00"), 480, 1320, 60)).isNull()
        assertThat(QueueSchedule.dueSlot(LocalDateTime.parse("2026-10-14T22:00:00"), 480, 1320, 60)).isNull()
    }

    test("a five-minute queue reaches the next slot after an 11:10 delivery") {
        val lastSlot = QueueSchedule.dueSlot(LocalDateTime.parse("2026-10-14T11:10:00"), 8 * 60, 22 * 60, 5)
        val nextSlot = QueueSchedule.dueSlot(LocalDateTime.parse("2026-10-14T11:15:00"), 8 * 60, 22 * 60, 5)

        assertThat(lastSlot).isEqualTo(LocalDateTime.parse("2026-10-14T11:10:00"))
        assertThat(nextSlot).isEqualTo(LocalDateTime.parse("2026-10-14T11:15:00"))
        assertThat(nextSlot).isNotEqualTo(lastSlot)
    }

    test("keeps interval slots continuous through an overnight window") {
        val slot = QueueSchedule.dueSlot(LocalDateTime.parse("2026-10-15T01:10:00"), 22 * 60, 4 * 60, 90)

        assertThat(slot).isEqualTo(LocalDateTime.parse("2026-10-15T01:00:00"))
    }

    test("manual mode keeps the order it was given, ignoring duplicates") {
        val order =
            QueueSchedule.cycleOrder(
                itemIds = listOf(4, 2, 4, 1),
                mode = QueueMode.MANUAL,
                dropChance = { 0 },
            )

        assertThat(order).containsExactly(4, 2, 1)
    }

    test("weighted mode treats the value as a per-cycle skip chance") {
        val noneSkipped =
            QueueSchedule.cycleOrder(
                itemIds = listOf(1, 2, 3),
                mode = QueueMode.WEIGHTED,
                dropChance = { 0 },
                random = Random(7),
            )
        val partial =
            QueueSchedule.cycleOrder(
                itemIds = listOf(1, 2, 3),
                mode = QueueMode.WEIGHTED,
                dropChance = { if (it == 3L) 0 else 100 },
                random = Random(7),
            )
        val allSkipped =
            QueueSchedule.cycleOrder(
                itemIds = listOf(1, 2, 3),
                mode = QueueMode.WEIGHTED,
                dropChance = { 100 },
                random = Random(7),
            )

        assertThat(noneSkipped).containsExactlyInAnyOrder(1, 2, 3)
        assertThat(partial).containsExactly(3)
        assertThat(allSkipped).isEmpty()
    }

    test("the next slot is the one after now, never the one now is already sitting on") {
        val next =
            QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-14T10:37:00"), 8 * 60, 22 * 60, 60)

        assertThat(next).isEqualTo(LocalDateTime.parse("2026-10-14T11:00:00"))
        // Sitting exactly on a slot: that slot is the one just delivered, so the one after is due.
        assertThat(QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-14T10:00:00"), 480, 1320, 60))
            .isEqualTo(LocalDateTime.parse("2026-10-14T11:00:00"))
    }

    test("before the window opens the next slot is its opening, not an interval boundary") {
        val next = QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-14T06:12:00"), 480, 1320, 90)

        assertThat(next).isEqualTo(LocalDateTime.parse("2026-10-14T08:00:00"))
    }

    test("the last interval slot of the day is the end of the window, so nothing follows it") {
        assertThat(QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-14T21:30:00"), 480, 1320, 60))
            .isNull()
        // Already closed: the runtime will not deliver again until tomorrow opens, and neither does
        // the preview, because claiming a slot a day away would describe a schedule that is not set.
        assertThat(QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-14T23:30:00"), 480, 1320, 60))
            .isNull()
    }

    test("an overnight window counts the morning gap as still waiting for tonight") {
        // 22:00 -> 04:00, so at five in the morning the window closed four hours ago and reopens
        // this evening. The naive answer is "tomorrow 22:00", which would skip the next real slot.
        val next = QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-15T05:00:00"), 22 * 60, 4 * 60, 90)

        assertThat(next).isEqualTo(LocalDateTime.parse("2026-10-15T22:00:00"))
        assertThat(QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-15T23:00:00"), 1320, 240, 90))
            .isEqualTo(LocalDateTime.parse("2026-10-15T23:30:00"))
        assertThat(QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-15T01:10:00"), 1320, 240, 90))
            .isEqualTo(LocalDateTime.parse("2026-10-15T02:30:00"))
        assertThat(QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-15T03:30:00"), 1320, 240, 90))
            .isNull()
    }

    test("a malformed window is rejected the same way whichever way it is asked for") {
        assertThatThrownBy { QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-14T10:00:00"), -1, 1320, 60) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("startMinute")
        assertThatThrownBy { QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-14T10:00:00"), 480, 480, 60) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("endMinute")
        assertThatThrownBy { QueueSchedule.nextSlot(LocalDateTime.parse("2026-10-14T10:00:00"), 480, 1320, 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("intervalMinutes")
    }

    test("a weekday set decides whether the queue runs at all, and an empty one means no") {
        val wednesdayNoon = LocalDateTime.parse("2026-10-14T12:00:00")

        assertThat(QueueSchedule.isDayActive(wednesdayNoon, ReminderQueueConfig.ALL_DAYS)).isTrue()
        assertThat(QueueSchedule.isDayActive(wednesdayNoon, ReminderQueueConfig.WEEKDAYS)).isTrue()
        assertThat(QueueSchedule.isDayActive(wednesdayNoon, ReminderQueueConfig.WEEKEND)).isFalse()
        // Silencing rather than defaulting to "every day" is deliberate: a stored value written
        // before the field existed must not resume a queue the user had believed was off.
        assertThat(QueueSchedule.isDayActive(wednesdayNoon, emptySet())).isFalse()
    }

    test("the next slot for a weekday queue skips the days it does not run on") {
        val saturdayMorning = LocalDateTime.parse("2026-10-17T09:00:00")

        assertThat(
            QueueSchedule.nextSlotOnActiveDays(saturdayMorning, 480, 1320, 60, ReminderQueueConfig.WEEKDAYS),
        ).isEqualTo(LocalDateTime.parse("2026-10-19T08:00:00"))
        assertThat(
            QueueSchedule.nextSlotOnActiveDays(saturdayMorning, 480, 1320, 60, ReminderQueueConfig.WEEKEND),
        ).isEqualTo(LocalDateTime.parse("2026-10-17T10:00:00"))
        assertThat(
            QueueSchedule.nextSlotOnActiveDays(saturdayMorning, 480, 1320, 60, ReminderQueueConfig.ALL_DAYS),
        ).isEqualTo(LocalDateTime.parse("2026-10-17T10:00:00"))
    }

    test("the next slot for a weekday queue falls to the next allowed day once today's window closes") {
        // 22:00 -> 04:00 means the window closed at four this morning, so a Saturday night reader of a
        // weekday-only queue is told about Monday morning rather than about tonight's window that has
        // already been and gone.
        val saturdayNight = LocalDateTime.parse("2026-10-17T23:00:00")

        assertThat(
            QueueSchedule.nextSlotOnActiveDays(saturdayNight, 1320, 240, 60, ReminderQueueConfig.WEEKDAYS),
        ).isEqualTo(LocalDateTime.parse("2026-10-19T22:00:00"))
        assertThat(
            QueueSchedule.nextSlotOnActiveDays(saturdayNight, 480, 1320, 60, ReminderQueueConfig.ALL_DAYS),
        ).isEqualTo(LocalDateTime.parse("2026-10-18T08:00:00"))
    }

    test("both cadences speak on the interval the user chose") {
        // The bug this pins: continuous used to override the interval with a fixed five minutes, which
        // threw away the user's own setting and fired 168 times across a fourteen-hour window.
        assertThat(
            ReminderQueueConfig(cadence = QueueCadence.CONTINUOUS, intervalMinutes = 30).effectiveIntervalMinutes,
        ).isEqualTo(30)
        assertThat(
            ReminderQueueConfig(cadence = QueueCadence.CONTINUOUS, intervalMinutes = 120).effectiveIntervalMinutes,
        ).isEqualTo(120)
        assertThat(
            ReminderQueueConfig(cadence = QueueCadence.INTERVAL, intervalMinutes = 30).effectiveIntervalMinutes,
        ).isEqualTo(30)
    }

    test("a continuous queue has a slot on every minute of the day") {
        val noon = LocalDateTime.parse("2026-10-14T12:03:00")

        // Outside a window it still answers, which is the whole difference from the interval queue: there
        // is no hour of the day at which a continuous queue has nothing due.
        assertThat(ContinuousSchedule.dueSlot(noon, 5)).isEqualTo(LocalDateTime.parse("2026-10-14T12:00:00"))
        assertThat(ContinuousSchedule.nextSlot(noon, 5)).isEqualTo(LocalDateTime.parse("2026-10-14T12:05:00"))
        assertThat(ContinuousSchedule.nextSlot(LocalDateTime.parse("2026-10-14T23:58:00"), 5))
            .isEqualTo(LocalDateTime.parse("2026-10-15T00:00:00"))
    }

    test("a continuous queue does not stop when its window would have closed") {
        val config = ReminderQueueConfig(enabled = true, cadence = QueueCadence.CONTINUOUS, intervalMinutes = 30)
        // 22:30 is long past the 08:00-22:00 window an interval queue would have been finished with.
        val lateEvening = LocalDateTime.parse("2026-10-14T22:30:00")

        assertThat(
            QueueSchedule.nextSlotOnActiveDays(
                lateEvening,
                config.windowStartMinute,
                config.windowEndMinute,
                30,
                config.activeDays,
            ),
        ).isEqualTo(LocalDateTime.parse("2026-10-15T08:00:00"))
        assertThat(
            ContinuousSchedule.nextSlotOnActiveDays(
                lateEvening,
                config.effectiveIntervalMinutes,
                config.activeDays,
            ),
        ).isEqualTo(LocalDateTime.parse("2026-10-14T23:00:00"))
    }

    test("a continuous queue still waits for a day it is switched on") {
        val saturday = LocalDateTime.parse("2026-10-17T12:00:00")

        assertThat(QueueSchedule.isDayActive(saturday, ReminderQueueConfig.WEEKDAYS)).isFalse()
        assertThat(
            ContinuousSchedule.nextSlotOnActiveDays(
                saturday,
                intervalMinutes = 30,
                activeDays = ReminderQueueConfig.WEEKDAYS,
            ),
        ).isEqualTo(LocalDateTime.parse("2026-10-19T00:00:00"))
        assertThat(
            ContinuousSchedule.nextSlotOnActiveDays(
                saturday,
                intervalMinutes = 30,
                activeDays = ReminderQueueConfig.ALL_DAYS,
            ),
        ).isEqualTo(LocalDateTime.parse("2026-10-17T12:30:00"))
    }
}
