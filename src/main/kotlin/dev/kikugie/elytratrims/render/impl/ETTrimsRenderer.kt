package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.*
import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.api.impl.getEffectiveLight
import dev.kikugie.elytratrims.api.impl.submitToCollector
import dev.kikugie.elytratrims.api.item.ETItemFlag
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.item.TrimAccess.Companion.trim
import dev.kikugie.elytratrims.item.invoke
import dev.kikugie.elytratrims.render.RENDER_LOGGER
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.render.isMissing
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.EquipmentClientInfo
import net.minecraft.resources.Identifier
import net.minecraft.world.item.equipment.EquipmentAssets
import net.minecraft.world.item.equipment.trim.ArmorTrim

object ETTrimsRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("trims"), ETRenderMethod.POST)

    private val reported = mutableSetOf<Identifier>()
    private val lookup: Memoizer<ArmorTrim, TextureAtlasSprite> = memoize {
        ElytraTrimsAtlas.ATLAS_ID.getSprite(texture(it), ::report)
    }

    override fun getType(): ETRendererID = type

    override fun prepare(parameters: ETRenderParameters): ETRenderParameters = with(parameters) {
        val trim = stack.trim() ?: return@with parameters
        copy(
            sprite = lookup(trim),
            light = getEffectiveLight(),
            render = ElytraTrimsAtlas.elytraTrimsSheet(trim.pattern.value().decal)
        )
    }

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean = with(parameters) {
        runIf(!sprite.isMissing || ETItemFlag.DEBUG[stack]) { submitToCollector(collector) }
        stack.trim() != null
    }

    override fun reset() {
        reported.clear()
        lookup.clear()
    }

    private fun texture(trim: ArmorTrim) = trim.layerAssetId(
        EquipmentClientInfo.LayerType.WINGS.trimAssetPrefix(),
        EquipmentAssets.ELYTRA
    )

    private fun report(id: Identifier) {
        if (reported.add(id)) RENDER_LOGGER.warn("Missing trim texture: $id")
    }
}