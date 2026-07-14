package dev.kikugie.elytratrims.resource.reload

import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.render.ElytraRenderTypes
import net.minecraft.client.renderer.rendertype.RenderType

//? if >=1.21.9
private typealias AtlasConfig = net.minecraft.client.resources.model.sprite.AtlasManager.AtlasConfig

object ElytraTrimsAtlas {
    const val ATLAS_NAME = "decorations"
    @JvmField val ATLAS_ID: Identifier = elytratrims(ATLAS_NAME)
    @JvmField val ATLAS_SHEET: Identifier = elytratrims("textures/atlas/$ATLAS_NAME.png")

    //? if >=1.21.9 {
    @JvmStatic fun createAtlasConfig(): AtlasConfig =
        AtlasConfig(ATLAS_SHEET, ATLAS_ID, false)
    //?}

    @JvmStatic fun elytraDecorations(): RenderType =
        ElytraRenderTypes.elytraTranslucent(ATLAS_SHEET)
}