package dev.kikugie.elytratrims.render.impl

import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.MemoizedProperty
import dev.kikugie.elytratrims.api.impl.copy
import dev.kikugie.elytratrims.api.impl.getEffectiveLight
import dev.kikugie.elytratrims.api.impl.renderToBuffer
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.ETFlags
import dev.kikugie.elytratrims.item.ItemFlagAccessor.Companion.flags
import dev.kikugie.elytratrims.memoized
import dev.kikugie.elytratrims.render.RENDER_LOGGER
import dev.kikugie.elytratrims.render.getSprite
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.TextureAtlasSprite

object ETBadAppleRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("bad_apple"), ETRenderMethod.POST)
    private val reported = mutableSetOf<Identifier>()
    private val animation: TextureAtlasSprite? by memoized {
        Sheets.ARMOR_TRIMS_SHEET.getSprite(elytratrims("feature/bad_apple"), ::report)
    }

    override fun getType(): ETRendererID = type

    override fun prepare(original: ETRenderParameters): ETRenderParameters = with(original) {
        if (!stack.flags[ETFlags.BAD_APPLE]) return@with original
        original.copy(texture = Sheets.ARMOR_TRIMS_SHEET, sprite = animation ?: return@with original, light = getEffectiveLight()) {
            val sprite = checkNotNull(it.sprite) { "Sprite should be set for bad apple rendering" }
            ItemRenderer.getArmorFoilBuffer(it.source, RenderType.armorTranslucent(it.texture), it.stack.hasFoil())
                .let(sprite::wrap)
        }
    }

    override fun render(parameters: ETRenderParameters): Boolean = with(parameters) {
        if (!stack.flags[ETFlags.BAD_APPLE] || sprite == null) return@with false
        renderToBuffer()
        true
    }

    fun report(id: Identifier) {
        if (reported.add(id)) RENDER_LOGGER.warn("Missing pattern texture: $id")
    }

    override fun reset() {
        (::animation.getDelegate() as? MemoizedProperty<*>)?.clear()
    }
}