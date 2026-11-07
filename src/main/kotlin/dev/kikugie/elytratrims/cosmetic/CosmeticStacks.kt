package dev.kikugie.elytratrims.cosmetic

import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.item.ETItemFlag
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.util.Util
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.equipment.trim.ArmorTrim
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimPattern
import net.minecraft.world.level.block.entity.BannerPattern
import net.minecraft.world.level.block.entity.BannerPatternLayers
import kotlin.jvm.optionals.getOrNull

/**
 * Turns an [ElytraCosmetic] into a client-only copy of the worn elytra carrying the matching vanilla components,
 * which the existing decoration renderers then draw as if the item was really decorated.
 *
 * Holders are taken from the connected server's registries when available and are created directly otherwise,
 * so trims, materials and patterns render even if the server doesn't know about them.
 */
object CosmeticStacks {
    private const val CACHE_SIZE = 16

    private class Entry(val cosmetic: ElytraCosmetic, val registries: RegistryAccess?, val base: ItemStack, val result: ItemStack)

    private val cache = ArrayDeque<Entry>(CACHE_SIZE)

    @JvmStatic
    fun apply(cosmetic: ElytraCosmetic, base: ItemStack, registries: RegistryAccess?): ItemStack {
        // Rendering happens on the render thread only, but keep the cache consistent if a mod calls in from elsewhere.
        synchronized(cache) {
            val hit = cache.firstOrNull {
                it.cosmetic == cosmetic && it.registries === registries && ItemStack.isSameItemSameComponents(it.base, base)
            }
            if (hit != null) return hit.result

            val result = build(cosmetic, base, registries)
            if (cache.size >= CACHE_SIZE) cache.removeLast()
            cache.addFirst(Entry(cosmetic, registries, base.copy(), result))
            return result
        }
    }

    private fun build(cosmetic: ElytraCosmetic, base: ItemStack, registries: RegistryAccess?): ItemStack = base.copy().apply {
        // The cosmetic fully replaces whatever decorations the real item has.
        remove(DataComponents.TRIM)
        remove(DataComponents.DYED_COLOR)
        remove(DataComponents.BANNER_PATTERNS)
        remove(DataComponents.CUSTOM_NAME)

        cosmetic.trim?.let { set(DataComponents.TRIM, ArmorTrim(material(it.material, registries), pattern(it.pattern, registries))) }
        cosmetic.color?.let { set(DataComponents.DYED_COLOR, DyedItemColor(it and 0xFFFFFF)) }
        if (cosmetic.rainbow) set(DataComponents.CUSTOM_NAME, Text.literal("jeb_"))
        if (cosmetic.patterns.isNotEmpty()) set(DataComponents.BANNER_PATTERNS, BannerPatternLayers(
            cosmetic.patterns.map { BannerPatternLayers.Layer(bannerPattern(it.pattern, registries), it.color) }
        ))

        ETItemFlag.GLOW[this] = cosmetic.glow
        ETItemFlag.BANNER[this] = cosmetic.bannerStyle
        ETItemFlag.GATEWAY[this] = cosmetic.gateway
        ETItemFlag.BAD_APPLE[this] = cosmetic.animation
    }

    @JvmStatic
    fun pattern(id: Identifier, registries: RegistryAccess?): Holder<TrimPattern> =
        registries.find(Registries.TRIM_PATTERN, id)
            ?: Holder.direct(TrimPattern(id, Text.translatable(Util.makeDescriptionId("trim_pattern", id)), false))

    @JvmStatic
    fun material(id: Identifier, registries: RegistryAccess?): Holder<TrimMaterial> =
        registries.find(Registries.TRIM_MATERIAL, id)
            ?: Holder.direct(TrimMaterial(id.withPrefix("trim/"), Text.translatable(Util.makeDescriptionId("trim_material", id))))

    @JvmStatic
    fun bannerPattern(id: Identifier, registries: RegistryAccess?): Holder<BannerPattern> =
        registries.find(Registries.BANNER_PATTERN, id)
            ?: Holder.direct(BannerPattern(id, Util.makeDescriptionId("block", id.withPrefix("banner."))))

    private fun <T : Any> RegistryAccess?.find(key: ResourceKey<out Registry<out T>>, id: Identifier): Holder<T>? =
        this?.lookup(key)?.getOrNull()?.get(id)?.getOrNull()
}
