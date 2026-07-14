package dev.kikugie.elytratrims.render

import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.Memoizer
import dev.kikugie.elytratrims.memoize
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer
import net.minecraft.client.renderer.rendertype.LayeringTransform
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object ElytraRenderTypes {
    private val COSMIC: Memoizer<Identifier, RenderType> = memoize {
        val builder = RenderSetup.builder(ElytraRenderPipelines.COSMIC)
            .withTexture("Sampler0", TheEndPortalRenderer.END_SKY_LOCATION)
            .withTexture("Sampler1", TheEndPortalRenderer.END_PORTAL_LOCATION)
            .withTexture("Sampler2", it)
            .createRenderSetup()
        RenderType.create("elytra_gateway", builder)
    }

    private val ELYTRA_TRANSLUCENT: Memoizer<Identifier, RenderType> = memoize {
        val builder = RenderSetup.builder(ElytraRenderPipelines.ELYTRA_TRANSLUCENT)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .withTexture("Sampler0", it)
            .affectsCrumbling()
            .sortOnUpload()
            .useLightmap()
            .useOverlay()
            .createRenderSetup()
        RenderType.create("elytra_gateway", builder)
    }

    @JvmStatic fun cosmic(texture: Identifier): RenderType =
        COSMIC(texture)

    @JvmStatic fun elytraTranslucent(texture: Identifier): RenderType =
        ELYTRA_TRANSLUCENT(texture)
}