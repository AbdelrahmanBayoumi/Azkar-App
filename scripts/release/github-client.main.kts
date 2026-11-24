@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.6.1")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.6.1")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json-jvm:3.6.1")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.13.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
@file:Import("release-rules.main.kts")

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String,
    val prerelease: Boolean,
    val draft: Boolean,
    @SerialName("published_at") val publishedAt: String?,
) {
    fun toReleaseInfo(): ReleaseInfo = ReleaseInfo(tagName, prerelease, draft, publishedAt?.let(Instant::parse))
}

@Serializable
data class GitHubComparisonResponse(
    val status: String,
)

class GitHubReleaseClient(
    private val repository: String,
    private val token: String? = null,
    engine: HttpClientEngine = CIO.create(),
) : AutoCloseable {
    init {
        require(Regex("""[A-Za-z0-9_-]+/[A-Za-z0-9_.-]+""").matches(repository)) { "Invalid GitHub repository" }
        require(repository.substringAfter('/') !in setOf(".", "..")) { "Invalid GitHub repository" }
    }

    private val client =
        HttpClient(engine) {
            expectSuccess = true
            // Never forward the token to a redirect target.
            followRedirects = false
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
        }

    private suspend fun request(
        path: String,
        page: Int? = null,
    ): HttpResponse =
        client.get("https://api.github.com/repos/$repository/$path") {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header(HttpHeaders.UserAgent, "Azkar-release-scripts")
            header("X-GitHub-Api-Version", "2022-11-28")
            token?.takeIf { it.isNotBlank() }?.let { bearerAuth(it) }
            if (page != null) {
                parameter("per_page", 100)
                parameter("page", page)
            }
        }

    private fun HttpResponse.hasNextPage(): Boolean =
        headers[HttpHeaders.Link].orEmpty().split(',').any { link ->
            link.split(';').drop(1).any { it.trim() == "rel=\"next\"" }
        }

    suspend fun releases(): List<ReleaseInfo> {
        val releases = mutableListOf<ReleaseInfo>()
        var page = 1
        do {
            val response = request("releases", page++)
            releases += response.body<List<GitHubReleaseResponse>>().map { it.toReleaseInfo() }
            // Increment our own page number instead of following arbitrary Link URLs.
        } while (response.hasNextPage())
        return releases
    }

    suspend fun comparison(
        baseTag: String,
        head: String,
    ): String {
        require(devTagPattern.matches(baseTag)) { "Invalid Dev release tag" }
        require(Regex("[0-9a-fA-F]{40}").matches(head)) { "Expected a full commit SHA" }
        return request("compare/$baseTag...$head").body<GitHubComparisonResponse>().status
    }

    override fun close() = client.close()
}
