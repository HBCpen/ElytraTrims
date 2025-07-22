package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.api.impl.getEffectiveLight
import dev.kikugie.elytratrims.api.impl.renderToBuffer
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.ifTrue
import dev.kikugie.elytratrims.item.color
import dev.kikugie.elytratrims.resource.image.Color4i
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.Mth
import net.minecraft.world.item.Item

object ETColorRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("color"), ETRenderMethod.POST)

    override fun getType(): ETRendererID = type

    override fun prepare(parameters: ETRenderParameters): ETRenderParameters = with(parameters) {
        val custom = when {
            stack.displayName.tryCollapseToString() == "jeb_" ->
                Color4i(Mth.hsvToRgb((Minecraft.getInstance().level?.gameTime ?: 0) % 360 / 360F, 1F, 1F))

            stack.color.value.isVisible() ->
                stack.color.value

            else ->
                return@with parameters.copy(color = 0)
        }
        parameters.copy(color = Color4i(color).blend(custom).value, light = getEffectiveLight()) {
            ItemRenderer.getArmorFoilBuffer(it.source, RenderType.armorCutoutNoCull(elytraSpriteFor(it.stack.item)), it.stack.hasFoil())
        }
    }

    override fun render(parameters: ETRenderParameters): Boolean = with(parameters) {
        (color != 0).ifTrue { renderToBuffer() }
    }

    override fun reset() {
        // TODO: Report missing sprites
    }

    private fun elytraSpriteFor(item: Item): Identifier = BuiltInRegistries.ITEM.getKey(item)
        .withPath { "textures/entity/equipment/wings/${it}_overlay.png" }
}