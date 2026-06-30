@file:OptIn(ExperimentalStdlibApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.elytratrims.item

import dev.kikugie.elytratrims.resource.image.Color4i
import dev.kikugie.elytratrims.resource.image.isVisible
import dev.kikugie.elytratrims.resource.image.toUColor4i
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.equipment.trim.ArmorTrim
import net.minecraft.world.level.block.entity.BannerPatternLayers

interface ETFeatureAccess<T> {
    fun get(): T
    fun set(value: T)
    fun copy(stack: ItemStack)
    fun clear()
}

private fun <T : Any> ItemStack.setOrRemove(type: DataComponentType<T>, value: T?) {
    if (value != null) set(type, value) else remove(type)
}

class TrimAccess(private val owner: ItemStack) : ETFeatureAccess<ArmorTrim?> {
    companion object {
        val ItemStack.trim: TrimAccess
            inline get() = TrimAccess(this)
    }
    override fun get(): ArmorTrim? = owner.get(TRIM)
    override infix fun set(value: ArmorTrim?) = owner.setOrRemove(TRIM, value)
    override infix fun copy(stack: ItemStack) = set(stack.get(TRIM))
    override fun clear() = set(null)
}

class ColorAccess(private val owner: ItemStack) : ETFeatureAccess<Color4i> {
    companion object {
        val ItemStack.color: ColorAccess
            inline get() = ColorAccess(this)
    }
    override fun get(): Color4i = DyedItemColor.getOrDefault(owner, 0).toUColor4i()
    override infix fun set(value: Color4i) {
        if (!value.isVisible) owner.remove(DYED_COLOR)
        else owner.set(DYED_COLOR, DyedItemColor(value.packed))
    }
    override infix fun copy(stack: ItemStack) = set(DyedItemColor.getOrDefault(stack, 0).toUColor4i())
    override fun clear() = set(Color4i.TRANSPARENT)
}

class PatternsAccess(private val owner: ItemStack) : ETFeatureAccess<BannerPatternLayers?>, Collection<BannerPatternLayers.Layer> {
    companion object {
        val ItemStack.patterns: PatternsAccess
            inline get() = PatternsAccess(this)
    }
    private val layers get() = get()?.layers.orEmpty()
    override fun get(): BannerPatternLayers? = owner.get(BANNER_PATTERNS)
    override infix fun set(value: BannerPatternLayers?) = owner.setOrRemove(BANNER_PATTERNS, value)
    override infix fun copy(stack: ItemStack) = set(stack.get(BANNER_PATTERNS))
    override fun clear() = set(null)

    override val size: Int get() = layers.size
    override fun isEmpty(): Boolean = layers.isEmpty()
    override fun iterator(): Iterator<BannerPatternLayers.Layer> = layers.iterator()
    override fun contains(element: BannerPatternLayers.Layer): Boolean = element in layers
    override fun containsAll(elements: Collection<BannerPatternLayers.Layer>): Boolean = layers.containsAll(elements)
}