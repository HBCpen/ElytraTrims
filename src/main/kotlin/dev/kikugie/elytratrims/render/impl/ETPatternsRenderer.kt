package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.Memoizer
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.item.hasNot
import dev.kikugie.elytratrims.memoize
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.render.glowingLightCoords
import dev.kikugie.elytratrims.render.isMissing
import dev.kikugie.elytratrims.render.prepare
import dev.kikugie.elytratrims.render.render
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BannerPattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ETPatternsRenderer : ETDecorator {
    @JvmField val ID: ETRendererID = ETRendererID(elytratrims("patterns"), ETRenderMethod.POST)

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val reported: MutableSet<Identifier> = mutableSetOf()
    private val patterns: Memoizer<Pair<Holder<BannerPattern>, Boolean>, TextureAtlasSprite> = memoize { (pattern, banner) ->
        ElytraTrimsAtlas.ATLAS_ID.getSprite(texture(pattern.value().assetId, banner), ::report)
    }

    override fun getType(): ETRendererID = ID

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean {
        val prm = parameters.clone().apply { lightCoords = glowingLightCoords }
        val isBanner = prm.stack has ETItemFlag.BANNER
        var rendered = false
        for (layer in prm.stack.patterns) {
            val pattern = patterns(layer.pattern to isBanner)
            if (pattern.isMissing && prm.stack hasNot ETItemFlag.DEBUG) continue

            prm.apply { tintedColor = layer.color.textureDiffuseColor; renderType = ElytraTrimsAtlas.elytraDecorations(); sprite = pattern }.prepare(ID)
            rendered = prm.render(ID, collector) || rendered
        }

        return rendered
    }

    private fun report(id: Identifier) {
        if (reported.add(id)) logger.warn("Missing pattern texture: $id")
    }

    private fun texture(asset: Identifier, banner: Boolean): Identifier =
        asset.withPrefix(if (banner) "entity/wings/banner/" else "entity/wings/shield/")
}