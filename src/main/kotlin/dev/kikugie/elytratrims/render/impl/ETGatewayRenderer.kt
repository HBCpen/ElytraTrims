package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.ifTrue
import dev.kikugie.elytratrims.render.ElytraRenderLayers
import dev.kikugie.elytratrims.api.impl.renderToBuffer
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.item.ETFlags
import dev.kikugie.elytratrims.item.ItemFlagAccessor.Companion.flags

object ETGatewayRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("cosmic"), ETRenderMethod.OVERRIDE)

    override fun getType(): ETRendererID = type

    override fun prepare(parameters: ETRenderParameters): ETRenderParameters = with(parameters) {
        if (!stack.flags[ETFlags.GATEWAY]) parameters
        else copy { source.getBuffer(ElytraRenderLayers.GATEWAY(it.texture)) }
    }

    override fun render(parameters: ETRenderParameters): Boolean = with(parameters) {
        stack.flags[ETFlags.GATEWAY].ifTrue { renderToBuffer() }
    }
}