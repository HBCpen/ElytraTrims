package dev.kikugie.elytratrims.cosmetic.sync

import dev.kikugie.elytratrims.cosmetic.CosmeticSettings
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings.SyncMode
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import com.google.gson.JsonElement
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.util.SignatureValidator
import net.minecraft.world.entity.player.ProfileKeyPair
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap

/**
 * Publishes the local player's cosmetic to the relay and fetches those of the players around.
 *
 * Everything runs on the client thread except the HTTP requests and the verification of their responses.
 * Records are only trusted after [SignedCosmetic.verify], so neither the relay nor other players can make someone
 * show a cosmetic they didn't publish.
 */
object CosmeticSync {
    private val LOGGER: Logger = LoggerFactory.getLogger("ElytraTrims Cosmetic Sync")

    private const val CHECK_INTERVAL_TICKS = 40
    private const val MAX_BATCH = 100
    private val REFRESH_AFTER: Duration = Duration.ofMinutes(5)
    private val FORGET_AFTER: Duration = Duration.ofMinutes(15)
    private val MIN_LOOKUP_GAP: Duration = Duration.ofSeconds(10)
    private val MIN_RETRY: Duration = Duration.ofMinutes(1)
    private val MAX_RETRY: Duration = Duration.ofMinutes(15)

    private class Remote(val cosmetic: ElytraCosmetic?, val fetchedAt: Instant, @Volatile var seenAt: Instant)

    /** What was last published in this session, to republish only on changes. */
    private data class Published(val relay: String, val profileId: UUID, val cosmetic: ElytraCosmetic?, val keys: ProfileKeyPair)

    private val remotes = ConcurrentHashMap<UUID, Remote>()

    private var ticks = 0
    private var lookupInFlight = false
    private var nextLookup: Instant = Instant.EPOCH
    private var lookupRetry: Duration = MIN_RETRY

    private var published: Published? = null
    private var publishInFlight = false
    private var nextPublish: Instant = Instant.EPOCH
    private var publishRetry: Duration = MIN_RETRY

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> if (++ticks % CHECK_INTERVAL_TICKS == 0) tick(client) }
    }

    /** The verified cosmetic of another player, or `null` if they have none or syncing is off. */
    @JvmStatic
    fun cosmeticFor(profileId: UUID): ElytraCosmetic? =
        if (CosmeticSettings.data.sync.mode == SyncMode.OFF) null else remotes[profileId]?.cosmetic

    /** Shows [cosmetic] for [profileId] as if it was fetched from the relay; for tests. */
    fun putVerified(profileId: UUID, cosmetic: ElytraCosmetic?) {
        val now = Instant.now()
        remotes[profileId] = Remote(cosmetic, now, now)
    }

    private fun tick(client: Minecraft) {
        val level = client.level ?: return
        val sync = CosmeticSettings.data.sync
        val relay = sync.relay ?: return
        val now = Instant.now()
        if (sync.mode != SyncMode.OFF) lookup(client, relay, level.players().map { it.uuid }, now)
        publish(client, relay, sync.mode, now)
    }

    // Fetching others

    private fun lookup(client: Minecraft, relay: String, players: List<UUID>, now: Instant) {
        val self = client.user.profileId
        for (id in players) remotes[id]?.seenAt = now
        remotes.values.removeIf { it.seenAt.plus(FORGET_AFTER).isBefore(now) }
        if (lookupInFlight || now.isBefore(nextLookup)) return

        // Only online-mode accounts (random UUIDs) can have certified keys; skips offline players and NPCs.
        val due = players.asSequence()
            .filter { it != self && it.version() == 4 }
            .filter { id -> remotes[id].let { it == null || it.fetchedAt.plus(REFRESH_AFTER).isBefore(now) } }
            .distinct().take(MAX_BATCH).toList()
        if (due.isEmpty()) return
        val services = client.services().profileKeySignatureValidator() ?: return

        lookupInFlight = true
        nextLookup = now + MIN_LOOKUP_GAP
        RelayClient(relay).lookup(due)
            .thenApply { records -> verifyAll(due, records, services) }
            .whenComplete { verified, error ->
                client.execute {
                    lookupInFlight = false
                    if (error != null) {
                        LOGGER.warn("Failed to fetch cosmetics from {}: {}", relay, error.cause())
                        nextLookup = Instant.now() + lookupRetry
                        lookupRetry = (lookupRetry * 2).coerceAtMost(MAX_RETRY)
                        return@execute
                    }
                    lookupRetry = MIN_RETRY
                    val fetched = Instant.now()
                    for (id in due) remotes[id] = Remote(verified[id], fetched, fetched)
                }
            }
    }

    private fun verifyAll(requested: List<UUID>, records: List<JsonElement>, services: SignatureValidator): Map<UUID, ElytraCosmetic?> {
        val wanted = requested.toSet()
        val result = HashMap<UUID, ElytraCosmetic?>()
        for (json in records) {
            val record = SignedCosmetic.fromJson(json) ?: continue
            if (record.profileId !in wanted || record.profileId in result) continue
            try {
                result[record.profileId] = record.verify(record.profileId, services)
            } catch (e: SignedCosmetic.InvalidException) {
                LOGGER.warn("Ignoring the cosmetic of {}: {}", record.profileId, e.message)
            }
        }
        return result
    }

    // Publishing our own

    private fun publish(client: Minecraft, relay: String, mode: SyncMode, now: Instant) {
        if (publishInFlight || now.isBefore(nextPublish)) return
        val keys = client.connection?.chatSession?.keyPair() ?: return
        val profileId = client.user.profileId
        val last = published?.takeIf { it.relay == relay && it.profileId == profileId }

        val cosmetic = if (mode == SyncMode.SHARE) CosmeticSettings.active else null
        // Without sharing, only withdraw what this session published; never contact the relay otherwise.
        if (mode != SyncMode.SHARE && (last == null || last.cosmetic == null)) return
        // A new key pair means the old record expires with the old key, so sign it again.
        if (last != null && last.cosmetic == cosmetic && last.keys == keys) return

        val record = try {
            SignedCosmetic.sign(profileId, cosmetic, keys)
        } catch (e: Exception) {
            LOGGER.warn("Can't share the cosmetic: {}", e.message)
            nextPublish = now + MAX_RETRY
            return
        }
        publishInFlight = true
        RelayClient(relay).publish(record).whenComplete { _, error ->
            client.execute {
                publishInFlight = false
                if (error != null) {
                    LOGGER.warn("Failed to publish the cosmetic to {}: {}", relay, error.cause())
                    // A rejection won't change by retrying soon, so wait longer for those.
                    publishRetry = if (error.cause() is RelayClient.Rejected) MAX_RETRY else publishRetry
                    nextPublish = Instant.now() + publishRetry
                    publishRetry = (publishRetry * 2).coerceAtMost(MAX_RETRY)
                    return@execute
                }
                publishRetry = MIN_RETRY
                published = Published(relay, profileId, cosmetic, keys)
            }
        }
    }

    private fun Throwable.cause(): Throwable = if (this is CompletionException) cause ?: this else this

    private operator fun Duration.times(factor: Int): Duration = multipliedBy(factor.toLong())
}
