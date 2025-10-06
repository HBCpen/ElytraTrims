package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.*
import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.api.impl.getEffectiveLight
import dev.kikugie.elytratrims.api.impl.submitToCollector
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.item.TrimAccess.Companion.trim
import dev.kikugie.elytratrims.item.invoke
import dev.kikugie.elytratrims.render.RENDER_LOGGER
import dev.kikugie.elytratrims.render.getSprite
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.EquipmentClientInfo
import net.minecraft.world.item.equipment.EquipmentAssets
import net.minecraft.world.item.equipment.trim.ArmorTrim

object ETTrimsRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("trims"), ETRenderMethod.POST)
    override fun getType(): ETRendererID = type

    private val reported = mutableSetOf<Identifier>()
    private val armorTrims: Memoizer<ArmorTrim, TextureAtlasSprite?> = memoize {
        Sheets.ARMOR_TRIMS_SHEET.getSprite(texture(it), ::report)
    }

    override fun prepare(parameters: ETRenderParameters): ETRenderParameters = with(parameters) {
        val trim = stack.trim()?.let(armorTrims)
        if (trim == null) parameters else copy(texture = Sheets.ARMOR_TRIMS_SHEET, sprite = trim, light = getEffectiveLight(), render = RenderType.armorCutoutNoCull(texture))
    }

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean = with(parameters) {
        runIf(stack.trim()?.let(armorTrims) != null) { submitToCollector(collector) }
    }

    override fun reset() {
        reported.clear()
        armorTrims.clear()
    }

    private fun texture(trim: ArmorTrim) = trim.layerAssetId(
        EquipmentClientInfo.LayerType.WINGS.trimAssetPrefix(),
        EquipmentAssets.ELYTRA
    )

    private fun report(id: Identifier) {
        if (reported.add(id)) RENDER_LOGGER.warn("Missing trim texture: $id")
    }
}