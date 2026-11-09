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
import java.util.concurrent.CompletableFuture
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

    private val remotes = ConcurrentHashMap<UUID, Remote>()

    private var ticks = 0
    /**
     * Requests in flight. Their results are collected from the client tick rather than handed to the client's task
     * queue, which a disconnect empties; a dropped result would leave the request in flight forever.
     */
    private class PendingLookup(val future: CompletableFuture<Map<UUID, SignedCosmetic.Verified>>, val relay: String, val due: List<UUID>)
    private class PendingWrite(val future: CompletableFuture<Unit>, val profileId: UUID, val write: Write, val keys: ProfileKeyPair)

    private var pendingLookup: PendingLookup? = null
    private var pendingWrite: PendingWrite? = null
    private var nextLookup: Instant = Instant.EPOCH
    private var lookupRetry: Duration = MIN_RETRY

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            collectResults()
            if (++ticks % CHECK_INTERVAL_TICKS == 0) tick(client)
        }
    }

    /** Handles requests that finished since the last tick, also while not in a world. Public for tests. */
    fun collectResults() {
        pendingLookup?.takeIf { it.future.isDone }?.let {
            pendingLookup = null
            lookupCompleted(it)
        }
        pendingWrite?.takeIf { it.future.isDone }?.let {
            pendingWrite = null
            completed(it.profileId, it.write, it.keys, it.future.errorOrNull(), Instant.now())
        }
    }

    /** Tracks [future] as the result of [write]; for tests and [publish]. */
    fun track(profileId: UUID, write: Write, keys: ProfileKeyPair, future: CompletableFuture<Unit>) {
        started(profileId, write)
        pendingWrite = PendingWrite(future, profileId, write, keys)
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
        if (pendingLookup != null || now.isBefore(nextLookup)) return

        // Only online-mode accounts (random UUIDs) can have certified keys; skips offline players and NPCs.
        // Newcomers are only looked up while there is room for them, counting entries that could be evicted as room.
        var room = MAX_REMOTES - remotes.values.count { it.protects(now) }
        val due = players.asSequence()
            .filter { it != self && it.version() == 4 }
            .filter { id -> remotes[id].let { it == null || it.fetchedAt.plus(REFRESH_AFTER).isBefore(now) } }
            .distinct()
            .filter { id -> remotes.containsKey(id) || room-- > 0 }
            .take(MAX_BATCH).toList()
        if (due.isEmpty()) return
        val services = client.services().profileKeySignatureValidator() ?: return

        nextLookup = now + MIN_LOOKUP_GAP
        pendingLookup = PendingLookup(RelayClient(relay).lookup(due).thenApply { records -> verifyAll(due, records, services) }, relay, due)
    }

    private fun lookupCompleted(lookup: PendingLookup) {
        val error = lookup.future.errorOrNull()
        if (error != null) {
            LOGGER.warn("Failed to fetch cosmetics from {}: {}", lookup.relay, error.toString())
            nextLookup = Instant.now() + lookupRetry
            lookupRetry = (lookupRetry * 2).coerceAtMost(MAX_RETRY)
            return
        }
        lookupRetry = MIN_RETRY
        val verified = lookup.future.join()
        val fetched = Instant.now()
        for (id in lookup.due) accept(id, verified[id], fetched)
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
    //
    // Each relay that may serve the own cosmetic is noted in the config (CosmeticSettings.Sync.published) before
    // publishing to it, and the note is removed once the relay confirmed a withdrawal or the signing key expired.
    // Only one write is in flight at a time, so a withdrawal and a publication to the same relay can't overtake each
    // other, while retries are scheduled per relay, so a broken relay doesn't hold up the others.

    /** A write to send: [cosmetic] to [relay], where `null` withdraws it. */
    data class Write(val relay: String, val cosmetic: ElytraCosmetic?)

    /** What a relay is known to serve after its last confirmed write, and when to try it again after a failure. */
    private class RelayState {
        /** `null` when unknown, e.g. while a write is in flight. */
        var confirmed: Confirmed? = null
        var next: Instant = Instant.EPOCH
        var retry: Duration = MIN_RETRY

        val withdrawn: Boolean get() = confirmed.let { it != null && it.cosmetic == null }
    }

    private data class Confirmed(val cosmetic: ElytraCosmetic?, val keys: ProfileKeyPair)

    private val relays = HashMap<Pair<UUID, String>, RelayState>()
    private var writeInFlight = false
    private var nextNoteWrite: Instant = Instant.EPOCH
    private var noteWriteRetry: Duration = MIN_RETRY

    private fun state(profileId: UUID, relay: String) = relays.getOrPut(profileId to relay, ::RelayState)

    /** Forgets what relays are known to serve; for tests. */
    fun resetPublishing() {
        relays.clear()
        pendingWrite = null
        writeInFlight = false
        nextNoteWrite = Instant.EPOCH
        noteWriteRetry = MIN_RETRY
    }

    /**
     * The next write to make: the current [cosmetic] to [current] (the relay shared to, `null` when not sharing) if
     * it doesn't serve it yet, otherwise a withdrawal from a noted relay that is no longer shared to.
     * `null` while a write is in flight or everything is up to date or waiting for a retry. Public for tests.
     */
    fun nextWrite(profileId: UUID, current: String?, cosmetic: ElytraCosmetic?, keys: ProfileKeyPair, notes: List<Published>, now: Instant): Write? {
        if (writeInFlight) return null
        if (current != null) {
            val state = state(profileId, current)
            val confirmed = state.confirmed
            // A withdrawal needs no new signature when the key changes; a cosmetic does, as it expires with the key.
            val upToDate = confirmed != null && confirmed.cosmetic == cosmetic && (cosmetic == null || confirmed.keys == keys)
            val mayServe = notes.any { it.isFor(profileId, current) }
            if (!upToDate && (cosmetic != null || mayServe) && !now.isBefore(state.next)) return Write(current, cosmetic)
        }
        return notes.asSequence()
            .filter { it.profileId == profileId && it.relay != current }
            .map { it.relay to state(profileId, it.relay) }
            .filter { (_, state) -> !state.withdrawn && !now.isBefore(state.next) }
            .minByOrNull { (_, state) -> state.next }
            ?.let { (relay, _) -> Write(relay, null) }
    }

    /** Marks [write] as sent; it's unknown what the relay serves until [completed] is called. Public for tests. */
    fun started(profileId: UUID, write: Write) {
        writeInFlight = true
        state(profileId, write.relay).confirmed = null
    }

    /** Records the outcome of [write]: `null` [error] means the relay confirmed it. Public for tests. */
    fun completed(profileId: UUID, write: Write, keys: ProfileKeyPair, error: Throwable?, now: Instant) {
        writeInFlight = false
        val state = state(profileId, write.relay)
        when (error) {
            null -> {
                state.confirmed = Confirmed(write.cosmetic, keys)
                state.retry = MIN_RETRY
                state.next = Instant.EPOCH
            }
            is RelayClient.Outdated -> {
                // The relay keeps a newer record, e.g. from another device whose clock is ahead.
                // Sign again once this clock has passed it, so the new record wins.
                LOGGER.info("{} has a newer record of this account; writing again after {}", write.relay, error.storedIssuedAt)
                state.next = maxOf(now + Duration.ofSeconds(5), error.storedIssuedAt + Duration.ofSeconds(1))
            }
            else -> {
                LOGGER.warn("Failed to update the cosmetic on {}: {}", write.relay, error.toString())
                // A rejection won't change by retrying soon, so wait longer for those; rate limits and server errors may.
                if (error is RelayClient.Rejected && error.status != 429 && error.status < 500) state.retry = MAX_RETRY
                state.next = now + state.retry
                state.retry = (state.retry * 2).coerceAtMost(MAX_RETRY)
            }
        }
    }

    /** Notes that are still needed: not expired, and not for a relay that confirmed a withdrawal. Public for tests. */
    fun notesToKeep(profileId: UUID, notes: List<Published>, now: Instant): List<Published> = notes.filter { note ->
        note.expiresAt.isAfter(now) && !(note.profileId == profileId && relays[profileId to note.relay]?.withdrawn == true)
    }

    private fun publish(client: Minecraft, sync: CosmeticSettings.Sync, now: Instant) {
        val keys = client.connection?.chatSession?.keyPair() ?: return
        val profileId = client.user.profileId
        tidyNotes(profileId, now)

        val current = sync.relay.takeIf { sync.mode == SyncMode.SHARE }
        val cosmetic = if (current != null) CosmeticSettings.active else null
        val write = nextWrite(profileId, current, cosmetic, keys, CosmeticSettings.data.sync.published, now) ?: return

        val record = try {
            SignedCosmetic.sign(profileId, write.cosmetic, keys)
        } catch (e: Exception) {
            LOGGER.warn("Can't sign the cosmetic: {}", e.message)
            return completed(profileId, write, keys, RelayClient.Rejected(0, e.message ?: "Signing failed"), now)
        }
        if (write.cosmetic != null && !noteBeforePublishing(profileId, write.relay, keys))
            return completed(profileId, write, keys, java.io.IOException("Can't save the config"), now)

        track(profileId, write, keys, RelayClient(write.relay).publish(record))
    }

    /** Notes [relay] until the key expires, before publishing to it; the relay may store a record whose response is lost. */
    private fun noteBeforePublishing(profileId: UUID, relay: String, keys: ProfileKeyPair): Boolean {
        val notes = CosmeticSettings.data.sync.published
        val note = notes.firstOrNull { it.isFor(profileId, relay) }
        val expiresAt = maxOf(keys.publicKey().data().expiresAt(), note?.expiresAt ?: Instant.EPOCH)
        if (note?.expiresAt == expiresAt) return true
        if (CosmeticSettings.updatePublished(notes.filterNot { it.isFor(profileId, relay) } + Published(profileId, relay, expiresAt))) return true
        LOGGER.warn("Not sharing the cosmetic, since the config can't be saved to note where it is shared")
        return false
    }

    /** Removes notes that are no longer needed, retrying with a delay if the config can't be saved. */
    private fun tidyNotes(profileId: UUID, now: Instant) {
        if (now.isBefore(nextNoteWrite)) return
        val notes = CosmeticSettings.data.sync.published
        val keep = notesToKeep(profileId, notes, now)
        if (keep.size == notes.size) return
        if (CosmeticSettings.updatePublished(keep)) {
            noteWriteRetry = MIN_RETRY
        } else {
            nextNoteWrite = now + noteWriteRetry
            noteWriteRetry = (noteWriteRetry * 2).coerceAtMost(MAX_RETRY)
        }
    }

    /** The failure of a completed future, without the wrapping [CompletionException]; `null` if it succeeded. */
    private fun CompletableFuture<*>.errorOrNull(): Throwable? = runCatching { join() }.exceptionOrNull()
        ?.let { if (it is CompletionException) it.cause ?: it else it }

    private operator fun Duration.times(factor: Int): Duration = multipliedBy(factor.toLong())
}
