package dev.kikugie.elytratrims.cosmetic.sync

import com.google.gson.JsonElement
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings.Sync.Published
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings.SyncMode
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
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
 * show a cosmetic they didn't publish, and a record older than one already seen is ignored, so a relay can't roll a
 * player back to an earlier cosmetic either.
 */
object CosmeticSync {
    private val LOGGER: Logger = LoggerFactory.getLogger("ElytraTrims Cosmetic Sync")

    private const val CHECK_INTERVAL_TICKS = 40
    private const val MAX_BATCH = 100
    /** Bounds the cache on servers with many players coming and going. */
    private const val MAX_REMOTES = 4096
    private val REFRESH_AFTER: Duration = Duration.ofMinutes(5)
    private val FORGET_AFTER: Duration = Duration.ofMinutes(15)
    private val MIN_LOOKUP_GAP: Duration = Duration.ofSeconds(10)
    private val MIN_RETRY: Duration = Duration.ofMinutes(1)
    private val MAX_RETRY: Duration = Duration.ofMinutes(15)

    /**
     * What is known about another player. [record] is the newest verified record and is kept, even while the player
     * is out of sight, until its key expires, so that an older record can be recognized as such.
     */
    private class Remote(val record: SignedCosmetic.Verified?, val fetchedAt: Instant, @Volatile var seenAt: Instant)

    /** What was last published in this session, to republish only on changes. */
    private data class Sent(val relay: String, val profileId: UUID, val cosmetic: ElytraCosmetic?, val keys: ProfileKeyPair)

    private val remotes = ConcurrentHashMap<UUID, Remote>()

    private var ticks = 0
    private var lookupInFlight = false
    private var nextLookup: Instant = Instant.EPOCH
    private var lookupRetry: Duration = MIN_RETRY

    private var sent: Sent? = null
    private var publishInFlight = false
    private var nextPublish: Instant = Instant.EPOCH
    private var publishRetry: Duration = MIN_RETRY

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> if (++ticks % CHECK_INTERVAL_TICKS == 0) tick(client) }
    }

    /** The verified cosmetic of another player, or `null` if they have none, its key expired or syncing is off. */
    @JvmStatic
    fun cosmeticFor(profileId: UUID): ElytraCosmetic? {
        if (CosmeticSettings.data.sync.mode == SyncMode.OFF) return null
        val record = remotes[profileId]?.record ?: return null
        return record.cosmetic.takeIf { record.expiresAt.toEpochMilli() > System.currentTimeMillis() }
    }

    /**
     * Takes a verified record of [profileId] (or `null` if the relay has none) as if it was fetched from the relay.
     * Records older than the known one are ignored. Public for tests.
     */
    fun accept(profileId: UUID, record: SignedCosmetic.Verified?, now: Instant = Instant.now()) {
        val known = remotes[profileId]?.record?.takeIf { it.expiresAt.isAfter(now) }
        // A relay never drops a record before its key expires, and a newer record replaces it; keep what we know otherwise.
        val newest = if (record == null || (known != null && record.issuedAt.isBefore(known.issuedAt))) known else record
        remotes[profileId] = Remote(newest, now, now)
    }

    private fun tick(client: Minecraft) {
        val level = client.level ?: return
        val sync = CosmeticSettings.data.sync
        val now = Instant.now()
        val relay = sync.relay
        if (relay != null && sync.mode != SyncMode.OFF) lookup(client, relay, level.players().map { it.uuid }, now)
        publish(client, sync, now)
    }

    // Fetching others

    private fun lookup(client: Minecraft, relay: String, players: List<UUID>, now: Instant) {
        val self = client.user.profileId
        for (id in players) remotes[id]?.seenAt = now
        forgetStale(now)
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
                        LOGGER.warn("Failed to fetch cosmetics from {}: {}", relay, error.unwrap().toString())
                        nextLookup = Instant.now() + lookupRetry
                        lookupRetry = (lookupRetry * 2).coerceAtMost(MAX_RETRY)
                        return@execute
                    }
                    lookupRetry = MIN_RETRY
                    val fetched = Instant.now()
                    for (id in due) accept(id, verified[id], fetched)
                }
            }
    }

    private fun forgetStale(now: Instant) {
        remotes.values.removeIf { remote ->
            val unseen = remote.seenAt.plus(FORGET_AFTER).isBefore(now)
            unseen && remote.record?.expiresAt?.isAfter(now) != true
        }
        if (remotes.size > MAX_REMOTES) remotes.entries.sortedBy { it.value.seenAt }
            .take(remotes.size - MAX_REMOTES).forEach { remotes.remove(it.key, it.value) }
    }

    private fun verifyAll(requested: List<UUID>, records: List<JsonElement>, services: SignatureValidator): Map<UUID, SignedCosmetic.Verified> {
        val wanted = requested.toSet()
        val result = HashMap<UUID, SignedCosmetic.Verified>()
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

    /**
     * Publishes the own cosmetic while sharing, and withdraws it from every relay that may still serve it otherwise.
     * Relays that may serve it are remembered in the config until the withdrawal is confirmed.
     */
    private fun publish(client: Minecraft, sync: CosmeticSettings.Sync, now: Instant) {
        if (publishInFlight || now.isBefore(nextPublish)) return
        val keys = client.connection?.chatSession?.keyPair() ?: return
        val profileId = client.user.profileId
        val relay = sync.relay.takeIf { sync.mode == SyncMode.SHARE }

        // First withdraw from relays that shouldn't serve the cosmetic anymore: sharing is off, or the relay changed.
        val stale = sync.published.firstOrNull { it.profileId == profileId && it.relay != relay }
        if (stale != null) return send(client, Published(profileId, stale.relay), null, keys)
        if (relay == null) return

        val cosmetic = CosmeticSettings.active
        val target = Published(profileId, relay)
        val last = sent?.takeIf { it.relay == relay && it.profileId == profileId }
        // A new key pair means the old record expires with the old key, so sign it again.
        if (last != null && last.cosmetic == cosmetic && last.keys == keys) return
        // Nothing to share and nothing that could still be served: no need to contact the relay.
        if (cosmetic == null && target !in sync.published) return
        send(client, target, cosmetic, keys)
    }

    private fun send(client: Minecraft, target: Published, cosmetic: ElytraCosmetic?, keys: ProfileKeyPair) {
        val record = try {
            SignedCosmetic.sign(target.profileId, cosmetic, keys)
        } catch (e: Exception) {
            LOGGER.warn("Can't share the cosmetic: {}", e.message)
            nextPublish = Instant.now() + MAX_RETRY
            return
        }
        // Remember the relay before sending: the relay may store the record even if the response gets lost.
        if (cosmetic != null && target !in CosmeticSettings.data.sync.published)
            CosmeticSettings.updatePublished(CosmeticSettings.data.sync.published + target)

        publishInFlight = true
        RelayClient(target.relay).publish(record).whenComplete { _, error ->
            client.execute {
                publishInFlight = false
                val cause = error?.unwrap()
                when {
                    cause is RelayClient.Outdated -> {
                        // The relay keeps a newer record, e.g. from another device whose clock is ahead.
                        // Sign again once this clock has passed it, so the new record wins.
                        LOGGER.info("The relay has a newer cosmetic of this account; publishing again after {}", cause.storedIssuedAt)
                        nextPublish = maxOf(Instant.now() + Duration.ofSeconds(5), cause.storedIssuedAt + Duration.ofSeconds(1))
                    }
                    cause != null -> {
                        LOGGER.warn("Failed to publish the cosmetic to {}: {}", target.relay, cause.toString())
                        // A rejection won't change by retrying soon, so wait longer for those.
                        if (cause is RelayClient.Rejected) publishRetry = MAX_RETRY
                        nextPublish = Instant.now() + publishRetry
                        publishRetry = (publishRetry * 2).coerceAtMost(MAX_RETRY)
                    }
                    else -> {
                        publishRetry = MIN_RETRY
                        sent = Sent(target.relay, target.profileId, cosmetic, keys)
                        if (cosmetic == null) CosmeticSettings.updatePublished(CosmeticSettings.data.sync.published - target)
                    }
                }
            }
        }
    }

    private fun Throwable.unwrap(): Throwable = if (this is CompletionException) cause ?: this else this

    private operator fun Duration.times(factor: Int): Duration = multipliedBy(factor.toLong())
}
