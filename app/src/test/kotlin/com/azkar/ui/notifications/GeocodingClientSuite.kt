package com.azkar.ui.notifications

import de.infix.testBalloon.framework.core.testSuite
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat

/**
 * That a provider's bytes become the place the picker shows.
 *
 * The one shape that matters most is the one with no shape at all: a search that matched nothing
 * comes back as a perfectly successful response with the `results` key simply absent, which a
 * `results: List<…>` field with no default would turn into a parse failure and a "could not search"
 * message. Each of these tests is one of those shapes, read back through the same `HttpClient` stack
 * the app builds, so what is asserted is what the screen will actually get.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val GeocodingClientSuite by testSuite("The geocoding client") {
    test("a search names the place it found, with its zone") {
        runTest {
            val client =
                geocodingClient(
                    """
                    {
                      "results": [
                        {
                          "id": 2988507,
                          "name": "Paris",
                          "latitude": 48.85341,
                          "longitude": 2.3488,
                          "timezone": "Europe/Paris",
                          "country": "France",
                          "admin1": "Ile-de-France"
                        }
                      ]
                    }
                    """.trimIndent(),
                )

            val places = client.searchPlaces("Paris", "en")

            assertThat(places).hasSize(1)
            val place = places.single()
            assertThat(place.name).isEqualTo("Paris")
            assertThat(place.region).isEqualTo("Ile-de-France")
            assertThat(place.country).isEqualTo("France")
            assertThat(place.latitude).isEqualTo(48.85341)
            assertThat(place.longitude).isEqualTo(2.3488)
            assertThat(place.zoneId).isEqualTo("Europe/Paris")
            assertThat(place.label).isEqualTo("Paris, Ile-de-France, France")
        }
    }

    test("a search with no matches answers with nothing rather than failing") {
        runTest {
            // What Open-Meteo actually sends for a term it cannot place: a successful response
            // with the `results` key left out entirely, not an empty array.
            val client = geocodingClient("""{"generationtime_ms": 0.1}""")

            val places = client.searchPlaces("qwertyuiop", "en")

            assertThat(places).isEmpty()
        }
    }

    test("a search that the provider rejects is a failure, not an empty list") {
        runTest {
            val client = geocodingClient("""{"error": true}""", status = HttpStatusCode.InternalServerError)

            val result = runCatching { client.searchPlaces("Paris", "en") }

            // The distinction is the message: an empty list means "no such place", a failure means
            // "we could not ask", and the picker says different things for the two.
            assertThat(result.isFailure).isTrue()
        }
    }

    test("a blank search never reaches the provider") {
        runTest {
            val engine = emptyResultsEngine()
            val client = GeocodingClient(testHttpClient(engine))

            assertThat(client.searchPlaces("   ", "en")).isEmpty()
            assertThat(engine.requestHistory).isEmpty()
        }
    }

    test("the search asks the provider in the interface language") {
        runTest {
            val engine = emptyResultsEngine()
            val client = GeocodingClient(testHttpClient(engine))

            client.searchPlaces("القاهرة", "ar")

            val request = engine.requestHistory.single()
            assertThat(request.url.parameters["language"]).isEqualTo("ar")
            assertThat(request.url.parameters["name"]).isEqualTo("القاهرة")
        }
    }
}

/** An [io.ktor.client.HttpClient] over a [MockEngine] that always answers with [body] and [status]. */
private fun geocodingClient(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) = GeocodingClient(testHttpClient(jsonEngine(body, status)))

private fun emptyResultsEngine(): MockEngine = jsonEngine("""{"results": []}""")
