package com.azkar.i18n

import androidx.datastore.preferences.core.stringPreferencesKey
import com.azkar.settings.SettingsStore
import com.azkar.settings.withInMemorySettings
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * The stored language, and the rules that govern a stored value the app cannot honour.
 *
 * The theme store has the suite that caught a choice that never reached disk, and the language was
 * one store away from the same bug with none of the coverage: nothing here used to assert that
 * choosing Arabic survives the window closing, so a write that quietly went nowhere would have shipped
 * as "the language setting doesn't stick" with a green build.
 *
 * The round trip is asserted through a second reader over the same settings rather than through a
 * copy of a file. The question being asked is "does the choice come back?", and the in-memory store
 * answers it in full; whether a value survives a restart on disk is asked once, in
 * [com.azkar.settings.SettingsStoreSuite].
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val LocaleStoreSuite by testSuite("The stored language") {
    test("a fresh install falls back") {
        withInMemorySettings { _, store, _ ->
            // The fallback is stated rather than inherited, because production derives it from the
            // machine's default locale and a test asserting on the machine it runs on is a test that
            // passes on the author's laptop and fails on CI.
            val localeStore = LocaleStore(store, fallback = ARABIC)

            assertThat(localeStore.read()).isEqualTo(ARABIC)
        }
    }

    test("a stored language is what the next run opens with") {
        withInMemorySettings { scope, store, dataStore ->
            val localeStore = LocaleStore(store)
            localeStore.persistAndAwait(ARABIC)

            val nextLaunch = LocaleStore(SettingsStore(dataStore, scope))

            assertThat(nextLaunch.read()).isEqualTo(ARABIC)
        }
    }

    test("a stored language round-trips a region the app never offers") {
        withInMemorySettings { scope, store, dataStore ->
            val localeStore = LocaleStore(store)
            // `en-GB` is not in [supportedLocales], so it can only reach the settings by being
            // written directly. It is still a legal tag, and refusing it would throw away a stored
            // value a hand-edited file or an older build could legitimately have left behind.
            val british = Locale.forLanguageTag("en-GB")
            localeStore.persistAndAwait(british)

            val nextLaunch = LocaleStore(SettingsStore(dataStore, scope))

            assertThat(nextLaunch.read()).isEqualTo(british)
        }
    }

    test("a malformed tag falls back rather than launching in a language with no translations") {
        withInMemorySettings { _, store, _ ->
            val localeStore = LocaleStore(store, fallback = ENGLISH)
            // `Locale.forLanguageTag` answers `und` for anything it cannot parse. Accepting that
            // would render the app undetermined, which is the same as no translations at all.
            localeStore.putStoredTag("not a tag")

            assertThat(localeStore.read()).isEqualTo(ENGLISH)
        }
    }

    test("an empty tag falls back") {
        withInMemorySettings { _, store, _ ->
            val localeStore = LocaleStore(store, fallback = ENGLISH)
            // An empty string round-trips through `Locale.forLanguageTag` to ROOT, which has no
            // translations, so a blank write has to be treated as "nothing stored".
            localeStore.putStoredTag("")

            assertThat(localeStore.read()).isEqualTo(ENGLISH)
        }
    }

    test("readAndActivate is the only thing that moves the JVM default") {
        withInMemorySettings { _, store, _ ->
            val original = Locale.getDefault()
            try {
                val localeStore = LocaleStore(store)
                localeStore.persistAndAwait(ARABIC)

                // `read` answers the question and nothing more. This is the property that lets
                // `Main` decide when to touch a process-wide value, and a store that activated as a
                // side effect of being asked would make that decision impossible to place.
                assertThat(localeStore.read()).isEqualTo(ARABIC)
                assertThat(Locale.getDefault()).isEqualTo(original)

                localeStore.readAndActivate()
                assertThat(Locale.getDefault()).isEqualTo(ARABIC)
            } finally {
                // The JVM default is process-wide, so a test that moved it and did not put it back
                // would decide the language of every test that runs after it.
                Locale.setDefault(original)
            }
        }
    }

    test("the stored key is the name the file format already has") {
        // Written out rather than read back off [LocaleStore.key]: renaming the constant would
        // silently reset everyone's saved language, and this is where that should be noticed.
        assertThat(LocaleStore.key).isEqualTo(stringPreferencesKey("locale"))
    }

    test("localeOrNull rejects the shapes that mean no language, and nothing else") {
        // The parser is the part with real logic, and it is pure, so it is asserted directly rather
        // than through a store: a round trip cannot distinguish "rejected" from "parsed to something
        // that then fell back to the same place".
        assertThat("".localeOrNull()).isNull()
        assertThat("   ".localeOrNull()).isNull()
        assertThat(null.localeOrNull()).isNull()
        assertThat("not a tag".localeOrNull()).isNull()
        assertThat("en".localeOrNull()).isEqualTo(ENGLISH)
        assertThat("ar".localeOrNull()).isEqualTo(ARABIC)
        assertThat("en-GB".localeOrNull()).isEqualTo(Locale.forLanguageTag("en-GB"))
    }
}
