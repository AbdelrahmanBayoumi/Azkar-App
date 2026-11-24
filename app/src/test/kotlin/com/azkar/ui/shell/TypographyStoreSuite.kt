package com.azkar.ui.shell

import com.azkar.settings.SettingsStore
import com.azkar.settings.withInMemorySettings
import com.azkar.ui.theme.AppTypography
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * That a reader's face and size are still there on the next launch, and that a value this build
 * cannot honour is turned into one it can rather than thrown at the theme.
 *
 * The face half is the one with a real failure mode and it is not a crash: a settings file that
 * travels to a machine without the chosen face installed would otherwise leave the theme asking for
 * a family that does not resolve. [TypographyStore.read] answering with a usable value is what keeps
 * that a cosmetic problem, and the only way to test it is to write a name this machine does not have
 * — which is what `putStoredFamily` exists for.
 *
 * The round trip goes through a second reader over the same settings rather than a copy of the
 * file. The question being asked is "does the choice come back?", and the in-memory store answers it
 * in full; whether a value survives a restart on disk is asked once, in
 * [com.azkar.settings.SettingsStoreSuite].
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val TypographyStoreSuite by testSuite("The reader's typography") {
    test("a fresh install reads as the bundled pairing at full size") {
        withInMemorySettings { _, store, _ ->
            val typography = TypographyStore(store)

            assertThat(typography.read()).isEqualTo(AppTypography())
            assertThat(AppTypography().isAzkarFont).isTrue()
            assertThat(AppTypography().scale).isEqualTo(1f)
        }
    }

    test("a chosen face and size are what the next launch reads") {
        withInMemorySettings { scope, store, dataStore ->
            val chosen = AppTypography(family = "Georgia", scalePercent = 130)
            TypographyStore(store).persistAndAwait(chosen)

            val nextLaunch = TypographyStore(SettingsStore(dataStore, scope))

            assertThat(nextLaunch.read()).isEqualTo(chosen)
        }
    }

    test("a face this machine does not have still reads as a usable choice") {
        withInMemorySettings { _, store, _ ->
            val typography = TypographyStore(store)
            typography.persistAndAwait(AppTypography(scalePercent = 110))
            typography.putStoredFamily("A Face Only The Author Had")

            val read = typography.read()

            // The size, which this build does understand, survives; the face is handed back as the
            // name that was stored, because deciding what to show for it is the settings screen's job
            // and it already does — the row turns into a "not installed" notice.
            assertThat(read.scalePercent).isEqualTo(110)
            assertThat(read.family).isEqualTo("A Face Only The Author Had")
        }
    }

    test("a stored size outside the slider's range is clamped rather than honoured") {
        withInMemorySettings { _, store, _ ->
            val typography = TypographyStore(store)
            typography.persistAndAwait(AppTypography(scalePercent = 4_000))

            assertThat(typography.read().scalePercent).isEqualTo(AppTypography.MAX_SCALE_PERCENT)
        }
    }

    test("a stored size below the slider's range is clamped the other way") {
        withInMemorySettings { _, store, _ ->
            val typography = TypographyStore(store)
            typography.persistAndAwait(AppTypography(scalePercent = -50))

            assertThat(typography.read().scalePercent).isEqualTo(AppTypography.MIN_SCALE_PERCENT)
        }
    }

    test("writing the bundled pairing back clears the face rather than storing a name for it") {
        withInMemorySettings { _, store, _ ->
            val typography = TypographyStore(store)
            typography.persistAndAwait(AppTypography(family = "Georgia", scalePercent = 130))
            typography.persistAndAwait(AppTypography())

            assertThat(typography.read()).isEqualTo(AppTypography())
        }
    }
}
