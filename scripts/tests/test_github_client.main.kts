#!/usr/bin/env -S bash scripts/release/kotlin.sh
@file:DependsOn("io.ktor:ktor-client-mock-jvm:3.6.1")
@file:Import("../release/release-rules.main.kts", "../release/github-client.main.kts")

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
var passed = 0

suspend fun test(
    name: String,
    block: suspend () -> Unit,
) {
    try {
        block()
        passed++
        println("PASS: $name")
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        throw IllegalStateException("Failed: $name", error)
    }
}

runBlocking {
    test("paginated JSON releases preserve drafts and keep authentication on GitHub") {
        val pages = mutableListOf<String?>()
        val engine =
            MockEngine { request ->
                check(request.url.host == "api.github.com")
                check(request.url.encodedPath == "/repos/example/azkar/releases")
                check(request.url.parameters["per_page"] == "100")
                check(request.headers[HttpHeaders.Authorization] == "Bearer test-token")
                check(request.headers["X-GitHub-Api-Version"] == "2022-11-28")
                pages += request.url.parameters["page"]
                if (pages.size == 1) {
                    respond(
                        """[{"tag_name":"v1.2.3","prerelease":false,"draft":false,"published_at":"2026-10-28T00:00:00Z","extra":"ignored"}]""",
                        headers =
                            headersOf(
                                HttpHeaders.ContentType to listOf("application/json"),
                                HttpHeaders.Link to
                                    listOf(
                                        "<https://untrusted.invalid/?page=2>; rel=\"next\"; type=\"application/json\"",
                                    ),
                            ),
                    )
                } else {
                    respond(
                        """[{"tag_name":"v1.2.4-beta.1","prerelease":true,"draft":true,"published_at":null}]""",
                        headers = jsonHeaders,
                    )
                }
            }
        GitHubReleaseClient("example/azkar", "test-token", engine).use { client ->
            val releases = client.releases()
            check(releases.size == 2)
            check(releases[0].publishedAt == Instant.parse("2026-10-28T00:00:00Z"))
            check(releases[1].draft && releases[1].publishedAt == null)
            check(nextDevVersion(releases, emptyList()).appVersion == "1.2.4-beta.2")
        }
        check(pages == listOf("1", "2"))
    }

    test("commit comparisons decode JSON status") {
        val head = "a".repeat(40)
        val engine =
            MockEngine { request ->
                check(request.url.encodedPath == "/repos/example/azkar/compare/v1.2.4-beta.1...$head")
                respond("""{"status":"ahead","ahead_by":3}""", headers = jsonHeaders)
            }
        GitHubReleaseClient("example/azkar", engine = engine).use { client ->
            check(client.comparison("v1.2.4-beta.1", head) == "ahead")
        }
    }

    test("HTTP errors and redirects fail without following another host") {
        listOf(
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            HttpStatusCode.InternalServerError,
            HttpStatusCode.Found,
        ).forEach { status ->
            var requests = 0
            val engine =
                MockEngine { request ->
                    requests++
                    check(request.url.host == "api.github.com")
                    respond(
                        """{"message":"request failed"}""",
                        status,
                        headersOf(
                            HttpHeaders.Location to listOf("https://untrusted.invalid/"),
                            HttpHeaders.ContentType to listOf("application/json"),
                        ),
                    )
                }
            GitHubReleaseClient("example/azkar", "test-token", engine).use { client ->
                check(runCatching { client.releases() }.isFailure)
            }
            check(requests == 1)
        }
    }

    test("malformed JSON and invalid release fields fail closed") {
        listOf(
            "not-json",
            """[{"tag_name":"v1.2.3"}]""",
            """[{"tag_name":123,"prerelease":false,"draft":false,"published_at":null}]""",
            """[{"tag_name":"v1.2.3","prerelease":"not-a-boolean","draft":false,"published_at":null}]""",
            """[{"tag_name":"v1.2.3","prerelease":false,"draft":false}]""",
            """[{"tag_name":"v1.2.3","prerelease":false,"draft":false,"published_at":"bad-date"}]""",
        ).forEach { body ->
            val engine = MockEngine { respond(body, headers = jsonHeaders) }
            GitHubReleaseClient("example/azkar", engine = engine).use { client ->
                check(runCatching { client.releases() }.isFailure) { body }
            }
        }
    }

    test("public requests can omit credentials") {
        val engine =
            MockEngine { request ->
                check(request.headers[HttpHeaders.Authorization] == null)
                respond("[]", headers = jsonHeaders)
            }
        GitHubReleaseClient("example/azkar", engine = engine).use { client -> check(client.releases().isEmpty()) }
    }

    test("invalid comparison inputs cannot reach HTTP") {
        val engine = MockEngine { error("Invalid comparison must not make an HTTP request") }
        GitHubReleaseClient("example/azkar", engine = engine).use { client ->
            check(runCatching { client.comparison("../other", "a".repeat(40)) }.isFailure)
            check(runCatching { client.comparison("v1.2.4-beta.1", "../other") }.isFailure)
        }
    }

    test("cancelling the caller cancels an in-flight HTTP request") {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val engine =
            MockEngine {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        GitHubReleaseClient("example/azkar", engine = engine).use { client ->
            coroutineScope {
                val request = launch { client.releases() }
                withTimeout(5_000) {
                    started.await()
                    request.cancelAndJoin()
                    cancelled.await()
                }
                check(request.isCancelled)
            }
        }
    }

    test("script dependency versions match the project catalog") {
        val catalog = Files.readString(Path.of("gradle/libs.versions.toml"))
        val client = Files.readString(Path.of("scripts/release/github-client.main.kts"))
        val ktor = Regex("""(?m)^ktor = "([^"]+)"""").find(catalog)!!.groupValues[1]
        val serialization = Regex("""(?m)^jetbrains-serialization = "([^"]+)"""").find(catalog)!!.groupValues[1]
        val coroutines = Regex("""(?m)^jetbrains-coroutines = "([^"]+)"""").find(catalog)!!.groupValues[1]
        check("ktor-client-cio-jvm:$ktor" in client)
        check("ktor-client-content-negotiation-jvm:$ktor" in client)
        check("ktor-serialization-kotlinx-json-jvm:$ktor" in client)
        check("kotlinx-serialization-json-jvm:$serialization" in client)
        check("kotlinx-coroutines-core-jvm:$coroutines" in client)
        check(
            "kotlinx-coroutines-core-jvm:$coroutines" in
                Files.readString(Path.of("scripts/tests/test_release_workflow.main.kts")),
        )
        check("ktor-client-mock-jvm:$ktor" in Files.readString(Path.of("scripts/tests/test_github_client.main.kts")))
    }

    println("GitHub client fixtures passed: $passed test groups")
}
