package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.*
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.TrimAccess.Companion.trim
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.render.ElytraRenderTypes
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.render.glowingLightCoords
import dev.kikugie.elytratrims.render.isMissing
import dev.kikugie.elytratrims.render.prepare
import dev.kikugie.elytratrims.render.render
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.EquipmentClientInfo
import net.minecraft.resources.Identifier
import net.minecraft.world.item.equipment.EquipmentAssets
import net.minecraft.world.item.equipment.trim.ArmorTrim
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ETTrimsRenderer : ETDecorator {
    @JvmField val ID: ETRendererID = ETRendererID(elytratrims("trims"), ETRenderMethod.POST)

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val reported: MutableSet<Identifier> = mutableSetOf()

    //? if <26.3 {
    /*private val lookup: Memoizer<ArmorTrim, TextureAtlasSprite> = memoize {
        ElytraTrimsAtlas.ATLAS_ID.getSprite(it.layerAssetId(EquipmentClientInfo.LayerType.WINGS.trimAssetPrefix(), EquipmentAssets.ELYTRA), ::report)
    }
    *///?}

    override fun getType(): ETRendererID = ID

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean {
        //? if >=26.3 {
        val handle = parameters.trimTextureHandle ?: return false
        val prm = parameters.clone()
            .apply { sprite = handle; lightCoords = glowingLightCoords; renderType = ElytraRenderTypes.elytraTranslucent(handle.textureLocation()) }
            .prepare(ID)

        if (prm.sprite.let { it is net.minecraft.client.resources.palette.PalettedTextureManager.Handle && it.textureLocation().isMissing }
            || prm.sprite.let { it is TextureAtlasSprite && it.isMissing })
            return parameters.stack has ETItemFlag.DEBUG
        //?} else {
        /*val trim = parameters.stack.trim.get() ?: return false
        val prm = parameters.clone()
            .apply { sprite = lookup(trim); lightCoords = glowingLightCoords; renderType = ElytraTrimsAtlas.elytraDecorations() }
            .prepare(ID)

        if (prm.sprite.isMissing) return parameters.stack has ETItemFlag.DEBUG
        *///?}
        
        return prm.render(ID, collector)
    }

    private fun report(id: Identifier) {
        if (reported.add(id)) logger.warn("Missing trim texture: $id")
    }
}