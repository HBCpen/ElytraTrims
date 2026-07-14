package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.hasNot
import dev.kikugie.elytratrims.render.ElytraRenderTypes
import dev.kikugie.elytratrims.render.api.ETDecorator
import dev.kikugie.elytratrims.render.api.ETParameters
import dev.kikugie.elytratrims.render.submitModel
import net.minecraft.client.renderer.SubmitNodeCollector
import java.util.concurrent.atomic.AtomicInteger

object ETGatewayRenderer : ETDecorator {
    override fun render(parameters: ETParameters, collector: SubmitNodeCollector, i: AtomicInteger): Boolean = with(parameters) {
        if (stack hasNot ETItemFlag.GATEWAY) return false
        collector.submitModel(i, renderType = ElytraRenderTypes.cosmic(texture))
        return true
    }
}