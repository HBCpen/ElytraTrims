package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.*
import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.api.impl.getEffectiveLight
import dev.kikugie.elytratrims.api.impl.submitToCollector
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.item.ETFlags
import dev.kikugie.elytratrims.item.ItemFlagAccessor.Companion.flags
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.item.invoke
import dev.kikugie.elytratrims.render.RENDER_LOGGER
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.resource.image.Color4i
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Holder
import net.minecraft.world.level.block.entity.BannerPattern

object ETPatternsRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("patterns"), ETRenderMethod.POST)
    override fun getType(): ETRendererID = type

    private val reported = mutableSetOf<Identifier>()
    private val shieldBase: TextureAtlasSprite? by memoized {
        Sheets.SHIELD_SHEET.getSprite(texture(vanilla("base"), false), ::report)
    }
    private val bannerBase: TextureAtlasSprite? by memoized {
        Sheets.BANNER_SHEET.getSprite(texture(vanilla("base"), true), ::report)
    }
    private val shieldPatterns: Memoizer<Holder<BannerPattern>, TextureAtlasSprite?> = memoize {
        Sheets.SHIELD_SHEET.getSprite(texture(it.value().assetId, false), ::report)
    }
    private val bannerPatterns: Memoizer<Holder<BannerPattern>, TextureAtlasSprite?> = memoize {
        Sheets.BANNER_SHEET.getSprite(texture(it.value().assetId, true), ::report)
    }

    override fun prepare(parameters: ETRenderParameters): ETRenderParameters = with(parameters) {
        if (stack.patterns()?.layers.isNullOrEmpty()) return@with parameters

        val atlas = if (stack.flags[ETFlags.BANNER]) Sheets.BANNER_SHEET else Sheets.SHIELD_SHEET
        copy(texture = atlas, light = getEffectiveLight(), render = RenderType.armorTranslucent(texture))
    }

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean = with(parameters) {
        val banner = stack.flags[ETFlags.BANNER]
        var rendered = false

        for (it in stack.patterns) {
            val sprite = (if (banner) bannerPatterns(it.pattern) else shieldPatterns(it.pattern))
                ?: continue
            val blend = Color4i(color).blend(Color4i(it.color))
            copy(sprite = sprite, color = blend.value).submitToCollector(collector)
            rendered = true
        }

        rendered
    }

    fun report(id: Identifier) {
        if (reported.add(id)) RENDER_LOGGER.warn("Missing pattern texture: $id")
    }

    override fun reset() {
        reported.clear()
        shieldPatterns.clear()
        bannerPatterns.clear()

        (::shieldBase.getDelegate() as? MemoizedProperty<*>)?.clear()
        (::bannerBase.getDelegate() as? MemoizedProperty<*>)?.clear()
    }

    private fun texture(asset: Identifier, banner: Boolean) = asset.withPrefix(if (banner) "entity/wings/banner/" else "entity/wings/shield/")
}