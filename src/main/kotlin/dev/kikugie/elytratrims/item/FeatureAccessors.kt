@file:OptIn(ExperimentalStdlibApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.elytratrims.item

import dev.kikugie.elytratrims.api.item.ETFeatureAccess
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents.TRIM
import net.minecraft.core.component.DataComponents.DYED_COLOR
import net.minecraft.core.component.DataComponents.BANNER_PATTERNS
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.equipment.trim.ArmorTrim
import net.minecraft.world.level.block.entity.BannerPatternLayers

inline operator fun <T> ETFeatureAccess<T>.invoke(): T = get()
inline operator fun <T> ETFeatureAccess<T>.invoke(value: T) = set(value)

private fun <T : Any> ItemStack.setOrRemove(type: DataComponentType<T>, value: T?) {
    if (value != null) set(type, value) else remove(type)
}

class TrimAccess(private val owner: ItemStack) : ETFeatureAccess<ArmorTrim?> {
    companion object {
        val ItemStack.trim: TrimAccess
            inline get() = TrimAccess(this)
    }
    override fun get(): ArmorTrim? = owner.get(TRIM)
    override fun set(value: ArmorTrim?) = owner.setOrRemove(TRIM, value)
    override fun copy(stack: ItemStack) = set(stack.get(TRIM))
    override fun clear() = set(null)
}

class ColorAccess(private val owner: ItemStack) : ETFeatureAccess<Int> {
    companion object {
        val ItemStack.color: ColorAccess
            inline get() = ColorAccess(this)
    }
    override fun get(): Int = DyedItemColor.getOrDefault(owner, 0)
    override fun set(value: Int) = owner.setOrRemove(DYED_COLOR, value.takeIf { it > 0 }?.let(::DyedItemColor))
    override fun copy(stack: ItemStack) = set(DyedItemColor.getOrDefault(stack, 0))
    override fun clear() = set(0)
}

class PatternsAccess(private val owner: ItemStack) : ETFeatureAccess<BannerPatternLayers?>, Iterable<BannerPatternLayers.Layer> {
    companion object {
        val ItemStack.patterns: PatternsAccess
            inline get() = PatternsAccess(this)
    }
    override fun get(): BannerPatternLayers? = owner.get(BANNER_PATTERNS)
    override fun set(value: BannerPatternLayers?) = owner.setOrRemove(BANNER_PATTERNS, value)
    override fun copy(stack: ItemStack) = set(stack.get(BANNER_PATTERNS))
    override fun clear() = set(null)

    override fun iterator(): Iterator<BannerPatternLayers.Layer> =
        get()?.layers.orEmpty().iterator()
}