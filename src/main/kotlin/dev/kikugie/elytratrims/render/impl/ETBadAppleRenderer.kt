package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.hasNot
import dev.kikugie.elytratrims.render.api.ETDecorator
import dev.kikugie.elytratrims.render.api.ETParameters
import dev.kikugie.elytratrims.render.effectiveLightCoords
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.render.isMissing
import dev.kikugie.elytratrims.render.submitModel
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

object ETBadAppleRenderer : ETDecorator {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val sprite by lazy { ElytraTrimsAtlas.ATLAS_ID.getSprite(elytratrims("feature/bad_apple"), ::report) }
    private var reported: Boolean = false

    override fun render(parameters: ETParameters, collector: SubmitNodeCollector, i: AtomicInteger): Boolean = with(parameters) {
        if (stack hasNot ETItemFlag.BAD_APPLE || sprite.isMissing && stack hasNot ETItemFlag.DEBUG) return false
        collector.submitModel(i, renderType = ElytraTrimsAtlas.elytraDecorations(), lightCoords = effectiveLightCoords, sprite = sprite)
        return true
    }

    private fun report(id: Identifier) {
        if (!reported) {
            logger.warn("Missing pattern texture: $id")
            reported = true
        }
    }
}