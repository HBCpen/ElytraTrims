package dev.kikugie.elytratrims.resource.reload

import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.elytratrims
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes

object ElytraTrimsAtlas {
    const val ATLAS_NAME = "decorations"
    @JvmField val ATLAS_ID: Identifier = elytratrims(ATLAS_NAME)
    @JvmField val ATLAS_SHEET: Identifier = elytratrims("textures/atlas/$ATLAS_NAME.png")

    @JvmField val ELYTRA_TRIMS_RENDER: RenderType = RenderTypes.armorCutoutNoCull(ATLAS_SHEET)
    @JvmField val ELYTRA_TRIMS_RENDER_DECAL: RenderType = RenderTypes.createArmorDecalCutoutNoCull(ATLAS_SHEET)

    //? if >=1.21.9 {
    @JvmStatic fun createAtlasConfig(): net.minecraft.client.resources.model.sprite.AtlasManager.AtlasConfig =
        net.minecraft.client.resources.model.sprite.AtlasManager.AtlasConfig(ATLAS_SHEET, ATLAS_ID, false)
    //?}

    @JvmStatic fun elytraTrimsSheet(decal: Boolean): RenderType =
        if (decal) ELYTRA_TRIMS_RENDER_DECAL else ELYTRA_TRIMS_RENDER
}