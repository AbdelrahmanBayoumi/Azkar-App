package com.azkar.ui.theme

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * That the face picker is describing this machine rather than the one the app was written on.
 *
 * The picker exposes the complete family catalogue derived at runtime from AWT. These assertions are
 * about the relationship between the two lists, not about any particular family being present on the
 * machine that happens to be running the test.
 *
 * `SCALE_STEPS` is asserted separately because the slider is built from it: a step list that no
 * longer spans the clamped range produces a thumb that cannot reach either end, which is not a crash
 * and so nothing else would notice.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val TypographySuite by testSuite("The reader's typeface and size") {
    test("the installed families are sorted and listed once each") {
        val installed = SystemFontCatalogue.installed

        assertThat(installed).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER)
        assertThat(installed.map { it.lowercase() }).doesNotHaveDuplicates()
    }

    test("the picker offers every installed family in the same sorted order") {
        assertThat(SystemFontCatalogue.choices).isEqualTo(SystemFontCatalogue.installed)
    }

    test("a face this machine has resolves, and the same name resolves to the same object") {
        val installed = SystemFontCatalogue.installed.firstOrNull()
        if (installed == null) {
            assertThat(SystemFontCatalogue.installed).isEmpty()
        } else {
            val first = SystemFontCatalogue.family(installed)
            val second = SystemFontCatalogue.family(installed)

            assertThat(first).isNotNull()
            // Cached: building a family registers eighteen faces with the text backend, so a settings
            // screen that rebuilt one per frame would stutter on every frame.
            assertThat(first).isSameAs(second)
        }
    }

    test("a name this machine does not have resolves to nothing rather than to a substitute") {
        val invented = "A Face Only The Author Had"

        assertThat(SystemFontCatalogue.knows(invented)).isFalse()
        assertThat(SystemFontCatalogue.family(invented)).isNull()
    }

    test("the bundled pairing is recognised as not-a-system-face") {
        assertThat(SystemFontCatalogue.knows(AppTypography.AZKAR_FONT)).isFalse()
        assertThat(SystemFontCatalogue.family(AppTypography.AZKAR_FONT)).isNull()
    }

    test("a face found on disk is recognised whatever case it was stored in") {
        val installed = SystemFontCatalogue.installed.firstOrNull()
        if (installed == null) {
            assertThat(SystemFontCatalogue.installed).isEmpty()
        } else {
            assertThat(SystemFontCatalogue.knows(installed.uppercase())).isTrue()
        }
    }

    test("the size steps span the clamped range in the increments the slider promises") {
        val steps = AppTypography.SCALE_STEPS

        assertThat(steps.first()).isEqualTo(AppTypography.MIN_SCALE_PERCENT)
        assertThat(steps.last()).isEqualTo(AppTypography.MAX_SCALE_PERCENT)
        steps.zipWithNext().forEach { (earlier, later) ->
            assertThat(later - earlier).isEqualTo(25)
        }
    }
}

/**
 * That a stored size is turned into one the theme can draw rather than passed straight through.
 *
 * The clamp is not validation: a scale outside the range is not refused, it is rounded to the nearest
 * end. A settings file copied to a machine with a different display configuration has to arrive
 * usable, and a card the reader cannot read is worse than a card slightly smaller than they asked
 * for.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val AppTypographySuite by testSuite("The typography value") {
    test("the default is the bundled pairing at full size") {
        assertThat(AppTypography()).isEqualTo(AppTypography(family = "", scalePercent = 100))
        assertThat(AppTypography().isAzkarFont).isTrue()
        assertThat(AppTypography().scale).isEqualTo(1f)
    }

    test("a chosen family is not the bundled pairing") {
        assertThat(AppTypography(family = "Georgia").isAzkarFont).isFalse()
    }

    test("the scale is the percentage as a multiplier") {
        assertThat(AppTypography(scalePercent = 125).scale).isEqualTo(1.25f)
        assertThat(AppTypography(scalePercent = 85).scale).isEqualTo(0.85f)
    }

    test("a size inside the range is left exactly as stored") {
        val stored = AppTypography(family = "Georgia", scalePercent = 130)

        assertThat(stored.coerced()).isEqualTo(stored)
    }

    test("a size past the top is pulled back to the top") {
        assertThat(AppTypography(scalePercent = 900).coerced().scalePercent)
            .isEqualTo(AppTypography.MAX_SCALE_PERCENT)
    }

    test("a size past the bottom is pulled up to the bottom") {
        assertThat(AppTypography(scalePercent = -10).coerced().scalePercent)
            .isEqualTo(AppTypography.MIN_SCALE_PERCENT)
    }

    test("clamping the size leaves the face alone") {
        assertThat(AppTypography(family = "Georgia", scalePercent = 900).coerced().family)
            .isEqualTo("Georgia")
    }

    test("every step the slider offers survives the clamp unchanged") {
        AppTypography.SCALE_STEPS.forEach { step ->
            assertThat(AppTypography(scalePercent = step).coerced().scalePercent).isEqualTo(step)
        }
    }
}
