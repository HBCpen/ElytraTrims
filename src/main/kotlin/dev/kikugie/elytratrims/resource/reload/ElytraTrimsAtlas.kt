package dev.kikugie.elytratrims.resource.reload

import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.elytratrims
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.AtlasManager

object ElytraTrimsAtlas {
    const val ATLAS_NAME = "decorations"
    @JvmField val ATLAS_ID: Identifier = elytratrims(ATLAS_NAME)
    @JvmField val ATLAS_SHEET: Identifier = elytratrims("textures/atlas/$ATLAS_NAME.png")

    @JvmField val ELYTRA_TRIMS_RENDER: RenderType = RenderType.armorCutoutNoCull(ATLAS_SHEET)
    @JvmField val ELYTRA_TRIMS_RENDER_DECAL: RenderType = RenderType.createArmorDecalCutoutNoCull(ATLAS_SHEET)

    @JvmStatic fun createAtlasConfig(): AtlasManager.AtlasConfig =
        AtlasManager.AtlasConfig(ATLAS_SHEET, ATLAS_ID, false)

    @JvmStatic fun elytraTrimsSheet(decal: Boolean): RenderType =
        if (decal) ELYTRA_TRIMS_RENDER_DECAL else ELYTRA_TRIMS_RENDER
}