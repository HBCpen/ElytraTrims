package dev.kikugie.elytratrims.resource.provider

import com.mojang.blaze3d.platform.NativeImage
import dev.kikugie.elytratrims.elytratrims
import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.resource.image.Color4i
import dev.kikugie.elytratrims.resource.pack.InputSupplier
import dev.kikugie.elytratrims.resource.pack.PackIdentifier
import dev.kikugie.elytratrims.resource.image.EditableImage
import dev.kikugie.elytratrims.resource.image.toEditable
import dev.kikugie.elytratrims.resource.pack.ETRuntimePack.Companion.LOGGER
import dev.kikugie.elytratrims.then
import dev.kikugie.elytratrims.vanilla
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
            to[texture(id)] = imageSupplier(lookup, vanilla("$it.png")) { use(EditableImage::saturated) }
        }

        for (it in outlines) {
            val id = elytratrims("gui/sprites/container/slot/$it.png")
            to[texture(id)] = imageSupplier(lookup, vanilla("item/$it.png")) { use { it.outlined(Color4i(0xFF555555)) } }
        }

        to[texture(vanilla("trims/items/wings_broken_trim.png"))] = imageSupplier(lookup, vanilla("trims/items/wings_trim.png")) {
            load(lookup, vanilla("item/elytra_broken.png"))?.use { m -> use { it.masked(m) } } ?: this
        }

        val base by lazy { load(lookup, vanilla("entity/equipment/wings/elytra.png")) }

        for (it in ETAtlasGenerator.collectPatterns(lookup, useBanners = true)) {
            val id = it.withPath { "entity/wings/banner/${it.substringAfterLast('/')}.png" }
            to[texture(id)] = imageSupplier(lookup, it.withSuffix(".png")) {
                val pattern = use { it.resized(x2 = it.dims.height * 2) }
                base?.applyPattern(pattern, isBanner = true) ?: pattern
            }
        }

        for (it in ETAtlasGenerator.collectPatterns(lookup, useBanners = false)) {
            val id = it.withPath { "entity/wings/shield/${it.substringAfterLast('/')}.png" }
            to[texture(id)] = imageSupplier(lookup, it.withSuffix(".png")) {
                val pattern = use { it.resized(y2 = it.dims.width / 2) }
                base?.applyPattern(pattern, isBanner = false) ?: pattern
            }
        }
    }

    private fun texture(id: Identifier): PackIdentifier =
        PackIdentifier.of(PackType.CLIENT_RESOURCES, id.namespace, "textures/${id.path}")

    private fun load(lookup: ResourceManager, identifier: Identifier): EditableImage? {
        val path = identifier.withPrefix("textures/")
        val resource = lookup.getResource(path).getOrNull() ?: return null.also {
            LOGGER.warn("Resource '$path' wasn't found")
        }
        return resource.runCatching {
            NativeImage.read(open()).toEditable()
        }.getOrElse {
            LOGGER.warn("Error while reading resource '$path'", it) then null
        }
    }

    private fun imageSupplier(lookup: ResourceManager, identifier: Identifier, transform: EditableImage.() -> EditableImage) = object : InputSupplier {
        val value by lazy { load(lookup, identifier)?.transform().bytes() }

        override fun get(): InputStream = value.inputStream()

        private fun EditableImage?.bytes(): ByteArray {
            if (this == null) return byteArrayOf()

            val output = ByteArrayOutputStream()
            val channel = Channels.newChannel(output)
            native.writeToChannel(channel)
            return output.toByteArray()
        }
    }

    private fun EditableImage.applyPattern(texture: EditableImage, isBanner: Boolean): EditableImage {
        val scale = texture.dims.width / 64
        val dx = (if (isBanner) 35.5F else 34F) * scale
        val dy = (if (isBanner) 1.5F else 0F) * scale
        return texture.use { it.offset(dx.toInt(), dy.toInt()) }.use { it.masked(this) }
    }
}