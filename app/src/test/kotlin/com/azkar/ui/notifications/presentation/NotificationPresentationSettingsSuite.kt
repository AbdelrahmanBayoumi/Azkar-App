package com.azkar.ui.notifications.presentation

import androidx.compose.ui.unit.dp
import com.azkar.i18n.ARABIC
import com.azkar.ui.notifications.NotificationAction
import com.azkar.ui.notifications.NotificationContent
import com.azkar.ui.notifications.NotificationNotice
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * The persisted form of the presentation settings, asserted through the same encoder the repository
 * uses.
 *
 * This exists because nothing else could have caught the bug it exists to catch. The class carries a
 * `private companion object` for its bounds, the serialization plugin puts the generated serializer
 * outside the class, and the first encode at runtime failed with an `IllegalAccessError` on
 * `Companion` — after compilation succeeded, after every other test passed, and only once a person
 * actually moved a switch in the manager. A round trip through `Json` is the cheapest thing that can
 * prove the access is legal, so it is a test rather than a note in the code.
 *
 * The `Json` configuration is duplicated deliberately: mirroring it is what makes the failure mode
 * reproducible here, and a test that used a laxer configuration than production would prove less.
 */
private val json = Json { ignoreUnknownKeys = true }

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val NotificationPresentationSettingsSuite by testSuite("The notification presentation settings") {
    test("they survive a trip through the store") {
        val settings =
            NotificationPresentationSettings(
                durationSeconds = 25,
                placement = OverlayPlacement.BOTTOM_START,
                maxVisible = 5,
                cardWidthDp = 480,
                cardHeightDp = 250,
                notificationLanguageTag = "ar",
            )

        assertThat(json.decodeFromString<NotificationPresentationSettings>(json.encodeToString(settings)))
            .isEqualTo(settings)
    }

    test("every corner has a name the store can hold") {
        assertThat(OverlayPlacement.entries.map { it.name })
            .containsExactly("TOP_START", "TOP_END", "BOTTOM_START", "BOTTOM_END")
    }

    test("an unset preference reads as the defaults, not as a failure") {
        // What the repository does with a key that was never written, and with a value an older
        // version of the app wrote in a shape this one no longer knows.
        val absent = json.decodeFromString<NotificationPresentationSettings>("""{}""")

        assertThat(absent).isEqualTo(NotificationPresentationSettings())
        assertThat(absent.placement).isEqualTo(OverlayPlacement.TOP_END)
    }

    test("an out-of-range stored value is refused rather than half-accepted") {
        // The bounds are a `require` in the constructor, which is the right place for them: an
        // out-of-range model cannot exist, so nothing downstream has to re-check. The cost is that
        // the stored blob is validated on the way *in*, so a value that could never have been written
        // by this version fails to decode — and the repository's `decode` turns exactly that into
        // "fall back to the defaults", which is what this asserts.
        val stored = """{"durationSeconds":9000,"placement":"TOP_END","maxVisible":99}"""

        assertThat(runCatching { json.decodeFromString<NotificationPresentationSettings>(stored) }.getOrNull())
            .isNull()
        // And the fallback is a usable model rather than a blank one, `validated()` included.
        assertThat(NotificationPresentationSettings().validated()).isEqualTo(NotificationPresentationSettings())
    }

    test("a setting the app no longer has is dropped rather than fatal") {
        // The OS notification switch went with the channel it controlled. A machine that had turned
        // it on still holds a blob naming it, and because the reader is configured to ignore unknown
        // fields that blob still decodes — with every setting it still has, rather than being reset
        // to the defaults because one stale key is in it.
        val decoded =
            json.decodeFromString<NotificationPresentationSettings>(
                """{"systemNotificationsEnabled":true,"durationSeconds":14,"maxVisible":2}""",
            )

        assertThat(decoded.durationSeconds).isEqualTo(14)
        assertThat(decoded.maxVisible).isEqualTo(2)
        assertThat(decoded).isEqualTo(NotificationPresentationSettings(durationSeconds = 14, maxVisible = 2))
    }

    test("an unknown field from a newer version is ignored rather than fatal") {
        val decoded =
            json.decodeFromString<NotificationPresentationSettings>(
                """{"durationSeconds":12,"somethingNewer":true}""",
            )

        assertThat(decoded.durationSeconds).isEqualTo(12)
    }

    test("a blob written before the card had a size reads as the comfortable preset") {
        // The other half of the unknown-field rule, and the one that actually bites: a *missing*
        // field takes its default rather than being reported, so adding the size to the model did not
        // invalidate every stored setting on every machine that had one.
        val beforeSizes =
            json.decodeFromString<NotificationPresentationSettings>(
                """{"durationSeconds":12,"placement":"BOTTOM_END","maxVisible":2}""",
            )

        assertThat(beforeSizes.cardSizePreset).isEqualTo(OverlayCardSize.COMFORTABLE)
        assertThat(beforeSizes.cardWidth).isEqualTo(400.dp)
        assertThat(beforeSizes.cardHeight).isEqualTo(200.dp)
    }

    test("every preset sits exactly on a slider stop in both dimensions") {
        // The step mirrors `DIMENSION_SLIDER_STEP` in the settings screen; both sliders share it and
        // both range minimums (300 and 120) are whole multiples of it, so a preset dimension that is a
        // multiple of ten lands on a bar. A preset that did not would park the thumb between two bars,
        // and the reader would see a card size the presets do not describe. The concrete values are
        // pinned as well so that moving a preset is a deliberate, visible change to this test.
        val step = 10

        assertThat(OverlayCardSize.entries.map { it.widthDp }).containsExactly(320, 400, 480)
        assertThat(OverlayCardSize.entries.map { it.heightDp }).containsExactly(150, 200, 250)
        OverlayCardSize.entries.forEach { preset ->
            assertThat(preset.widthDp % step).isZero()
            assertThat(preset.heightDp % step).isZero()
        }
    }

    test("every preset round-trips to itself, and a size between two of them to none") {
        // The pair is the setting, so a preset that did not survive the round trip would leave the
        // buttons un-highlighted after a restart — the card would be the right size with no pill lit.
        OverlayCardSize.entries.forEach { preset ->
            val stored =
                NotificationPresentationSettings(
                    cardWidthDp = preset.widthDp,
                    cardHeightDp = preset.heightDp,
                )
            val decoded = json.decodeFromString<NotificationPresentationSettings>(json.encodeToString(stored))

            assertThat(decoded.cardSizePreset).isEqualTo(preset)
        }
        assertThat(NotificationPresentationSettings(cardWidthDp = 430, cardHeightDp = 200).cardSizePreset).isNull()
    }

    test("a card size outside the bounds is refused rather than half-accepted") {
        // Same rule as the duration and the stack cap, and for the same reason: a card that cannot be
        // built is better than a card the overlay window would size from a number it cannot lay out.
        val tooNarrow = """{"cardWidthDp":100,"cardHeightDp":196}"""
        val tooShort = """{"cardWidthDp":400,"cardHeightDp":40}"""

        assertThat(runCatching { json.decodeFromString<NotificationPresentationSettings>(tooNarrow) }.getOrNull())
            .isNull()
        assertThat(runCatching { json.decodeFromString<NotificationPresentationSettings>(tooShort) }.getOrNull())
            .isNull()
    }

    test("the window's size key moves for each of the three numbers it is derived from") {
        // A window that resized on only some of them is a card one dimension too wide inside a window
        // too narrow for it, which shows up as a clipped card that no setting can then explain.
        val base = NotificationPresentationSettings()

        assertThat(base.cardSizeKey)
            .isNotEqualTo(base.copy(cardWidthDp = base.cardWidthDp + 10).cardSizeKey)
            .isNotEqualTo(base.copy(cardHeightDp = base.cardHeightDp + 10).cardSizeKey)
            .isNotEqualTo(base.copy(maxVisible = base.maxVisible + 1).cardSizeKey)
    }

    test("the default card follows its passage, and a fixed slot survives a round trip") {
        // Fit-to-text is the default, so the mode has to come back exactly as it was written rather
        // than being normalised to the default on the way in: a reader who pinned a fixed slot and
        // found every card fitted again after a restart would have a setting that lasted one session.
        val default = NotificationPresentationSettings()

        assertThat(default.cardSizing).isEqualTo(OverlayCardSizing.FIT)
        assertThat(default.usesFittedHeight).isTrue()

        val fixed = NotificationPresentationSettings(cardSizing = OverlayCardSizing.FIXED)
        val decoded = json.decodeFromString<NotificationPresentationSettings>(json.encodeToString(fixed))

        assertThat(fixed.usesFittedHeight).isFalse()
        assertThat(decoded.cardSizing).isEqualTo(OverlayCardSizing.FIXED)
        assertThat(decoded.usesFittedHeight).isFalse()
    }

    test("a blob written before the card had a sizing mode reads as fitted") {
        // The same missing-field rule as the size itself, and the spot where the behaviour change is
        // actually pinned: introducing the mode did not invalidate every stored setting on every
        // machine that had one, and the default it falls back to is the fitted one. An existing reader's
        // cards therefore wrap their passage from the first launch after the update, and the height they
        // had chosen is kept — asserted here — as the fixed fallback behind the mode row rather than
        // being discarded.
        val beforeSizing =
            json.decodeFromString<NotificationPresentationSettings>(
                """{"durationSeconds":12,"cardWidthDp":400,"cardHeightDp":200}""",
            )

        assertThat(beforeSizing.cardSizing).isEqualTo(OverlayCardSizing.FIT)
        assertThat(beforeSizing.usesFittedHeight).isTrue()
        assertThat(beforeSizing.cardHeightDp).isEqualTo(200)
    }

    test("every sizing mode has a name the store can hold") {
        assertThat(OverlayCardSizing.entries.map { it.name }).containsExactly("FIT", "FIXED")
    }

    test("the default card stays for its own reading time, and a fixed stay survives a round trip") {
        // The same contract the sizing mode holds, settled the same way: reading time is the default
        // the model is built around, and a reader who pinned a number gets that number back after a
        // restart rather than having their choice silently re-fitted.
        val default = NotificationPresentationSettings()

        assertThat(default.durationMode).isEqualTo(OverlayDurationMode.READING)
        assertThat(default.usesReadingTime).isTrue()

        val fixed = NotificationPresentationSettings(durationMode = OverlayDurationMode.FIXED)
        val decoded = json.decodeFromString<NotificationPresentationSettings>(json.encodeToString(fixed))

        assertThat(decoded.durationMode).isEqualTo(OverlayDurationMode.FIXED)
        assertThat(decoded.usesReadingTime).isFalse()
    }

    test("a blob written before the card had a duration mode reads as the reading time") {
        // The missing-field rule again: the reading model did not invalidate a single stored setting
        // on upgrade, and the seconds a reader had set are still there behind the new mode, exactly as
        // the chosen height survives under fit sizing.
        val beforeModes =
            json.decodeFromString<NotificationPresentationSettings>(
                """{"durationSeconds":12,"cardSizing":"FIXED"}""",
            )

        assertThat(beforeModes.durationMode).isEqualTo(OverlayDurationMode.READING)
        assertThat(beforeModes.durationSeconds).isEqualTo(12)
    }

    test("every duration mode has a name the store can hold") {
        assertThat(OverlayDurationMode.entries.map { it.name }).containsExactly("READING", "FIXED")
    }

    test("a card's reading time is its own words, at the store's bounds") {
        // Pinned as raw millis so that retuning the model is a deliberate, visible change to this test,
        // and to the two people-pieces it stands on: the moment-to-see base and the words a minute.
        val settings = NotificationPresentationSettings()
        val notice = { words: String ->
            NotificationNotice(title = "t", message = words, action = NotificationAction.OpenApp)
        }

        // A blank body buys only the moment it takes to notice the card at all, but never less than
        // the store's floor — the shortest stay is a stay, not a flash.
        assertThat(settings.durationFor(notice(" ")))
            .isEqualTo(3_000)
        // Twelve words at the pinned rate: 2,500 to see it + 12 × 300 to read it.
        assertThat(settings.durationFor(notice("one two three four five six seven eight nine ten eleven twelve")))
            .isEqualTo(6_100)
        // A passage longer than a reader needs is capped by the same ceiling the fixed number has, so
        // neither mode can produce a duration the store would have refused.
        val floorFull = notice(List(600) { "word" }.joinToString(" "))

        assertThat(settings.durationFor(floorFull)).isEqualTo(120_000)
    }

    test("a card's time to read is the body the card shows, not the strings it carries") {
        // The estimate must follow what the reader actually reads. A passage card reads its passage,
        // a prayer card's sentence-like text is its prayer label, and a plain notice's body is its
        // message — its title is a caption, and charging the read time for both would time a card for
        // a page it does not have.
        val settings = NotificationPresentationSettings()

        assertThat(
            settings.durationFor(
                NotificationNotice(
                    title = "Did you know",
                    message = "one two three",
                    action = NotificationAction.OpenApp,
                    content = NotificationContent.Remembrance(text = "four five", position = 1, total = 2),
                ),
            ),
        ).isEqualTo(3_100)
        assertThat(
            settings.durationFor(
                NotificationNotice(
                    title = "t",
                    message = "one two three",
                    action = NotificationAction.OpenApp,
                    content = NotificationContent.Prayer(prayer = "four", time = "05:12", city = "Amman"),
                ),
            ),
        ).isEqualTo(3_000)
    }

    test("in fixed time, a card's duration is the chosen seconds whatever it shows") {
        // The one promise the fixed mode makes: the number is the number, independent of the content —
        // which is why a mode switch never has to touch the stored seconds at all.
        val fixed = NotificationPresentationSettings(durationMode = OverlayDurationMode.FIXED, durationSeconds = 20)

        assertThat(
            fixed.durationFor(
                NotificationNotice(
                    title = "t",
                    message = "one two three",
                    action = NotificationAction.OpenApp,
                ),
            ),
        ).isEqualTo(20_000)
    }

    test("an unset reminder language means the app's own language, not a language nobody chose") {
        // Blank is the default and the meaning of "follow the app": the runtime reads a null here and
        // falls back to the interface language, which is the behaviour every existing reader already has.
        assertThat(NotificationPresentationSettings().notificationLanguageTag).isEmpty()
        assertThat(NotificationPresentationSettings().notificationLocale).isNull()

        val beforeLanguages =
            json.decodeFromString<NotificationPresentationSettings>("""{"durationSeconds":12}""")

        assertThat(beforeLanguages.notificationLanguageTag).isEmpty()
        assertThat(beforeLanguages.notificationLocale).isNull()
    }

    test("a stored reminder language reads back as that language") {
        assertThat(NotificationPresentationSettings(notificationLanguageTag = "ar").notificationLocale)
            .isEqualTo(ARABIC)
        assertThat(NotificationPresentationSettings(notificationLanguageTag = "en").notificationLocale)
            .isEqualTo(Locale.ENGLISH)
    }

    test("a reminder-language tag this version cannot read falls back to the app rather than throwing") {
        // Unlike the numeric bounds, a bad tag must not fail the whole blob: it is normalised to blank by
        // `validated()` — one unreadable field is not worth resetting every other setting over — and the
        // repository's decode of a corrupt blob is otherwise the only thing standing between a bad tag and
        // a crash.
        val stored =
            json.decodeFromString<NotificationPresentationSettings>("""{"notificationLanguageTag":"und"}""")

        assertThat(stored.notificationLocale).isNull()
        assertThat(stored.validated().notificationLanguageTag).isEmpty()
        assertThat(stored.validated().notificationLocale).isNull()
    }
}
