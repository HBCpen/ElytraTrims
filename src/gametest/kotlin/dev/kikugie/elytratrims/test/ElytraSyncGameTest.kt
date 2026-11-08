@file:Suppress("UnstableApiUsage")

package dev.kikugie.elytratrims.test

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings.SyncMode
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetics
import dev.kikugie.elytratrims.cosmetic.sync.CosmeticSync
import dev.kikugie.elytratrims.cosmetic.sync.SignedCosmetic
import dev.kikugie.elytratrims.vanilla
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.util.SignatureValidator
import net.minecraft.util.Signer
import net.minecraft.world.entity.decoration.Mannequin
import net.minecraft.world.entity.player.ProfileKeyPair
import net.minecraft.world.entity.player.ProfilePublicKey
import net.minecraft.world.item.DyeColor
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlin.io.path.writeText

/**
 * Tests for syncing cosmetics between players: signing and verifying records, and rendering a verified cosmetic on
 * another avatar. A mannequin stands in for the other player, since a client test has no second player.
 */
object ElytraSyncGameTest : FabricClientGameTest {
    private val PROFILE_ID: UUID = UUID.fromString("6ab43178-89fd-4905-97f6-0f67d9d76fd9")
    private val COSMETIC = ElytraCosmetic(
        trim = ElytraCosmetic.Trim(vanilla("wild"), vanilla("gold")),
        color = 0xCC3333,
        patterns = listOf(ElytraCosmetic.PatternLayer(vanilla("stripe_center"), DyeColor.WHITE)),
        glow = true,
    )

    override fun runTest(context: ClientGameTestContext) {
        testSignatures()
        context.runInWorld { testRemoteRendering(context) }
    }

    private fun testSignatures() {
        // Stand-ins for Mojang's certificate key and a player's chat key, certified like Mojang does it.
        val mojang = rsa()
        val services = SignatureValidator.from(mojang.public, "SHA1withRSA")
        val keys = certifiedKeys(PROFILE_ID, mojang, Instant.now() + Duration.ofHours(48))

        val signed = SignedCosmetic.sign(PROFILE_ID, COSMETIC, keys)
        val received = SignedCosmetic.fromJson(signed.toJson()) ?: error("Record didn't survive a JSON round trip")
        check(received.verify(PROFILE_ID, services) == COSMETIC) { "Verified cosmetic differs" }
        check(SignedCosmetic.sign(PROFILE_ID, null, keys).verify(PROFILE_ID, services) == null) { "A withdrawn cosmetic must verify as none" }

        expectInvalid("another player's record") { received.verify(UUID.randomUUID(), services) }
        expectInvalid("a changed cosmetic") {
            SignedCosmetic(received.profileId, received.issuedAt, received.cosmetic.replace("gold", "iron"), received.key, received.signature)
                .verify(PROFILE_ID, services)
        }
        expectInvalid("a later issue time") {
            SignedCosmetic(received.profileId, received.issuedAt.plusMillis(1), received.cosmetic, received.key, received.signature)
                .verify(PROFILE_ID, services)
        }
        expectInvalid("a key Mojang didn't certify") { received.verify(PROFILE_ID, SignatureValidator.from(rsa().public, "SHA1withRSA")) }
        expectInvalid("a key certified for someone else") {
            SignedCosmetic.sign(PROFILE_ID, COSMETIC, certifiedKeys(UUID.randomUUID(), mojang, Instant.now() + Duration.ofHours(48)))
                .verify(PROFILE_ID, services)
        }
        expectInvalid("an expired key") {
            SignedCosmetic.sign(PROFILE_ID, COSMETIC, certifiedKeys(PROFILE_ID, mojang, Instant.now() - Duration.ofMinutes(1)))
                .verify(PROFILE_ID, services)
        }
        expectInvalid("a record from the future") {
            SignedCosmetic.sign(PROFILE_ID, COSMETIC, keys, Instant.now() + Duration.ofHours(1)).verify(PROFILE_ID, services)
        }
        expectInvalid("an invalid cosmetic") {
            // Correctly signed, but more layers than the mod allows.
            val layer = """{"pattern":"minecraft:stripe_center","color":"white"}"""
            val json = """{"patterns":[${List(ElytraCosmetic.MAX_PATTERN_LAYERS + 1) { layer }.joinToString(",")}]}"""
            val issued = Instant.ofEpochMilli(Instant.now().toEpochMilli())
            val signature = Signer.from(keys.privateKey(), "SHA256withRSA").sign(SignedCosmetic.signedPayload(PROFILE_ID, issued, json))
            SignedCosmetic(PROFILE_ID, issued, json, keys.publicKey().data(), signature).verify(PROFILE_ID, services)
        }

        // Lets the relay tests check a record signed by this code: `ET_SYNC_SAMPLE=<path> npm test` in relay/.
        val sample = JsonObject().apply {
            addProperty("mojang_public_key", Base64.getEncoder().encodeToString(mojang.public.encoded))
            add("record", JsonParser.parseString(signed.toJson()))
        }
        val path = FabricLoader.getInstance().gameDir.resolve("elytratrims_sync_sample.json")
        path.writeText(GsonBuilder().setPrettyPrinting().create().toJson(sample))
        println("Wrote a signed sample record to ${path.toAbsolutePath()}")
    }

    private fun expectInvalid(case: String, action: () -> Unit) {
        try {
            action()
        } catch (_: SignedCosmetic.InvalidException) {
            return
        }
        error("Accepted $case")
    }

    private fun rsa(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    /** A key pair with a certificate in the format of [ProfilePublicKey.Data], signed by [mojang]. */
    private fun certifiedKeys(profileId: UUID, mojang: KeyPair, expiresAt: Instant): ProfileKeyPair {
        val player = rsa()
        val expiry = Instant.ofEpochMilli(expiresAt.toEpochMilli())
        val encoded = player.public.encoded
        val payload = ByteBuffer.allocate(24 + encoded.size)
            .putLong(profileId.mostSignificantBits).putLong(profileId.leastSignificantBits)
            .putLong(expiry.toEpochMilli()).put(encoded).array()
        val certificate = Signer.from(mojang.private, "SHA1withRSA").sign(payload)
        return ProfileKeyPair(player.private, ProfilePublicKey(ProfilePublicKey.Data(expiry, player.public, certificate)), expiry)
    }

    /** A verified cosmetic of someone else is drawn on their avatar, unless syncing is turned off. */
    private fun TestSingleplayerContext.testRemoteRendering(context: ClientGameTestContext) {
        server.runCommand("summon minecraft:mannequin ~ ~ ~3 {Rotation:[0f,0f]}")
        server.runCommand("summon minecraft:mannequin ~2 ~ ~3 {Rotation:[0f,0f]}")
        server.runCommand("item replace entity @e[type=minecraft:mannequin] armor.chest with minecraft:elytra")
        context.waitTicks(10)

        val (withCosmetic, without) = context.computeOnClient<Pair<UUID, UUID>, Throwable> { client ->
            val mannequins = client.level!!.entitiesForRendering().filterIsInstance<Mannequin>().sortedBy { it.x }
            check(mannequins.size == 2) { "Expected two mannequins, found ${mannequins.size}" }
            CosmeticSettings.update(CosmeticSettings.Data(sync = CosmeticSettings.Sync(SyncMode.SHARE, "")))
            CosmeticSync.putVerified(mannequins[0].uuid, COSMETIC)
            client.options.cameraType = CameraType.FIRST_PERSON
            client.player!!.yRot = 0F
            client.player!!.xRot = 10F
            mannequins[0].uuid to mannequins[1].uuid
        }
        context.waitTicks(10)
        context.runOnClient<Throwable> { client ->
            check(cosmeticOf(client, withCosmetic) == COSMETIC) { "The synced cosmetic is not attached" }
            check(cosmeticOf(client, without) == null) { "Avatars without a synced cosmetic must render as-is" }
        }
        context.takeScreenshot("sync_remote")

        context.runOnClient<Throwable> { client ->
            CosmeticSettings.update(CosmeticSettings.Data(sync = CosmeticSettings.Sync(SyncMode.OFF, "")))
            check(cosmeticOf(client, withCosmetic) == null) { "Synced cosmetics must be hidden when syncing is off" }
            CosmeticSettings.update(CosmeticSettings.Data())
        }
    }

    private fun cosmeticOf(client: Minecraft, id: UUID): ElytraCosmetic? {
        val entity = client.level!!.entitiesForRendering().first { it.uuid == id }
        return ElytraCosmetics.forEntity(entity)
    }
}
