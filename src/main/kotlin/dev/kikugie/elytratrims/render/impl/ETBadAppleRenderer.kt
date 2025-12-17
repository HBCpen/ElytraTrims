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
import dev.kikugie.elytratrims.render.RENDER_LOGGER
import dev.kikugie.elytratrims.render.getSprite
import dev.kikugie.elytratrims.render.isMissing
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.data.AtlasIds
import net.minecraft.resources.Identifier

object ETBadAppleRenderer : ETDecorator {
    @JvmField val type: ETRendererID = ETRendererID(elytratrims("bad_apple"), ETRenderMethod.POST)
    private val reported = mutableSetOf<Identifier>()
    private val animation: TextureAtlasSprite by memoized {
        // For some reason, it must be in the armor trims atlas, or it will use an invalid texture
        AtlasIds.ARMOR_TRIMS.getSprite(elytratrims("feature/bad_apple"), ::report)
    }

    override fun getType(): ETRendererID = type

    override fun prepare(original: ETRenderParameters): ETRenderParameters = with(original) {
        if (!ETItemFlag.BAD_APPLE[stack]) return@with original
        original.copy(
            sprite = animation,
            light = getEffectiveLight(),
            render = Sheets.armorTrimsSheet(false),
            /*? if <1.21.9*///argument = converterImpl
        )
    }

    override fun render(parameters: ETRenderParameters, collector: SubmitNodeCollector): Boolean = with(parameters) {
        runIf(ETItemFlag.BAD_APPLE[stack] && (!sprite.isMissing || ETItemFlag.DEBUG[stack])) { submitToCollector(collector) }
    }

    fun report(id: Identifier) {
        if (reported.add(id)) RENDER_LOGGER.warn("Missing pattern texture: $id")
    }

    override fun reset() {
        (::animation.getDelegate() as? MemoizedProperty<*>)?.clear()
    }
}