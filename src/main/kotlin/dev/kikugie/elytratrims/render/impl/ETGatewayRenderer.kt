package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.render.ElytraRenderTypes
import dev.kikugie.elytratrims.render.prepare
import dev.kikugie.elytratrims.render.submitToCollector
import net.minecraft.client.renderer.SubmitNodeCollector

object ETGatewayRenderer : ETDecorator {
    @JvmField val ID: ETRendererID = ETRendererID(elytratrims("cosmic"), ETRenderMethod.OVERRIDE)

    override fun getType(): ETRendererID = ID

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean {
        val prm = parameters.clone()
            .apply { baseTexture?.let { renderType = ElytraRenderTypes.cosmic(it) } }
            .prepare(ID)
        return prm.stack has ETItemFlag.GATEWAY && prm.submitToCollector(collector)
    }
}