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
    /**
     * Bounds the cache on servers with many players coming and going. Entries that protect against rollback (a valid
     * record) are never evicted; when the cache is full of them, further players aren't synced until some expire.
     */
    const val MAX_REMOTES = 4096
    private val REFRESH_AFTER: Duration = Duration.ofMinutes(5)
    private val FORGET_AFTER: Duration = Duration.ofMinutes(15)
    private val MIN_LOOKUP_GAP: Duration = Duration.ofSeconds(10)
    private val MIN_RETRY: Duration = Duration.ofMinutes(1)
    private val MAX_RETRY: Duration = Duration.ofMinutes(15)

    /**
     * What is known about another player. [record] is the newest verified record and is kept, even while the player
     * is out of sight, until its key expires, so that an older record can be recognized as such.
     */
    private class Remote(val record: SignedCosmetic.Verified?, val fetchedAt: Instant, @Volatile var seenAt: Instant) {
        fun protects(now: Instant): Boolean = record != null && record.expiresAt.isAfter(now)
    }

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

    /** Withdrawals from relays no longer shared to, retried per relay so a dead relay doesn't hold up anything else. */
    private var withdrawInFlight = false
    private val nextWithdraw = HashMap<String, Instant>()
    private val withdrawRetry = HashMap<String, Duration>()

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
        if (!remotes.containsKey(profileId) && !makeRoom(now)) return
        val known = remotes[profileId]?.record?.takeIf { it.expiresAt.isAfter(now) }
        // A relay never drops a record before its key expires, and a newer record replaces it; keep what we know otherwise.
        val newest = if (record == null || (known != null && record.issuedAt.isBefore(known.issuedAt))) known else record
        remotes[profileId] = Remote(newest, now, now)
    }

    /** Forgets everything about other players; for tests. */
    fun clearRemotes() = remotes.clear()

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
        // With a full cache, only players already in it are refreshed, since new ones couldn't be stored anyway.
        val full = remotes.size >= MAX_REMOTES
        val due = players.asSequence()
            .filter { it != self && it.version() == 4 && (!full || remotes.containsKey(it)) }
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

    /** Forgets players out of sight for a while, except for records that still protect against rollback. */
    private fun forgetStale(now: Instant) {
        remotes.values.removeIf { remote -> remote.seenAt.plus(FORGET_AFTER).isBefore(now) && !remote.protects(now) }
    }

    /** Whether another player fits in the cache, evicting the longest unseen entry that doesn't protect anything. */
    private fun makeRoom(now: Instant): Boolean {
        if (remotes.size < MAX_REMOTES) return true
        val victim = remotes.entries.filter { !it.value.protects(now) }.minByOrNull { it.value.seenAt } ?: return false
        return remotes.remove(victim.key, victim.value)
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
     * Relays that may serve it are noted in the config (see [CosmeticSettings.Sync.published]) before publishing,
     * and the note is removed once a withdrawal is confirmed or the signing key has expired.
     */
    private fun publish(client: Minecraft, sync: CosmeticSettings.Sync, now: Instant) {
        val keys = client.connection?.chatSession?.keyPair() ?: return
        val profileId = client.user.profileId
        val relay = sync.relay.takeIf { sync.mode == SyncMode.SHARE }

        // Records signed with an expired key are no longer served, so there's nothing left to withdraw.
        if (sync.published.any { !it.expiresAt.isAfter(now) })
            CosmeticSettings.updatePublished(sync.published.filter { it.expiresAt.isAfter(now) })

        if (!withdrawInFlight) CosmeticSettings.data.sync.published
            .filter { it.profileId == profileId && it.relay != relay && !now.isBefore(nextWithdraw[it.relay] ?: Instant.EPOCH) }
            .minByOrNull { nextWithdraw[it.relay] ?: Instant.EPOCH }
            ?.let { withdraw(client, it, keys) }

        if (relay != null && !publishInFlight && !now.isBefore(nextPublish)) share(client, profileId, relay, keys, now)
    }

    /** Publishes the current cosmetic, or a withdrawal when there's none, to the relay shared to. */
    private fun share(client: Minecraft, profileId: UUID, relay: String, keys: ProfileKeyPair, now: Instant) {
        val cosmetic = CosmeticSettings.active
        val published = CosmeticSettings.data.sync.published
        val note = published.firstOrNull { it.isFor(profileId, relay) }
        val last = sent?.takeIf { it.relay == relay && it.profileId == profileId }
        // A new key pair means the old record expires with the old key, so sign it again.
        if (last != null && last.cosmetic == cosmetic && last.keys == keys) return
        // Nothing to share and nothing that could still be served: no need to contact the relay.
        if (cosmetic == null && note == null) return

        val record = sign(profileId, cosmetic, keys)
        if (record == null) {
            nextPublish = now + MAX_RETRY
            return
        }
        if (cosmetic != null) {
            // Note the relay before sending: it may store the record even if the response gets lost.
            val expiresAt = maxOf(keys.publicKey().data().expiresAt(), note?.expiresAt ?: Instant.EPOCH)
            if (note?.expiresAt != expiresAt) {
                val noted = published.filterNot { it.isFor(profileId, relay) } + Published(profileId, relay, expiresAt)
                if (!CosmeticSettings.updatePublished(noted)) {
                    LOGGER.warn("Not sharing the cosmetic, since the config can't be saved to note where it is shared")
                    nextPublish = now + publishRetry
                    publishRetry = (publishRetry * 2).coerceAtMost(MAX_RETRY)
                    return
                }
            }
        }

        publishInFlight = true
        RelayClient(relay).publish(record).whenComplete { _, error ->
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
                        LOGGER.warn("Failed to publish the cosmetic to {}: {}", relay, cause.toString())
                        // A rejection won't change by retrying soon, so wait longer for those.
                        if (cause is RelayClient.Rejected) publishRetry = MAX_RETRY
                        nextPublish = Instant.now() + publishRetry
                        publishRetry = (publishRetry * 2).coerceAtMost(MAX_RETRY)
                    }
                    else -> {
                        publishRetry = MIN_RETRY
                        sent = Sent(relay, profileId, cosmetic, keys)
                        if (cosmetic == null) forget(profileId, relay)
                    }
                }
            }
        }
    }

    /** Withdraws the cosmetic from a relay that is no longer shared to. */
    private fun withdraw(client: Minecraft, note: Published, keys: ProfileKeyPair) {
        val record = sign(note.profileId, null, keys) ?: return
        withdrawInFlight = true
        RelayClient(note.relay).publish(record).whenComplete { _, error ->
            client.execute {
                withdrawInFlight = false
                val cause = error?.unwrap()
                if (cause == null) {
                    nextWithdraw.remove(note.relay)
                    withdrawRetry.remove(note.relay)
                    forget(note.profileId, note.relay)
                    return@execute
                }
                LOGGER.warn("Failed to withdraw the cosmetic from {}: {}", note.relay, cause.toString())
                val retry = withdrawRetry[note.relay] ?: MIN_RETRY
                withdrawRetry[note.relay] = (retry * 2).coerceAtMost(MAX_RETRY)
                // 409: a newer record from elsewhere is stored; sign again once this clock has passed it.
                val after = (cause as? RelayClient.Outdated)?.storedIssuedAt?.plusSeconds(1) ?: Instant.EPOCH
                nextWithdraw[note.relay] = maxOf(Instant.now() + retry, after)
            }
        }
    }

    private fun sign(profileId: UUID, cosmetic: ElytraCosmetic?, keys: ProfileKeyPair): SignedCosmetic? = try {
        SignedCosmetic.sign(profileId, cosmetic, keys)
    } catch (e: Exception) {
        LOGGER.warn("Can't sign the cosmetic: {}", e.message)
        null
    }

    private fun forget(profileId: UUID, relay: String) {
        val published = CosmeticSettings.data.sync.published
        if (published.any { it.isFor(profileId, relay) })
            CosmeticSettings.updatePublished(published.filterNot { it.isFor(profileId, relay) })
    }

    private fun Throwable.unwrap(): Throwable = if (this is CompletionException) cause ?: this else this

    private operator fun Duration.times(factor: Int): Duration = multipliedBy(factor.toLong())
}
