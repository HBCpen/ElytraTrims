package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.Memoizer
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.item.hasNot
import dev.kikugie.elytratrims.memoize
import dev.kikugie.elytratrims.render.*
import dev.kikugie.elytratrims.render.api.ETDecorator
import dev.kikugie.elytratrims.render.api.ETParameters
import dev.kikugie.elytratrims.resource.image.isVisible
import dev.kikugie.elytratrims.resource.image.toColor4i
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BannerPattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

object ETPatternsRenderer : ETDecorator {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val reported: MutableSet<Identifier> = mutableSetOf()
    private val patterns: Memoizer<Pair<Holder<BannerPattern>, Boolean>, TextureAtlasSprite> = memoize { (pattern, banner) ->
        ElytraTrimsAtlas.ATLAS_ID.getSprite(texture(pattern.value().assetId, banner), ::report)
    }

    override fun ETParameters.render(collector: SubmitNodeCollector, i: AtomicInteger): Boolean {
        if (stack.patterns.isEmpty() || !color.isVisible) return false

        val isBanner = stack has ETItemFlag.BANNER
        for (layer in stack.patterns) {
            val sprite = patterns(layer.pattern to isBanner)
            if (sprite.isMissing && stack hasNot ETItemFlag.DEBUG) continue

            val color = layer.color.textureDiffuseColor.toColor4i(color.a)
            val renderType = RenderTypes.armorTranslucent(ElytraTrimsAtlas.ATLAS_SHEET)
            collector.submitModel(i, renderType = renderType, lightCoords = effectiveLightCoords, sprite = sprite, color = color)
        }
        return true
    }

    private fun report(id: Identifier) {
        if (reported.add(id)) logger.warn("Missing pattern texture: $id")
    }

    private fun texture(asset: Identifier, banner: Boolean): Identifier =
        asset.withPrefix(if (banner) "entity/wings/banner/" else "entity/wings/shield/")
}