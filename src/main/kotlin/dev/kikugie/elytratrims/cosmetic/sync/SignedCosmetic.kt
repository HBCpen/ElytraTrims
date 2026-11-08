package dev.kikugie.elytratrims.cosmetic.sync

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import net.minecraft.core.UUIDUtil
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.SignatureValidator
import net.minecraft.util.Signer
import net.minecraft.world.entity.player.ProfileKeyPair
import net.minecraft.world.entity.player.ProfilePublicKey
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A player's cosmetic as exchanged through the relay, signed with the player's chat signing key.
 *
 * The key comes with a certificate from Mojang that binds it to the player's UUID (the same one vanilla uses to verify
 * chat), so anyone can check that a record was published by that player, without trusting the relay or the server.
 *
 * @property cosmetic the cosmetic as compact JSON exactly as signed, or an empty string when the player shows none.
 */
class SignedCosmetic(
    val profileId: UUID,
    val issuedAt: Instant,
    val cosmetic: String,
    val key: ProfilePublicKey.Data,
    val signature: ByteArray,
) {
    /** Reason a record was rejected by [verify]. */
    class InvalidException(message: String) : Exception(message)

    /**
     * Checks that the record belongs to [expected] and was signed with a valid key of that player,
     * and returns its cosmetic, or `null` when the player shows none.
     *
     * @param services validator for the certificates Mojang issues to profile keys
     */
    @Throws(InvalidException::class)
    fun verify(expected: UUID, services: SignatureValidator, now: Instant = Instant.now()): ElytraCosmetic? {
        if (profileId != expected) throw InvalidException("Record of $profileId was returned for $expected")
        if (key.hasExpired()) throw InvalidException("Expired profile key")
        if (issuedAt.isAfter(now + MAX_CLOCK_SKEW)) throw InvalidException("Issued in the future")
        try {
            ProfilePublicKey.createValidated(services, profileId, key)
        } catch (_: ProfilePublicKey.ValidationException) {
            throw InvalidException("Profile key is not certified for $profileId")
        }
        if (!ProfilePublicKey(key).createSignatureValidator().validate(signedPayload(profileId, issuedAt, cosmetic), signature))
            throw InvalidException("Invalid signature")
        return parseCosmetic(cosmetic)
    }

    fun toJson(): String = GSON.toJson(CODEC.encodeStart(JsonOps.INSTANCE, this).orThrow)

    companion object {
        const val VERSION = 1

        /** Upper bound for the signed cosmetic JSON. 16 pattern layers with long modded identifiers stay well below it. */
        const val MAX_COSMETIC_LENGTH = 4096

        private val MAX_CLOCK_SKEW: Duration = Duration.ofMinutes(10)

        /** Prefix of the signed bytes, so these signatures can never be mistaken for chat message signatures. */
        private val DOMAIN: ByteArray = "elytratrims_cosmetic/v1\n".toByteArray(Charsets.UTF_8)

        private val GSON = GsonBuilder().disableHtmlEscaping().create()

        val CODEC: Codec<SignedCosmetic> = RecordCodecBuilder.create { it.group(
            Codec.intRange(VERSION, VERSION).fieldOf("version").forGetter { VERSION },
            UUIDUtil.STRING_CODEC.fieldOf("profile_id").forGetter(SignedCosmetic::profileId),
            Codec.LONG.fieldOf("issued_at").forGetter { c -> c.issuedAt.toEpochMilli() },
            Codec.string(0, MAX_COSMETIC_LENGTH).fieldOf("cosmetic").forGetter(SignedCosmetic::cosmetic),
            ProfilePublicKey.Data.CODEC.fieldOf("key").forGetter(SignedCosmetic::key),
            ExtraCodecs.BASE64_STRING.fieldOf("signature").forGetter(SignedCosmetic::signature),
        ).apply(it) { _, id, issued, cosmetic, key, signature -> SignedCosmetic(id, Instant.ofEpochMilli(issued), cosmetic, key, signature) } }

        /** Signs [cosmetic] (`null` for none) as the owner of [keys]. */
        fun sign(profileId: UUID, cosmetic: ElytraCosmetic?, keys: ProfileKeyPair, now: Instant = Instant.now()): SignedCosmetic {
            val json = cosmetic?.takeUnless { it.isEmpty }?.let(::encodeCosmetic).orEmpty()
            require(json.length <= MAX_COSMETIC_LENGTH) { "Cosmetic is too large to share" }
            // The relay keeps the newest record, which needs millisecond precision only.
            val issued = Instant.ofEpochMilli(now.toEpochMilli())
            val signature = Signer.from(keys.privateKey(), "SHA256withRSA").sign(signedPayload(profileId, issued, json))
            return SignedCosmetic(profileId, issued, json, keys.publicKey().data(), signature)
        }

        fun fromJson(json: JsonElement): SignedCosmetic? = CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null)

        fun fromJson(text: String): SignedCosmetic? = runCatching { fromJson(JsonParser.parseString(text)) }.getOrNull()

        /** `domain || profile id || issued at (ms, big-endian) || cosmetic (UTF-8)`; mirrored by the relay. */
        fun signedPayload(profileId: UUID, issuedAt: Instant, cosmetic: String): ByteArray {
            val body = cosmetic.toByteArray(Charsets.UTF_8)
            return ByteBuffer.allocate(DOMAIN.size + 24 + body.size)
                .put(DOMAIN)
                .putLong(profileId.mostSignificantBits).putLong(profileId.leastSignificantBits)
                .putLong(issuedAt.toEpochMilli())
                .put(body)
                .array()
        }

        private fun encodeCosmetic(cosmetic: ElytraCosmetic): String =
            GSON.toJson(ElytraCosmetic.CODEC.encodeStart(JsonOps.INSTANCE, cosmetic).orThrow)

        private fun parseCosmetic(json: String): ElytraCosmetic? {
            if (json.isEmpty()) return null
            val element = runCatching { JsonParser.parseString(json) }.getOrElse { throw InvalidException("Malformed cosmetic") }
            return ElytraCosmetic.CODEC.parse(JsonOps.INSTANCE, element).result().orElseThrow { InvalidException("Invalid cosmetic") }
                .takeUnless { it.isEmpty }
        }
    }
}
