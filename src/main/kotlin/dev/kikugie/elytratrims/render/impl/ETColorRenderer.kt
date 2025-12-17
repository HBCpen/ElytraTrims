package dev.kikugie.elytratrims.render.impl

import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.api.impl.getEffectiveLight
import dev.kikugie.elytratrims.api.impl.submitToCollector
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.ColorAccess.Companion.color
import dev.kikugie.elytratrims.item.invoke
import dev.kikugie.elytratrims.resource.image.Color4i
import dev.kikugie.elytratrims.runIf
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.Mth
import net.minecraft.world.item.Item

object ETColorRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("color"), ETRenderMethod.POST)

    override fun getType(): ETRendererID = type

    override fun prepare(parameters: ETRenderParameters): ETRenderParameters = with(parameters) {
        val custom = when {
            stack.displayName.string.trim('[', ']') == "jeb_" ->
                Color4i(Mth.hsvToRgb((Minecraft.getInstance().level?.gameTime ?: 0) % 360 / 360F, 1F, 1F))

            Color4i(stack.color()).isVisible() ->
                Color4i(stack.color())

            else ->
                return@with parameters.copy(color = 0)
        }
        parameters.copy(
            color = Color4i(color).blend(custom).value,
            light = getEffectiveLight(),
            render = RenderTypes.armorTranslucent(elytraSpriteFor(stack.item))
        )
    }

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean = with(parameters) {
        runIf(color != 0) { submitToCollector(collector) }
    }

    override fun reset() {
        // TODO: Report missing sprites
    }

    private fun elytraSpriteFor(item: Item): Identifier = BuiltInRegistries.ITEM.getKey(item)
        .withPath { "textures/entity/equipment/wings/${it}_overlay.png" }
}