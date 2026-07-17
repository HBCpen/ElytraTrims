package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.hasNot
import dev.kikugie.elytratrims.render.glowingLightCoords
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.render.isMissing
import dev.kikugie.elytratrims.render.prepare
import dev.kikugie.elytratrims.render.render
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ETBadAppleRenderer : ETDecorator {
    @JvmField val ID: ETRendererID = ETRendererID(elytratrims("bad_apple"), ETRenderMethod.POST)

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val animation by lazy { ElytraTrimsAtlas.ATLAS_ID.getSprite(elytratrims("feature/bad_apple"), ::report) }
    private var reported: Boolean = false

    override fun getType(): ETRendererID = ID

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean {
        val prm = parameters.clone()
            .apply { lightCoords = glowingLightCoords; sprite = animation; renderType = ElytraTrimsAtlas.elytraDecorations() }
            .prepare(ID)
        return if (prm.stack hasNot ETItemFlag.BAD_APPLE
            || prm.sprite.let { it is TextureAtlasSprite && it.isMissing }
            && prm.stack hasNot ETItemFlag.DEBUG)
            false
        else prm.render(ID, collector)
    }

    private fun report(id: Identifier) {
        if (!reported) {
            logger.warn("Missing pattern texture: $id")
            reported = true
        }
    }
}