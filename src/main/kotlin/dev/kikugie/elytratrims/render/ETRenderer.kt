package dev.kikugie.elytratrims.render

import dev.kikugie.elytratrims.MINECRAFT
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@JvmField val RENDER_LOGGER: Logger = LoggerFactory.getLogger("ETRenderer")

val TextureAtlasSprite?.isMissing
    get() = this == null || contents().name() == MissingTextureAtlasSprite.getLocation()

fun Identifier.getSprite(identifier: Identifier, reporter: (Identifier) -> Unit): TextureAtlasSprite =
    MINECRAFT.atlasManager.getAtlasOrThrow(this).getSprite(identifier).also { if (it.isMissing) reporter(identifier) }
