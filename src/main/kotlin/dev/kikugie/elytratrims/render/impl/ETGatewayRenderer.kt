package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.api.impl.submitToCollector
import dev.kikugie.elytratrims.api.item.ETItemFlag
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.render.ElytraRenderLayers
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.runIf
import net.minecraft.client.renderer.SubmitNodeCollector

object ETGatewayRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("cosmic"), ETRenderMethod.OVERRIDE)

    override fun getType(): ETRendererID = type

    override fun prepare(parameters: ETRenderParameters): ETRenderParameters = with(parameters) {
        if (!ETItemFlag.GATEWAY[stack]) parameters else copy(
            render = ElytraRenderLayers.GATEWAY(texture)
        )
    }

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean = with(parameters) {
        runIf(ETItemFlag.GATEWAY[stack]) { submitToCollector(collector) }
    }
}