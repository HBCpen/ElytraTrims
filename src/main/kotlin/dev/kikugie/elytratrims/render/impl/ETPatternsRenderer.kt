package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.*
import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.api.impl.getEffectiveLight
import dev.kikugie.elytratrims.api.impl.submitToCollector
import dev.kikugie.elytratrims.api.item.ETItemFlag
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.item.invoke
import dev.kikugie.elytratrims.render.RENDER_LOGGER
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.render.isMissing
import dev.kikugie.elytratrims.resource.image.Color4i
import dev.kikugie.elytratrims.resource.image.blend
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BannerPattern

object ETPatternsRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("patterns"), ETRenderMethod.POST)

    private val reported = mutableSetOf<Identifier>()
    private val patterns: Memoizer<Pair<Holder<BannerPattern>, Boolean>, TextureAtlasSprite> = memoize { (pattern, banner) ->
        ElytraTrimsAtlas.ATLAS_ID.getSprite(texture(pattern.value().assetId, banner), ::report)
    }

    override fun getType(): ETRendererID = type

    override fun prepare(parameters: ETRenderParameters): ETRenderParameters = with(parameters) {
        if (stack.patterns()?.layers.isNullOrEmpty()) return@with parameters
        copy(
            light = getEffectiveLight()
        )
    }

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean = with(parameters) {
        val banner = ETItemFlag.BANNER[stack]
        for (it in stack.patterns) {
            val sprite = patterns(it.pattern to banner)
            if (sprite.isMissing && !ETItemFlag.DEBUG[stack]) continue

            val blend = Color4i(color).blend(Color4i(it.color))
            copy(sprite = sprite, color = blend.packed, render = RenderTypes.armorTranslucent(ElytraTrimsAtlas.ATLAS_SHEET)).submitToCollector(collector)
        }

        stack.patterns.isNotEmpty()
    }

    fun report(id: Identifier) {
        if (reported.add(id)) RENDER_LOGGER.warn("Missing pattern texture: $id")
    }

    override fun reset() {
        reported.clear()
        patterns.clear()
    }

    private fun texture(asset: Identifier, banner: Boolean) = asset.withPrefix(if (banner) "entity/wings/banner/" else "entity/wings/shield/")
}