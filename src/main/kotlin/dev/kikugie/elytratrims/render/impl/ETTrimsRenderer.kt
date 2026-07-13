package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.*
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.TrimAccess.Companion.trim
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.render.api.ETDecorator
import dev.kikugie.elytratrims.render.api.ETParameters
import dev.kikugie.elytratrims.render.effectiveLightCoords
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.render.isMissing
import dev.kikugie.elytratrims.render.submitModel
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType.WINGS
import net.minecraft.resources.Identifier
import net.minecraft.world.item.equipment.EquipmentAssets
import net.minecraft.world.item.equipment.trim.ArmorTrim
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

object ETTrimsRenderer : ETDecorator {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val reported: MutableSet<Identifier> = mutableSetOf()
    private val lookup: Memoizer<ArmorTrim, TextureAtlasSprite> = memoize {
        ElytraTrimsAtlas.ATLAS_ID.getSprite(elytratrims("id"), ::report)
//        ElytraTrimsAtlas.ATLAS_ID.getSprite(it.layerAssetId(WINGS.trimAssetPrefix(), EquipmentAssets.ELYTRA), ::report)
    }

    override fun ETParameters.render(collector: SubmitNodeCollector, i: AtomicInteger): Boolean {
        val sprite = lookup(stack.trim.get() ?: return false)
        if (sprite.isMissing) return stack has ETItemFlag.DEBUG

        collector.submitModel(i, renderType = ElytraTrimsAtlas.elytraTrimsSheet(), lightCoords = effectiveLightCoords, sprite = sprite)
        return true
    }

    private fun report(id: Identifier) {
        if (reported.add(id)) logger.warn("Missing trim texture: $id")
    }
}