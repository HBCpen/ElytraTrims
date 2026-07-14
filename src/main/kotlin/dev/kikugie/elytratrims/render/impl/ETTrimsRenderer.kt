package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.*
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.render.ElytraRenderTypes
import dev.kikugie.elytratrims.render.glowingLightCoords
import dev.kikugie.elytratrims.render.isMissing
import dev.kikugie.elytratrims.render.prepare
import dev.kikugie.elytratrims.render.render
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ETTrimsRenderer : ETDecorator {
    @JvmField val ID: ETRendererID = ETRendererID(elytratrims("trims"), ETRenderMethod.POST)

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val reported: MutableSet<Identifier> = mutableSetOf()

    //? if <26.3 {
    /*private val lookup: Memoizer<ArmorTrim, TextureAtlasSprite> = memoize {
        ElytraTrimsAtlas.ATLAS_ID.getSprite(it.layerAssetId(WINGS.trimAssetPrefix(), EquipmentAssets.ELYTRA), ::report)
    }
    *///?}

    override fun getType(): ETRendererID = ETPatternsRenderer.ID

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean {
        val handle = parameters.trimTextureHandle ?: return false
        if (handle.textureLocation().isMissing) return parameters.stack has ETItemFlag.DEBUG

        val prm = parameters.clone()
            .apply { sprite = handle; lightCoords = glowingLightCoords; renderType = ElytraRenderTypes.elytraTranslucent(handle.textureLocation()) }
            .prepare(ID)
        return prm.render(ID, collector)
    }

    private fun report(id: Identifier) {
        if (reported.add(id)) logger.warn("Missing trim texture: $id")
    }
}