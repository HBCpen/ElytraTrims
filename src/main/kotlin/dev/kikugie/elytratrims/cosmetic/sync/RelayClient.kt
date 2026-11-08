package dev.kikugie.elytratrims.cosmetic.sync

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

/**
 * HTTP client for the relay in `relay/`, which stores the latest [SignedCosmetic] of each player.
 * The relay is not trusted: every record is verified by the caller.
 */
class RelayClient(private val baseUrl: String) {
    class Rejected(val status: Int, message: String) : IOException(message)

    fun publish(record: SignedCosmetic): CompletableFuture<Unit> =
        send(request("v1/cosmetics/${record.profileId}").PUT(HttpRequest.BodyPublishers.ofString(record.toJson())).build())
            .thenApply { }

    /** Records stored for [ids]; ids without one are left out. */
    fun lookup(ids: Collection<UUID>): CompletableFuture<List<JsonElement>> {
        val body = JsonObject().apply { add("profile_ids", JsonArray().apply { ids.forEach { add(it.toString()) } }) }
        return send(request("v1/cosmetics/lookup").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build())
            .thenApply { text -> JsonParser.parseString(text).asJsonObject.getAsJsonArray("records").toList() }
    }

    private fun request(path: String): HttpRequest.Builder = HttpRequest.newBuilder(URI.create("$baseUrl/$path"))
        .timeout(TIMEOUT)
        .header("Content-Type", "application/json")
        .header("User-Agent", USER_AGENT)

    private fun send(request: HttpRequest): CompletableFuture<String> =
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenApply { response ->
            val text = response.body().use { it.readLimited() }
            if (response.statusCode() !in 200..299) throw Rejected(response.statusCode(), "Relay answered ${response.statusCode()}: ${text.take(200)}")
            text
        }

    private companion object {
        /** Far above a full lookup batch, but bounded so a broken relay can't exhaust memory. */
        const val MAX_RESPONSE_BYTES = 1 shl 20

        val TIMEOUT: Duration = Duration.ofSeconds(15)

        val HTTP: HttpClient by lazy {
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()
        }

        val USER_AGENT: String by lazy {
            val version = FabricLoader.getInstance().getModContainer("elytratrims_cosmetic").getOrNull()?.metadata?.version?.friendlyString
            "ElytraTrimsCosmetic/${version ?: "unknown"}"
        }

        fun InputStream.readLimited(): String {
            val bytes = readNBytes(MAX_RESPONSE_BYTES + 1)
            if (bytes.size > MAX_RESPONSE_BYTES) throw IOException("Relay response is too large")
            return bytes.toString(Charsets.UTF_8)
        }
    }
}
