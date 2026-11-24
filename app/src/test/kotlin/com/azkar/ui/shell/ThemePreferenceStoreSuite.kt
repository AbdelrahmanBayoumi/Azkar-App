package com.azkar.ui.shell

import androidx.datastore.preferences.core.stringPreferencesKey
import com.azkar.settings.SettingsStore
import com.azkar.settings.withInMemorySettings
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * The stored colour preference, and the rules that govern a stored value nobody can honour.
 *
 * This is the fix for a toggle that changed the palette and forgot: `selectTheme` moved the state
 * and never wrote it, and nothing read the key back, so the next launch always opened following the
 * system.
 *
 * The round trip is asserted through a second reader over the same settings rather than through a
 * copy of a file. The question being asked here is "does the choice come back?", and the in-memory
 * store answers it in full: it holds the same bytes a file would, and the store under test writes
 * them through the same code. The question that *is* about the disk — does a value survive a
 * restart, and what happens when the file is corrupt — has its own home, in
 * [com.azkar.settings.SettingsStoreSuite], and is asked there.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val ThemePreferenceStoreSuite by testSuite("The stored colour preference") {
    test("a fresh install follows the system") {
        withInMemorySettings { _, store, _ ->
            val themeStore = ThemePreferenceStore(store)

            assertThat(themeStore.read()).isEqualTo(ThemePreference.SYSTEM)
        }
    }

    test("a stored preference is what the next run opens with") {
        withInMemorySettings { scope, store, dataStore ->
            val themeStore = ThemePreferenceStore(store)
            themeStore.persistAndAwait(ThemePreference.DARK)

            // The next run is a new store over the same settings. An in-memory store has no
            // one-instance-per-file rule, so there is nothing to copy first and nothing to race.
            val nextLaunch = ThemePreferenceStore(SettingsStore(dataStore, scope))

            assertThat(nextLaunch.read()).isEqualTo(ThemePreference.DARK)
        }
    }

    test("a name this build does not know falls back to following the system") {
        withInMemorySettings { _, store, _ ->
            val themeStore = ThemePreferenceStore(store)
            // Seeded under the production key rather than through [ThemePreferenceStore.persist],
            // because no preference this build has can produce a name it does not know.
            themeStore.putStoredName("SEPIA")

            // A light window beats no window: a hand-edited file or a renamed constant must not take
            // the launch down with it.
            assertThat(themeStore.read()).isEqualTo(ThemePreference.SYSTEM)
        }
    }

    test("the stored key is the name the file format already has") {
        // The pinning is the point of this one, and it is why the suite does not simply assert on
        // [ThemePreferenceStore.key]: renaming the constant would silently drop everyone's saved
        // theme, so the name itself has to be written out here and compared.
        assertThat(ThemePreferenceStore.key)
            .isEqualTo(stringPreferencesKey("theme_preference"))
    }
}
