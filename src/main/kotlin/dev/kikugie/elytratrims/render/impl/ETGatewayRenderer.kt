package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.api.*
import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.ifTrue
import dev.kikugie.elytratrims.item.ETFlag
import dev.kikugie.elytratrims.item.flags
import dev.kikugie.elytratrims.render.ElytraRenderLayers
import dev.kikugie.elytratrims.render.renderToBuffer

object ETGatewayRenderer : ETDecorator {
    @JvmField val type: ETRendererType = ETRenderingAPI.registerDecorator(ETRendererType(elytratrims("cosmic"), ETRenderMethod.OVERRIDE), this)

    override fun getType(): ETRendererType = type

    override fun prepare(parameters: ETRenderParameters): ETRenderParameters = with(parameters) {
        if (!stack.flags[ETFlag.GATEWAY]) parameters
        else copy { source.getBuffer(ElytraRenderLayers.GATEWAY(it.texture)) }
    }

    override fun render(parameters: ETRenderParameters): Boolean = with(parameters) {
        stack.flags[ETFlag.GATEWAY].ifTrue { renderToBuffer() }
    }
}