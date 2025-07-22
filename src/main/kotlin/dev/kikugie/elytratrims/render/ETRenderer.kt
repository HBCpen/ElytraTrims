package dev.kikugie.elytratrims.render

import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.api.ETRenderParameters
import dev.kikugie.elytratrims.item.ETFlag
import dev.kikugie.elytratrims.item.flags
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val RENDER_LOGGER: Logger = LoggerFactory.getLogger("ETRenderer")

val TextureAtlasSprite.isMissing
    get() = contents().name() == MissingTextureAtlasSprite.getLocation()

fun ETRenderParameters.getEffectiveLight() =
    if (stack.flags[ETFlag.GLOW]) 0xFF00FF else light

fun ETRenderParameters.renderToBuffer() =
    elytra.renderToBuffer(matrices, converter.apply(this), light, OverlayTexture.NO_OVERLAY, color)

fun Identifier.getSprite(identifier: Identifier, reporter: (Identifier) -> Unit): TextureAtlasSprite? =
    Minecraft.getInstance().getTextureAtlas(this).apply(identifier).checkMissing(identifier, reporter)

fun TextureAtlasSprite.checkMissing(identifier: Identifier, reporter: (Identifier) -> Unit): TextureAtlasSprite? {
    if (!isMissing) return this
    reporter(identifier)
    return null
}