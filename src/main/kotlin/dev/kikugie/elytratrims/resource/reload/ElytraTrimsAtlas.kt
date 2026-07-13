package dev.kikugie.elytratrims.resource.reload

import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.elytratrims
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes

object ElytraTrimsAtlas {
    const val ATLAS_NAME = "decorations"
    @JvmField val ATLAS_ID: Identifier = elytratrims(ATLAS_NAME)
    @JvmField val ATLAS_SHEET: Identifier = elytratrims("textures/atlas/$ATLAS_NAME.png")

    //? if >=1.21.9 {
    @JvmStatic fun createAtlasConfig(): net.minecraft.client.resources.model.sprite.AtlasManager.AtlasConfig =
        net.minecraft.client.resources.model.sprite.AtlasManager.AtlasConfig(ATLAS_SHEET, ATLAS_ID, false)
    //?}

    @JvmStatic fun elytraTrimsSheet(): RenderType =
        //~ if >=26.3 armorTranslucent -> armorCutoutNoCull
        RenderTypes.armorCutoutNoCull(ATLAS_SHEET)
}