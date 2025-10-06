package dev.kikugie.elytratrims.render

import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.MINECRAFT
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@JvmField val RENDER_LOGGER: Logger = LoggerFactory.getLogger("ETRenderer")

val TextureAtlasSprite.isMissing
    get() = contents().name() == MissingTextureAtlasSprite.getLocation()

fun Identifier.getSprite(identifier: Identifier, reporter: (Identifier) -> Unit): TextureAtlasSprite? =
    MINECRAFT.atlasManager.getAtlasOrThrow(this).getSprite(identifier).checkMissing(identifier, reporter)

fun TextureAtlasSprite.checkMissing(identifier: Identifier, reporter: (Identifier) -> Unit): TextureAtlasSprite? {
    if (!isMissing) return this
    reporter(identifier)
    return null
}