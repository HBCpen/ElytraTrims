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
import dev.kikugie.elytratrims.render.RENDER_LOGGER
import dev.kikugie.elytratrims.render.getSprite
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.data.AtlasIds

object ETBadAppleRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("bad_apple"), ETRenderMethod.POST)
    private val reported = mutableSetOf<Identifier>()
    private val animation: TextureAtlasSprite? by memoized {
        AtlasIds.ARMOR_TRIMS.getSprite(elytratrims("feature/bad_apple"), ::report)
    }

    override fun getType(): ETRendererID = type

    override fun prepare(original: ETRenderParameters): ETRenderParameters = with(original) {
        if (!stack.flags[ETFlags.BAD_APPLE]) return@with original
        original.copy(sprite = animation ?: return@with original, light = getEffectiveLight(), render = Sheets.armorTrimsSheet(false))
    }

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean = with(parameters) {
        runIf(stack.flags[ETFlags.BAD_APPLE] && sprite != null) { submitToCollector(collector) }
    }

    fun report(id: Identifier) {
        if (reported.add(id)) RENDER_LOGGER.warn("Missing pattern texture: $id")
    }

    override fun reset() {
        (::animation.getDelegate() as? MemoizedProperty<*>)?.clear()
    }
}