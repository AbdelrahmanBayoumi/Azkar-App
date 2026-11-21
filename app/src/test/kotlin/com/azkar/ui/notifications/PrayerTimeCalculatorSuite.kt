package com.azkar.ui.notifications

import com.azkar.ui.home.PrayerKey
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.time.Duration
import java.time.LocalDate

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val PrayerTimeCalculatorSuite by testSuite("Configured prayer time calculation") {
    test("returns ordered local prayer times for the configured location") {
        val times =
            PrayerTimeCalculator.calculate(
                LocalDate.parse("2026-02-16"),
                PrayerLocationSettings(autoDetect = false),
            )

        assertThat(times.keys).containsExactly(
            PrayerKey.FAJR,
            PrayerKey.DHUHR,
            PrayerKey.ASR,
            PrayerKey.MAGHRIB,
            PrayerKey.ISHA,
        )
        assertThat(times.getValue(PrayerKey.FAJR)).isBefore(times.getValue(PrayerKey.DHUHR))
        assertThat(times.getValue(PrayerKey.DHUHR)).isBefore(times.getValue(PrayerKey.ASR))
        assertThat(times.getValue(PrayerKey.ASR)).isBefore(times.getValue(PrayerKey.MAGHRIB))
        assertThat(times.getValue(PrayerKey.MAGHRIB)).isBefore(times.getValue(PrayerKey.ISHA))
    }

    test("uses the selected calculation preset and timezone") {
        val date = LocalDate.parse("2026-02-16")
        val defaultLocation = PrayerLocationSettings(autoDetect = false)
        val northAmerica = defaultLocation.copy(method = PrayerCalculationMethod.NORTH_AMERICA)
        val utc = defaultLocation.copy(zoneId = "UTC")

        val fajrDefault = PrayerTimeCalculator.calculate(date, defaultLocation).getValue(PrayerKey.FAJR)
        val fajrNorthAmerica = PrayerTimeCalculator.calculate(date, northAmerica).getValue(PrayerKey.FAJR)
        val dhuhrLocal = PrayerTimeCalculator.calculate(date, defaultLocation).getValue(PrayerKey.DHUHR)
        val dhuhrUtc = PrayerTimeCalculator.calculate(date, utc).getValue(PrayerKey.DHUHR)

        assertThat(fajrNorthAmerica).isNotEqualTo(fajrDefault)
        assertThat(Duration.between(dhuhrUtc, dhuhrLocal).toMinutes()).isEqualTo(120)
    }
}
