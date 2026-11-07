package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.ColorAccess.Companion.color
import dev.kikugie.elytratrims.render.ElytraRenderTypes
import dev.kikugie.elytratrims.render.glowingLightCoords
import dev.kikugie.elytratrims.render.prepare
import dev.kikugie.elytratrims.render.render
import dev.kikugie.elytratrims.resource.image.isVisible
import dev.kikugie.elytratrims.resource.image.toUColor4i
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack

object ETColorRenderer : ETDecorator {
    @JvmField val ID: ETRendererID = ETRendererID(elytratrims("color"), ETRenderMethod.POST)

    override fun getType(): ETRendererID = ID

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean {
        val prm = parameters.clone()
            .apply { renderType = stack.getRenderType(); tintedColor = stack.determineColor(); lightCoords = glowingLightCoords }
            .prepare(ID)
        return if (!prm.tintedColor.toUColor4i().isVisible) false
        else prm.render(ID, collector)
    }

    private fun ItemStack.determineColor(): Int = when {
        displayName.string.trim('[', ']') == "jeb_" -> {
            val time = Minecraft.getInstance().level?.gameTime ?: 0
            Mth.hsvToArgb(time % 360L / 360F, 1F, 1F, 0xFF)
        }

        else -> color.get().let { if (it.isVisible) it.packed else 0 }
    }

    private fun ItemStack.getRenderType(): RenderType = BuiltInRegistries.ITEM.getKey(item)
        .withPath { "textures/entity/equipment/wings/${it}_overlay.png" }
        .let(ElytraRenderTypes::elytraTranslucent)
}