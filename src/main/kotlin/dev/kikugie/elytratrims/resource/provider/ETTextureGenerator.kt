package dev.kikugie.elytratrims.resource.provider

import com.mojang.blaze3d.platform.NativeImage
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.resource.image.Color4i
import dev.kikugie.elytratrims.resource.image.EditableImage
import dev.kikugie.elytratrims.resource.image.asEditable
import dev.kikugie.elytratrims.resource.image.copy
import dev.kikugie.elytratrims.resource.image.mask
import dev.kikugie.elytratrims.resource.image.outline
import dev.kikugie.elytratrims.resource.image.saturate
import dev.kikugie.elytratrims.resource.pack.ETRuntimePack.Companion.LOGGER
import dev.kikugie.elytratrims.resource.pack.InputSupplier
import dev.kikugie.elytratrims.resource.pack.PackIdentifier
import dev.kikugie.elytratrims.then
import dev.kikugie.elytratrims.vanilla
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.channels.Channels
import kotlin.jvm.optionals.getOrNull

object ETTextureGenerator : ETResourceProvider {
    private val overlays = listOf("entity/equipment/wings/elytra", "item/elytra", "item/elytra_broken")
    private val outlines = listOf("elytra", "nether_star", "paper", "leather", "glow_ink_sac", "golden_apple", "ominous_trial_key", "trial_key")

    override fun collect(lookup: ResourceManager, to: MutableMap<PackIdentifier, InputSupplier>) {
        for (it in overlays) {
            val id = vanilla("${it}_overlay.png")
            to[texture(id)] = ImageSupplier(lookup, vanilla("$it.png")) { it.apply(EditableImage::saturate) }
        }

        for (it in outlines) {
            val id = elytratrims("gui/sprites/container/slot/$it.png")
            to[texture(id)] = ImageSupplier(lookup, vanilla("item/$it.png")) { it.outline(Color4i(0xFF555555.toInt())) }
        }

        to[texture(vanilla("trims/items/wings_broken_trim.png"))] = ImageSupplier(lookup, vanilla("trims/items/wings_trim.png")) {
            val base = load(lookup, vanilla("item/elytra_broken.png")) ?: return@ImageSupplier it
            it.apply { mask(base) }
        }

        val base by lazy { load(lookup, vanilla("entity/equipment/wings/elytra.png")) }

        for (it in ETAtlasGenerator.collectPatterns(lookup, useBanners = true)) {
            val id = it.withPath { "entity/wings/banner/${it.substringAfterLast('/')}.png" }
            to[texture(id)] = ImageSupplier(lookup, it.withSuffix(".png")) {
                applyBannerPattern(base ?: return@ImageSupplier it, it)
            }
        }

        for (it in ETAtlasGenerator.collectPatterns(lookup, useBanners = false)) {
            val id = it.withPath { "entity/wings/shield/${it.substringAfterLast('/')}.png" }
            to[texture(id)] = ImageSupplier(lookup, it.withSuffix(".png")) {
                applyShieldPattern(base ?: return@ImageSupplier it, it)
            }
        }
    }

    private fun applyBannerPattern(elytra: EditableImage, pattern: EditableImage): EditableImage = pattern.use {
        val scale = it.width / 64
        val canvas = EditableImage(it.width * scale * 2, it.width * scale)
        canvas.copyScaled(it, scale, 72, 4, 1, 1, 20, 40)
        canvas.copyScaled(it, scale, 48, 0, 1, 1, -20, -4)
        canvas.copyScaled(it, scale, 68, 0, 1, 37, -20, -4)
        canvas.copyScaled(it, scale, 44, 4, 1, 1, -4, 40)
        canvas.copyScaled(it, scale, 68, 4, 17, 1, -4, 40)
        canvas.mask(elytra)
        canvas
    }

    private fun applyShieldPattern(elytra: EditableImage, pattern: EditableImage): EditableImage = pattern.use {
        val scale = it.width / 64
        val canvas = EditableImage(it.width * scale, it.width * scale / 2)
        canvas.copyScaled(it, scale, 36, 2, 2, 2, 10, 20)
        canvas.copyScaled(it, scale, 24, 0, 2, 2, -10, -2)
        canvas.copyScaled(it, scale, 34, 0, 2, 20, -10, -2)
        canvas.copyScaled(it, scale, 34, 2, 2, 2, -2, 20)
        canvas.copyScaled(it, scale, 22, 2, 10, 2, -2, 20)
        canvas.mask(elytra)
        canvas
    }

    private fun texture(id: Identifier): PackIdentifier =
        PackIdentifier.of(PackType.CLIENT_RESOURCES, id.namespace, "textures/${id.path}")

    private fun load(lookup: ResourceManager, identifier: Identifier): EditableImage? {
        val path = identifier.withPrefix("textures/")
        val resource = lookup.getResource(path).getOrNull() ?: return null.also {
            LOGGER.warn("Resource '$path' wasn't found")
        }
        return resource.runCatching {
            NativeImage.read(open()).asEditable()
        }.getOrElse {
            LOGGER.warn("Error while reading resource '$path'", it) then null
        }
    }

    class ImageSupplier(
        private val lookup: ResourceManager,
        private val identifier: Identifier,
        private val transform: (EditableImage) -> EditableImage
    ) : InputSupplier {
        val value by lazy { load(lookup, identifier)?.let(transform).bytes() }

        override fun get(): InputStream = value.inputStream()

        private fun EditableImage?.bytes(): ByteArray {
            if (this == null) return byteArrayOf()

            val output = ByteArrayOutputStream()
            val channel = Channels.newChannel(output)
            native.writeToChannel(channel)
            return output.toByteArray()
        }
    }

}

private fun EditableImage.copyScaled(
    other: EditableImage,
    scale: Int,
    dx: Int = 0,
    dy: Int = 0,
    sx: Int = 0,
    sy: Int = 0,
    sw: Int = other.width,
    sh: Int = other.height
): Unit = copy(other, dx * scale, dy * scale, sx * scale, sy * scale, sw * scale, sh * scale)
