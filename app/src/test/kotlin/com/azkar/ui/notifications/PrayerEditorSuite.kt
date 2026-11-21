package com.azkar.ui.notifications

import com.azkar.settings.FakePreferencesDataStore
import com.azkar.settings.SettingsStore
import de.infix.testBalloon.framework.core.testSuite
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import kotlin.time.Duration.Companion.milliseconds

/**
 * That the picker's two ways in — search, and a picked suggestion — land in the same place, and that
 * neither of them is a way to lose the calculation method.
 *
 * The state is driven directly rather than through the composable: the buttons and the search box
 * are one call each into this class, and the interesting behaviour is what the calls do with a
 * provider's answer, not which pixels moved.
 *
 * These run on real time rather than the test scheduler. The editor's writes and its searches all
 * leave through a launched coroutine, and a search's answer comes back off the HTTP client's own
 * dispatcher — neither of which a virtual clock can run out. So the tests wait for the state to
 * settle with [awaitUntil] instead, which is both honest about the concurrency and short enough to
 * stay well under the search's own debounce.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val PrayerEditorSuite by testSuite("The prayer location editor") {
    test("typing searches only once the user stops") {
        withEditor(geocoding = GeocodingClient(testHttpClient(jsonEngine(parisSearchResults())))) { fixture ->
            fixture.state.prayer.updateSearchQuery("Paris")

            // The debounce means a keystroke is not yet a request.
            assertThat(fixture.state.prayer.isSearching).isFalse()
            assertThat(fixture.state.prayer.suggestions).isEmpty()

            awaitUntil {
                fixture.state.prayer.suggestions
                    .isNotEmpty()
            }

            assertThat(
                fixture.state.prayer.suggestions
                    .map { it.name },
            ).containsExactly("Paris")
            assertThat(fixture.state.prayer.isSearching).isFalse()
        }
    }

    test("a search that finds nothing says so instead of showing an empty list") {
        withEditor(
            geocoding = GeocodingClient(testHttpClient(jsonEngine("""{"generationtime_ms": 0.1}"""))),
        ) { fixture ->
            fixture.state.prayer.updateSearchQuery("qwertyuiop")

            awaitUntil { fixture.state.prayer.notice != null }

            assertThat(fixture.state.prayer.suggestions).isEmpty()
            assertThat(fixture.state.prayer.notice).isEqualTo(LocationNotice.NoResults)
        }
    }

    test("a search that could not be asked reports the failure") {
        val engine = jsonEngine("""{"error": true}""", HttpStatusCode.ServiceUnavailable)
        withEditor(geocoding = GeocodingClient(testHttpClient(engine))) { fixture ->
            fixture.state.prayer.updateSearchQuery("Paris")

            awaitUntil { fixture.state.prayer.notice != null }

            assertThat(fixture.state.prayer.notice).isInstanceOf(LocationNotice.SearchFailed::class.java)
        }
    }

    test("clearing the field drops the suggestions without a further search") {
        val engine = jsonEngine(parisSearchResults())
        withEditor(geocoding = GeocodingClient(testHttpClient(engine))) { fixture ->
            fixture.state.prayer.updateSearchQuery("Paris")
            awaitUntil {
                fixture.state.prayer.suggestions
                    .isNotEmpty()
            }

            fixture.state.prayer.updateSearchQuery("")

            assertThat(fixture.state.prayer.suggestions).isEmpty()
            assertThat(engine.requestHistory).hasSize(1)
        }
    }

    test("picking a suggestion fills every field and saves it") {
        withEditor { fixture ->
            fixture.state.prayer.selectPlace(paris())

            awaitUntil { fixture.state.prayer.notice is LocationNotice.PlaceSelected }

            val saved = fixture.preferences.prayerLocation.value
            assertThat(saved.city).isEqualTo("Paris, Ile-de-France, France")
            assertThat(saved.latitude).isEqualTo(48.85)
            assertThat(saved.longitude).isEqualTo(2.35)
            assertThat(saved.zoneId).isEqualTo("Europe/Paris")
            // A chosen place is no longer whatever the last auto-detect found.
            assertThat(saved.autoDetect).isFalse()
            assertThat(fixture.state.locationDraft.city).isEqualTo("Paris, Ile-de-France, France")
            assertThat(
                fixture.state.prayer.notice,
            ).isEqualTo(LocationNotice.PlaceSelected("Paris, Ile-de-France, France"))
        }
    }

    test("picking a suggestion leaves the calculation method alone") {
        withEditor { fixture ->
            fixture.state.prayer.setMethod(PrayerCalculationMethod.KARACHI)
            awaitUntil { fixture.preferences.prayerLocation.value.method == PrayerCalculationMethod.KARACHI }

            fixture.state.prayer.selectPlace(paris())
            awaitUntil { fixture.state.prayer.notice is LocationNotice.PlaceSelected }

            assertThat(fixture.preferences.prayerLocation.value.method).isEqualTo(PrayerCalculationMethod.KARACHI)
        }
    }

    test("a suggestion with no zone of its own keeps the stored zone") {
        withEditor { fixture ->
            val storedZone = fixture.preferences.prayerLocation.value.zoneId

            fixture.state.prayer.selectPlace(paris().copy(zoneId = null))
            awaitUntil { fixture.state.prayer.notice is LocationNotice.PlaceSelected }

            // The suggestion carried no zone — a place the provider could name but not place in
            // time — and dropping the stored one would leave the calculator with nothing.
            assertThat(fixture.preferences.prayerLocation.value.zoneId).isEqualTo(storedZone)
        }
    }
}

private class EditorFixture(
    val state: NotificationSettingsState,
    val preferences: NotificationPreferencesRepository,
)

private fun withEditor(
    geocoding: GeocodingClient? = null,
    block: suspend (EditorFixture) -> Unit,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        val preferences = NotificationPreferencesRepository(SettingsStore(FakePreferencesDataStore(), scope))
        val state =
            NotificationSettingsState(
                scope = scope,
                preferences = preferences,
                geocodingClient = geocoding,
            )
        runBlocking { block(EditorFixture(state, preferences)) }
    } finally {
        scope.cancel()
    }
}

/**
 * Waits for a launched write to land.
 *
 * Polling rather than a single sleep because the work being waited on crosses a real dispatcher and
 * has no completion the test can join; a bounded loop that stops as soon as the state settles keeps
 * the tests fast without making them race.
 */
private suspend fun awaitUntil(
    timeoutMillis: Long = 5_000,
    condition: () -> Boolean,
) {
    withTimeout(timeoutMillis.milliseconds) {
        while (!condition()) delay(10.milliseconds)
    }
}

private fun paris() =
    PrayerPlace(
        name = "Paris",
        region = "Ile-de-France",
        country = "France",
        latitude = 48.85,
        longitude = 2.35,
        zoneId = "Europe/Paris",
    )

private fun parisSearchResults() =
    """
    {
      "results": [
        { "id": 1, "name": "Paris", "latitude": 48.85, "longitude": 2.35, "timezone": "Europe/Paris", "country": "France", "admin1": "Ile-de-France" }
      ]
    }
    """.trimIndent()
