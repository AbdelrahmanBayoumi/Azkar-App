package com.azkar.ui.shell

import com.azkar.generated.resources.Res
import com.azkar.generated.resources.desktopPrayerTomorrowTime
import com.azkar.i18n.ENGLISH
import com.azkar.ui.components.AzkarClock
import com.azkar.ui.home.PrayerKey
import com.azkar.ui.home.name
import com.azkar.ui.notifications.PrayerLocationSettings
import com.azkar.ui.notifications.PrayerTimeCalculator
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.compose.resources.getString
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

@Suppress("unused")
val DesktopPrayerStatusStateSuite by testSuite("The desktop prayer countdown") {
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
        val location = PrayerLocationSettings()
        val state = DesktopPrayerStatusState(ENGLISH, location)
        state.tick(LocalDate.of(2025, 11, 28).atTime(16, 0).atZone(ZoneId.of(location.zoneId)))
        assertThat(state.status.value).isNull()
    }

    test("the same snapshot is shared until its displayed minute changes") {
        val location = PrayerLocationSettings()
        val date = LocalDate.of(2026, 1, 19)
        val times = PrayerTimeCalculator.calculate(date, location)
        val asr = date.atTime(times.getValue(PrayerKey.ASR)).atZone(ZoneId.of(location.zoneId))
        val state = DesktopPrayerStatusState(ENGLISH, location)
        state.start()

        state.tick(asr.minusMinutes(43).minusSeconds(10))
        val status = requireNotNull(state.status.value)
        assertThat(status.prayerName).isEqualTo(PrayerKey.ASR.name())
        assertThat(status.timeLabel).isEqualTo(AzkarClock.format(asr.toLocalTime(), ENGLISH))
        assertThat(status.remainingLabel).isEqualTo("43m")
        assertThat(status.countdownLabel).contains(status.prayerName, status.remainingLabel)
        assertThat(status.today.map { it.key }).containsExactlyElementsOf(PrayerKey.entries)
        assertThat(status.today.map { it.value }).containsExactlyElementsOf(
            PrayerKey.entries.map { AzkarClock.format(times.getValue(it), ENGLISH) },
        )
        assertThat(status.nextIsTomorrow).isFalse()

        state.tick(asr.minusMinutes(43))
        assertThat(state.status.value).isSameAs(status)

        state.tick(asr.plusSeconds(1))
        assertThat(state.status.value?.prayerName).isEqualTo(PrayerKey.MAGHRIB.name())
        assertThat(
            state.status.value?.timeLabel,
        ).isEqualTo(AzkarClock.format(times.getValue(PrayerKey.MAGHRIB), ENGLISH))
    }

    test("after Isha the time and countdown account for tomorrow's clock change") {
        val location = PrayerLocationSettings(latitude = 40.7128, longitude = -74.0060, zoneId = "America/New_York")
        val date = LocalDate.of(2026, 3, 7)
        val zone = ZoneId.of(location.zoneId)
        val tomorrow = date.plusDays(1)
        val tomorrowFajr = PrayerTimeCalculator.calculate(tomorrow, location).getValue(PrayerKey.FAJR)
        val state = DesktopPrayerStatusState(ENGLISH, location)
        state.start()

        val now = date.atTime(23, 59).atZone(zone)
        state.tick(now)

        val status = requireNotNull(state.status.value)
        val elapsed = Duration.between(now, tomorrow.atTime(tomorrowFajr).atZone(zone))
        assertThat(status.prayerName).isEqualTo(PrayerKey.FAJR.name())
        val tomorrowTime = AzkarClock.format(tomorrowFajr, ENGLISH)
        assertThat(status.timeLabel).isEqualTo(getString(Res.string.desktopPrayerTomorrowTime, tomorrowTime))
        assertThat(status.remainingLabel).isEqualTo(formatRemaining(elapsed))
        assertThat(status.nextIsTomorrow).isTrue()
        val todayFajr = PrayerTimeCalculator.calculate(date, location).getValue(PrayerKey.FAJR)
        assertThat(status.today.first().value).isEqualTo(AzkarClock.format(todayFajr, ENGLISH))

        state.tick(tomorrow.atStartOfDay(zone))
        val afterMidnight = requireNotNull(state.status.value)
        assertThat(afterMidnight.timeLabel).isEqualTo(tomorrowTime)
        assertThat(afterMidnight.today.first().value).isEqualTo(tomorrowTime)
        assertThat(afterMidnight.nextIsTomorrow).isFalse()
    }
}
