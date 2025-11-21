package dev.kikugie.elytratrims.resource.reload

import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.elytratrims
import net.minecraft.client.renderer.RenderType

object ElytraTrimsAtlas {
    const val ATLAS_NAME = "decorations"
    @JvmField val ATLAS_ID: Identifier = elytratrims(ATLAS_NAME)
    @JvmField val ATLAS_SHEET: Identifier = elytratrims("textures/atlas/$ATLAS_NAME.png")

    @JvmField val ELYTRA_TRIMS_RENDER: RenderType = RenderType.armorCutoutNoCull(ATLAS_SHEET)
    @JvmField val ELYTRA_TRIMS_RENDER_DECAL: RenderType = RenderType.createArmorDecalCutoutNoCull(ATLAS_SHEET)

    //? if >=1.21.9 {
    @JvmStatic fun createAtlasConfig(): net.minecraft.client.resources.model.AtlasManager.AtlasConfig =
        net.minecraft.client.resources.model.AtlasManager.AtlasConfig(ATLAS_SHEET, ATLAS_ID, false)
    //?}

    @JvmStatic fun elytraTrimsSheet(decal: Boolean): RenderType =
        if (decal) ELYTRA_TRIMS_RENDER_DECAL else ELYTRA_TRIMS_RENDER
}