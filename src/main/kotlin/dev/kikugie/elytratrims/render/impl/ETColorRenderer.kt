package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.item.ColorAccess.Companion.color
import dev.kikugie.elytratrims.render.api.ETDecorator
import dev.kikugie.elytratrims.render.api.ETParameters
import dev.kikugie.elytratrims.render.effectiveLightCoords
import dev.kikugie.elytratrims.render.submitModel
import dev.kikugie.elytratrims.resource.image.Color4i
import dev.kikugie.elytratrims.resource.image.isVisible
import dev.kikugie.elytratrims.resource.image.toUColor4i
import dev.kikugie.elytratrims.resource.image.withAlpha
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import java.util.concurrent.atomic.AtomicInteger

object ETColorRenderer : ETDecorator {
    override fun ETParameters.render(collector: SubmitNodeCollector, i: AtomicInteger): Boolean {
        val custom = stack.determineColor(color).apply { if (!isVisible) return false }
        collector.submitModel(i, renderType = stack.getRenderType(), lightCoords = effectiveLightCoords, color = custom)
        return true
    }

    private fun ItemStack.determineColor(base: Color4i): Color4i = when {
        displayName.string.trim('[', ']') == "jeb_" -> {
            val time = Minecraft.getInstance().level?.gameTime ?: 0
            Mth.hsvToArgb(time % 360L / 360F, 1F, 1F, base.a).toUColor4i()
        }

        else -> color.get().let {
            if (it.isVisible) it.withAlpha(base.a)
            else Color4i.TRANSPARENT
        }
    }

    private fun ItemStack.getRenderType(): RenderType = BuiltInRegistries.ITEM.getKey(item)
        .withPath { "textures/entity/equipment/wings/${it}_overlay.png" }
        .let(RenderTypes::armorTranslucent)
}