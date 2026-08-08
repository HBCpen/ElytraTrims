package dev.kikugie.elytratrims.resource.reload

import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.render.ElytraRenderTypes
import dev.kikugie.elytratrims.vanilla
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.sprite.AtlasManager
import net.minecraft.client.resources.model.sprite.SpriteId

object ElytraTrimsAtlas {
    const val ATLAS_NAME: String = "decorations"
    @JvmField val ATLAS_ID: Identifier = elytratrims(ATLAS_NAME)
    @JvmField val ATLAS_SHEET: Identifier = elytratrims("textures/atlas/$ATLAS_NAME.png")

    @JvmStatic fun createAtlasConfig(): AtlasManager.AtlasConfig =
        AtlasManager.AtlasConfig(ATLAS_SHEET, ATLAS_ID, false)

    @JvmStatic fun elytraDecorations(): RenderType =
        ElytraRenderTypes.elytraTranslucent(ATLAS_SHEET)

    @JvmStatic fun removeDuplicateSprites(sprites: MutableMap<SpriteId, TextureAtlasSprite>) {
        val location: Identifier = vanilla("textures/atlas/armor_trims.png")
        val removing: MutableSet<SpriteId> = HashSet()
        sprites.keys.filterTo(removing) {
            it.atlasLocation == location && it.texture.path.startsWith("trims/entity/wings/")
        }

        sprites -= removing
    }
}