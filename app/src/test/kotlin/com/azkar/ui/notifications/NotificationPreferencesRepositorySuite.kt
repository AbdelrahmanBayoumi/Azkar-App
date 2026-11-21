package com.azkar.ui.notifications

import com.azkar.settings.SettingsStore
import com.azkar.settings.withInMemorySettings
import com.azkar.ui.home.PrayerKey
import com.azkar.ui.notifications.presentation.NotificationPresentationSettings
import com.azkar.ui.notifications.presentation.OverlayPlacement
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import java.io.IOException

/**
 * That every value the manager screen writes is a value the next launch reads.
 *
 * The screens do not write files; they call `update…` on this repository, and the repository
 * serialises. Nothing between the switch in the UI and the stored bytes is covered by a screen test
 * or a model test, and the gap had a bug in it: the presentation settings carried a `private
 * companion object` for their bounds, and the generated serializer sits outside the class, so the
 * *first* time anybody moved a switch the write failed at runtime with an `IllegalAccessError` —
 * after a clean compile and a green suite. These tests are that gap, closed: each one writes through
 * the real repository, then opens a second store over the same settings, which is what the next run
 * of the app would do.
 *
 * The settings are in memory because the bug was never about the disk. The serialiser is what
 * failed, and it is what runs here: the repository encodes the value to JSON and the repository
 * decodes that JSON again, in memory, exactly as it does when the bytes eventually land in a file.
 * Whether they land is asked once, in [com.azkar.settings.SettingsStoreSuite].
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val NotificationPreferencesRepositorySuite by testSuite("The notification preferences") {
    test("audio defaults preserve changes made while installation is suspended") {
        withInMemorySettings { _, store, _ ->
            val started = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            val preferences =
                NotificationPreferencesRepository(store) {
                    started.complete(Unit)
                    finish.await()
                    "/default/notification.mp3" to "/default/prayer.mp3"
                }
            val loading = launch { preferences.refresh() }
            started.await()
            preferences.updateAudio { it.copy(notificationFile = "/custom.mp3") }
            preferences.setPrayerSoundEnabled(PrayerKey.FAJR, false)
            finish.complete(Unit)
            loading.join()
            assertThat(preferences.audio.value.notificationFile).isEqualTo("/custom.mp3")
            assertThat(preferences.audio.value.prayerFile).isEqualTo("/default/prayer.mp3")
            assertThat(preferences.audio.value.prayerSoundsEnabled[PrayerKey.FAJR.id]).isFalse()
        }
    }

    test("audio installation failure does not stop observing prayer settings") {
        withInMemorySettings { scope, store, _ ->
            val preferences = NotificationPreferencesRepository(store) { throw IOException("unavailable") }
            preferences.observe(scope)
            preferences.setPrayerEnabled(PrayerKey.ASR, false)
            assertThat(preferences.prayerFlags.value[PrayerKey.ASR]).isFalse()
        }
    }
    test("a fresh install has the overlay it is designed with") {
        withInMemorySettings { _, store, _ ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.refresh()

            val presentation = preferences.presentation.value
            assertThat(presentation.durationSeconds).isEqualTo(8)
            assertThat(presentation.placement).isEqualTo(OverlayPlacement.TOP_END)
            assertThat(presentation.maxVisible).isEqualTo(3)
        }
    }

    test("a presentation change is what the next launch reads") {
        withInMemorySettings { scope, store, dataStore ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.refresh()
            val chosen =
                NotificationPresentationSettings(
                    durationSeconds = 20,
                    placement = OverlayPlacement.BOTTOM_START,
                    maxVisible = 4,
                )
            preferences.updatePresentation { chosen }

            val nextLaunch = NotificationPreferencesRepository(SettingsStore(dataStore, scope))
            nextLaunch.refresh()

            assertThat(nextLaunch.presentation.value).isEqualTo(chosen)
            assertThat(preferences.presentation.value).isEqualTo(chosen)
        }
    }

    test("a presentation change is kept beside the queue rather than replacing it") {
        withInMemorySettings { scope, store, dataStore ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.refresh()
            preferences.updateQueue { it.copy(enabled = true, mode = QueueMode.RANDOM, intervalMinutes = 45) }
            preferences.updatePresentation { it.copy(durationSeconds = 15) }

            val nextLaunch = NotificationPreferencesRepository(SettingsStore(dataStore, scope))
            nextLaunch.refresh()

            assertThat(nextLaunch.queue.value.enabled).isTrue()
            assertThat(nextLaunch.queue.value.mode).isEqualTo(QueueMode.RANDOM)
            assertThat(nextLaunch.queue.value.intervalMinutes).isEqualTo(45)
            assertThat(nextLaunch.presentation.value.durationSeconds).isEqualTo(15)
        }
    }

    test("a queue change is what the next launch reads") {
        withInMemorySettings { scope, store, dataStore ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.refresh()
            preferences.updateQueue { it.copy(collectionIds = listOf(4L, 9L), intervalMinutes = 30) }

            val nextLaunch = NotificationPreferencesRepository(SettingsStore(dataStore, scope))
            nextLaunch.refresh()

            assertThat(nextLaunch.queue.value.collectionIds).containsExactly(4L, 9L)
            assertThat(nextLaunch.queue.value.intervalMinutes).isEqualTo(30)
        }
    }

    test("a prayer location is what the next launch calculates from") {
        withInMemorySettings { scope, store, dataStore ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.refresh()
            val chosen =
                PrayerLocationSettings(
                    latitude = 41.01,
                    longitude = 28.98,
                    city = "Istanbul, Türkiye",
                    zoneId = "Europe/Istanbul",
                    method = PrayerCalculationMethod.KARACHI,
                    autoDetect = false,
                )
            preferences.savePrayerLocation(chosen)

            val nextLaunch = NotificationPreferencesRepository(SettingsStore(dataStore, scope))
            nextLaunch.refresh()

            assertThat(nextLaunch.prayerLocation.value).isEqualTo(chosen)
        }
    }

    test("the five prayer switches are still the keys the home screen writes") {
        withInMemorySettings { scope, store, dataStore ->
            val preferences = NotificationPreferencesRepository(store)
            preferences.refresh()
            preferences.setPrayerEnabled(PrayerKey.ASR, false)
            preferences.setPrayerEnabled(PrayerKey.ISHA, false)

            val nextLaunch = NotificationPreferencesRepository(SettingsStore(dataStore, scope))
            nextLaunch.refresh()

            assertThat(nextLaunch.prayerFlags.value[PrayerKey.ASR]).isFalse()
            assertThat(nextLaunch.prayerFlags.value[PrayerKey.ISHA]).isFalse()
            assertThat(nextLaunch.prayerFlags.value[PrayerKey.FAJR]).isTrue()
        }
    }
}
