package com.azkar.ui.components

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalTime
import java.util.Locale

/**
 * [AzkarClock] and the typed half of [AzkarTimePicker].
 *
 * The meridiem test is the one that matters most and is deliberately written against the *runtime's*
 * spelling rather than against the parser's own table: if a locale ever starts writing its meridiem
 * differently, this is the test that notices, rather than a reader discovering that the typed field
 * has quietly stopped accepting the afternoons.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val AzkarClockSuite by testSuite("Every clock the app prints, and the typed half of the time picker") {
    val english = Locale.ENGLISH
    val arabic = Locale.forLanguageTag("ar")

    fun am(
        hour: Int,
        minute: Int = 0,
    ) = "(?i)$hour:%02d[\\s\\u00a0\\u202f]*am".format(Locale.ROOT, minute)

    fun pm(
        hour: Int,
        minute: Int = 0,
    ) = "(?i)$hour:%02d[\\s\\u00a0\\u202f]*pm".format(Locale.ROOT, minute)

    /**
     * The meridiem a formatted time ends with.
     *
     * Split on an explicit class rather than `trim()`, because the space CLDR puts in front of a
     * meridiem is U+202F NARROW NO-BREAK SPACE and `Char.isWhitespace` calls that one *not*
     * whitespace — so `substringAfter(' ').trim()` finds nothing at all on English.
     */
    fun marker(formatted: String): String = formatted.split(Regex("[\\s\\u00A0\\u202F]+")).last()

    test("times are written on a twelve-hour clock in every locale") {
        // The casing of the meridiem and the space in front of it are both JDK/CLDR decisions rather
        // than this app's, so the assertions are on the shape rather than on one exact rendering.
        assertThat(AzkarClock.format(8, 0, english)).matches(am(8))
        assertThat(AzkarClock.format(13, 0, english)).matches(pm(1))
        assertThat(AzkarClock.format(22, 0, english)).matches(pm(10))
        // Noon is PM and midnight is AM even though both print a twelve, which is the whole reason a
        // bare "12" is refused by the parser and a bare hour has to mean one thing.
        assertThat(AzkarClock.format(12, 0, english)).matches(pm(12))
        assertThat(AzkarClock.format(0, 0, english)).matches(am(12))
    }

    test("an out-of-range hour and minute clamp instead of throwing") {
        // A corrupted preference must not take the settings screen down with it.
        assertThat(AzkarClock.format(25, 0, english)).matches(pm(11))
        assertThat(AzkarClock.format(8, 99, english)).matches(am(8, 59))
    }

    test("minutes are padded to two digits, in the locale's own numerals") {
        assertThat(AzkarClock.formatMinuteOfHour(5, english)).isEqualTo("05")
        assertThat(AzkarClock.formatMinuteOfHour(35, english)).isEqualTo("35")
        // This JVM prints Latin digits for Arabic; the two agreeing is the point, so the assertion is
        // the equality rather than a literal numeral set.
        assertThat(AzkarClock.formatMinuteOfHour(35, arabic))
            .isEqualTo(AzkarClock.formatMinuteOfHour(35, english))
    }

    test("the formatter cache is keyed by locale, not by the string it wraps") {
        assertThat(AzkarClock.timeFormatter(Locale.ENGLISH))
            .isEqualTo(AzkarClock.timeFormatter(Locale.forLanguageTag("en")))
        assertThat(AzkarClock.format(LocalTime.of(6, 30), english))
            .isEqualTo(AzkarClock.format(6, 30, english))
    }

    test("the meridiem table matches what the formatters actually emit") {
        val morning = marker(AzkarClock.format(9, 0, english))
        val evening = marker(AzkarClock.format(21, 0, english))

        assertThat(AzkarClockParse.isEvening(morning)).isFalse()
        assertThat(AzkarClockParse.isEvening(evening)).isTrue()
        assertThat(AzkarClockParse.isEvening(marker(AzkarClock.format(9, 0, arabic)))).isFalse()
        assertThat(AzkarClockParse.isEvening(marker(AzkarClock.format(21, 0, arabic)))).isTrue()
    }

    test("every meridiem the parser knows is spelled the way a formatter spells it") {
        // Guards against a table entry no locale here ever produces, which would be a marker this app
        // honours but nothing writes.
        val written =
            listOf(english, arabic)
                .flatMap { locale ->
                    listOf(AzkarClock.format(9, 0, locale), AzkarClock.format(21, 0, locale))
                        .map { marker(it) }
                }.toSet()
        assertThat(written).isNotEmpty()
        assertThat(AzkarClockParse.meridiemMarkers()).containsAll(written.map { it.lowercase(Locale.ROOT) })
    }

    test("the strings the parser accepts include the ones the formatter produces") {
        listOf(0, 8 * 60, 8 * 60 + 30, 12 * 60, 12 * 60 + 5, 20 * 60 + 30, 23 * 60 + 59).forEach { minute ->
            assertThat(AzkarClock.parse(AzkarClock.format(minute, english)))
                .describedAs("round trip of %d", minute)
                .isEqualTo(minute)
        }
    }

    test("a bare hour reads as morning, because that is the clock the field shows") {
        assertThat(AzkarClock.parse("8")).isEqualTo(8 * 60)
        assertThat(AzkarClock.parse("8:45")).isEqualTo(8 * 60 + 45)
        assertThat(AzkarClock.parse("08:45")).isEqualTo(8 * 60 + 45)
    }

    test("hours above twelve are taken at face value") {
        assertThat(AzkarClock.parse("20:00")).isEqualTo(20 * 60)
        assertThat(AzkarClock.parse("23:59")).isEqualTo(23 * 60 + 59)
        assertThat(AzkarClock.parse("13")).isEqualTo(13 * 60)
    }

    test("a meridiem overrides the twelve-hour reading") {
        assertThat(AzkarClock.parse("8 PM")).isEqualTo(20 * 60)
        assertThat(AzkarClock.parse("8pm")).isEqualTo(20 * 60)
        assertThat(AzkarClock.parse("8:45 p.m.")).isEqualTo(20 * 60 + 45)
        assertThat(AzkarClock.parse("8 AM")).isEqualTo(8 * 60)
        assertThat(AzkarClock.parse("12 AM")).isEqualTo(0)
        assertThat(AzkarClock.parse("12 PM")).isEqualTo(12 * 60)
    }

    test("noon is the one bare hour that is refused") {
        // 12 in the morning and 12 at night are twelve hours apart, and there is no defensible guess.
        assertThat(AzkarClock.parse("12")).isNull()
    }

    test("thirteen o'clock in the afternoon is refused rather than folded") {
        // Folding it to 1pm would silently lose twelve hours off a typo.
        assertThat(AzkarClock.parse("13 PM")).isNull()
        assertThat(AzkarClock.parse("13 pm")).isNull()
    }

    test("half-written input parses to nothing, so the field can hold what was typed") {
        listOf("", " ", ":", "9:", ":30", "am", "9 am pm", "half past nine", "25:00", "8:99", "9:45:30")
            .forEach { typed ->
                assertThat(AzkarClock.parse(typed)).describedAs("%s", typed).isNull()
            }
    }

    test("the exotic spaces CLDR puts before a meridiem are not a parse failure") {
        val formatted = AzkarClock.format(20 * 60 + 30, english)
        assertThat(formatted).contains("\u202F")
        assertThat(AzkarClock.parse(formatted)).isEqualTo(20 * 60 + 30)
    }

    test("arabic-indic digits are read as the numbers they are") {
        // ٨:٤٥ is eight forty-five and ٨:٠٠ ص is eight in the morning. Written without the colon the
        // same way it is read on a clock, ٨٠٠ ص is eight hundred o'clock and is refused as out of range.
        assertThat(AzkarClock.parse("٨:٤٥")).isEqualTo(8 * 60 + 45)
        assertThat(AzkarClock.parse("٨:٠٠ ص")).isEqualTo(8 * 60)
        assertThat(AzkarClock.parse("٨٠٠ ص")).isNull()
    }

    test("every minute of a day is a legal value to store, and nothing beyond it is") {
        assertThat(AzkarClock.isValidMinuteOfDay(0)).isTrue()
        assertThat(AzkarClock.isValidMinuteOfDay(24 * 60 - 1)).isTrue()
        assertThat(AzkarClock.isValidMinuteOfDay(-1)).isFalse()
        assertThat(AzkarClock.isValidMinuteOfDay(24 * 60)).isFalse()
    }
}
