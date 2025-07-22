package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.*
import dev.kikugie.elytratrims.api.*
import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.item.ETFlag
import dev.kikugie.elytratrims.item.banner
import dev.kikugie.elytratrims.item.flags
import dev.kikugie.elytratrims.render.RENDER_LOGGER
import dev.kikugie.elytratrims.api.impl.getEffectiveLight
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.api.impl.renderToBuffer
import dev.kikugie.elytratrims.resource.image.Color4i
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Holder
import net.minecraft.world.level.block.entity.BannerPattern

object ETPatternsRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRenderingAPI.registerDecorator(ETRendererID(elytratrims("cosmic"), ETRenderMethod.OVERRIDE), this)
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
        val access = stack.banner
        if (access.base == null && access.patterns.isEmpty()) return@with parameters

        val atlas = if (stack.flags[ETFlag.BANNER]) Sheets.BANNER_SHEET else Sheets.SHIELD_SHEET
        return copy(texture = atlas, light = getEffectiveLight()) {
            val sprite = checkNotNull(it.sprite) { "Sprite should be set for pattern rendering" }
            ItemRenderer.getArmorFoilBuffer(it.source, RenderType.armorTranslucent(it.texture), it.stack.hasFoil())
                .let(sprite::wrap)
        }
    }

    override fun render(parameters: ETRenderParameters): Boolean = with(parameters) {
        val access = stack.banner
        if (access.base == null && access.patterns.isEmpty()) return@with false

        val banner = stack.flags[ETFlag.BANNER]
        var rendered = false

        access.base?.let {
            val sprite = (if (banner) bannerBase else shieldBase)
                ?: return@let
            val blend = Color4i(color).blend(Color4i(it))
            copy(sprite = sprite, color = blend.value).renderToBuffer()
            rendered = true
        }

        access.patterns.forEach { (pattern, it) ->
            val sprite = (if (banner) bannerPatterns(pattern) else shieldPatterns(pattern))
                ?: return@forEach
            val blend = Color4i(color).blend(Color4i(it))
            copy(sprite = sprite, color = blend.value).renderToBuffer()
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