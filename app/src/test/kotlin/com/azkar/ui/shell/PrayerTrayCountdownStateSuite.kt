package com.azkar.ui.shell

import com.azkar.i18n.ENGLISH
import com.azkar.ui.notifications.PrayerLocationSettings
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.time.Duration
import java.time.LocalDateTime

/**
 * The whole-minute countdown the tray shows.
 *
 * [formatRemaining] and [PrayerTrayCountdownState.tick] are pure against their `now`, which is
 * what the acceptance asks for and what a wall-clock read would break: minute formatting, a tick
 * landing past the prayer moment remaining nonnegative, and a holder that has not loaded its
 * resources publishing nothing rather than a half-written line.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val PrayerTrayCountdownStateSuite by testSuite("The tray countdown clock") {
    test("remaining time shows whole minutes without seconds") {
        assertThat(formatRemaining(Duration.ofSeconds(83))).isEqualTo("1m")
        assertThat(formatRemaining(Duration.ofMinutes(45))).isEqualTo("45m")
    }

    test("less than one minute is shown as under a minute") {
        assertThat(formatRemaining(Duration.ofSeconds(59))).isEqualTo("<1m")
        assertThat(formatRemaining(Duration.ofSeconds(1))).isEqualTo("<1m")
    }

    test("hours and remaining minutes are shown without seconds") {
        assertThat(formatRemaining(Duration.ofHours(1))).isEqualTo("1h")
        assertThat(formatRemaining(Duration.ofMinutes(61))).isEqualTo("1h 1m")
        assertThat(formatRemaining(Duration.ofMinutes(79 * 60 + 12))).isEqualTo("79h 12m")
    }

    test("a tick past the prayer moment stays under a minute") {
        assertThat(formatRemaining(Duration.ofMillis(-1))).isEqualTo("<1m")
        assertThat(formatRemaining(Duration.ofMinutes(-90))).isEqualTo("<1m")
    }

    test("seconds do not change the displayed minute") {
        assertThat(formatRemaining(Duration.ofMinutes(10).plusSeconds(30))).isEqualTo("10m")
    }

    test("a holder that has not loaded publishes nothing yet") {
        val state = PrayerTrayCountdownState(ENGLISH, PrayerLocationSettings())

        // The wall clock defaults inside `tick`, but loading is the gate in front of any clock
        // read at all: the tooltip falls back to the app's name until the resources resolve.
        state.tick()

        assertThat(state.tooltipText).isEmpty()
    }

    test("a second tick past a failed load still publishes nothing") {
        val state = PrayerTrayCountdownState(ENGLISH, PrayerLocationSettings())
        state.tick()
        state.tick(now = LocalDateTime.of(2025, 11, 28, 16, 0, 0, 500_000_000))

        assertThat(state.tooltipText).isEmpty()
    }
}
