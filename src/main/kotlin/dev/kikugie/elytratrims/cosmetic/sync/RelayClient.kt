package dev.kikugie.elytratrims.cosmetic.sync

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

/**
 * HTTP client for the relay in `relay/`, which stores the latest [SignedCosmetic] of each player.
 * The relay is not trusted: every record is verified by the caller.
 *
 * @param timeout limit for a whole request, including the response body
 */
class RelayClient(private val baseUrl: String, private val timeout: Duration = TIMEOUT) {
    /** The relay answered with an error status. */
    open class Rejected(val status: Int, message: String) : IOException(message)

    /** The relay already stores a record issued at [storedIssuedAt], which is not older than the published one. */
    class Outdated(val storedIssuedAt: Instant) : Rejected(409, "The relay stores a record issued at $storedIssuedAt")

    fun publish(record: SignedCosmetic): CompletableFuture<Unit> = guarded {
        send(request("v1/cosmetics/${record.profileId}").PUT(HttpRequest.BodyPublishers.ofString(record.toJson())).build())
            .thenApply { }
    }

    /** Records stored for [ids]; ids without one are left out. */
    fun lookup(ids: Collection<UUID>): CompletableFuture<List<JsonElement>> = guarded {
        val body = JsonObject().apply { add("profile_ids", JsonArray().apply { ids.forEach { add(it.toString()) } }) }
        send(request("v1/cosmetics/lookup").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build())
            .thenApply { text -> JsonParser.parseString(text).asJsonObject.getAsJsonArray("records").toList() }
    }

    /** Reports failures to build or start a request (such as a malformed URL) through the future, like any other. */
    private inline fun <T> guarded(block: () -> CompletableFuture<T>): CompletableFuture<T> =
        try { block() } catch (e: Exception) { CompletableFuture.failedFuture(e) }

    private fun request(path: String): HttpRequest.Builder = HttpRequest.newBuilder(URI.create("$baseUrl/$path"))
        .timeout(timeout)
        .header("Content-Type", "application/json")
        .header("User-Agent", USER_AGENT)

    private fun send(request: HttpRequest): CompletableFuture<String> {
        val response = HTTP.sendAsync(request, HttpResponse.BodyHandlers.limiting(HttpResponse.BodyHandlers.ofString(), MAX_RESPONSE_BYTES))
        // The request timeout only covers the headers; also give up on a body that never finishes.
        CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS).execute { response.cancel(true) }
        return response.thenApply { it.checkStatus() }
    }

    private fun HttpResponse<String>.checkStatus(): String {
        val status = statusCode()
        if (status in 200..299) return body()
        if (status == 409) runCatching { JsonParser.parseString(body()).asJsonObject["issued_at"].asLong }.getOrNull()
            ?.let { throw Outdated(Instant.ofEpochMilli(it)) }
        throw Rejected(status, "Relay answered $status: ${body().take(200)}")
    }

    private companion object {
        /** Far above a full lookup batch, but bounded so a broken relay can't exhaust memory. */
        const val MAX_RESPONSE_BYTES = 1L shl 20

        val TIMEOUT: Duration = Duration.ofSeconds(15)

        val HTTP: HttpClient by lazy {
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()
        }

        val USER_AGENT: String by lazy {
            val version = FabricLoader.getInstance().getModContainer("elytratrims_cosmetic").getOrNull()?.metadata?.version?.friendlyString
            "ElytraTrimsCosmetic/${version ?: "unknown"}"
        }
    }
}
