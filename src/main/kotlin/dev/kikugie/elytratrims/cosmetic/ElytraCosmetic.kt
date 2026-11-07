package dev.kikugie.elytratrims.cosmetic

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.Identifier
import net.minecraft.world.item.DyeColor
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * A purely visual description of how an elytra should look.
 *
 * Everything is stored as plain identifiers, so a cosmetic is independent of the registries of the server
 * the player is connected to, and can be serialized as-is (config file now, peer-to-peer sync later).
 */
data class ElytraCosmetic(
    val trim: Trim? = null,
    /** Dye color as `0xRRGGBB`, or `null` to keep the elytra undyed. */
    val color: Int? = null,
    /** Cycles the dye color through the rainbow (the `jeb_` easter egg). */
    val rainbow: Boolean = false,
    val patterns: List<PatternLayer> = emptyList(),
    /** Uses the high-resolution banner textures for [patterns] instead of the shield ones. */
    val bannerStyle: Boolean = false,
    val glow: Boolean = false,
    val gateway: Boolean = false,
    val animation: Boolean = false,
) {
    data class Trim(val pattern: Identifier, val material: Identifier) {
        companion object {
            @JvmField val CODEC: Codec<Trim> = RecordCodecBuilder.create { it.group(
                Identifier.CODEC.fieldOf("pattern").forGetter(Trim::pattern),
                Identifier.CODEC.fieldOf("material").forGetter(Trim::material),
            ).apply(it, ::Trim) }
        }
    }

    data class PatternLayer(val pattern: Identifier, val color: DyeColor) {
        companion object {
            @JvmField val CODEC: Codec<PatternLayer> = RecordCodecBuilder.create { it.group(
                Identifier.CODEC.fieldOf("pattern").forGetter(PatternLayer::pattern),
                DyeColor.CODEC.fieldOf("color").forGetter(PatternLayer::color),
            ).apply(it, ::PatternLayer) }
        }
    }

    val isEmpty: Boolean get() = this == EMPTY

    companion object {
        @JvmField val EMPTY = ElytraCosmetic()

        /** Upper bound for pattern layers, mostly to keep the editor and future sync payloads sane. */
        const val MAX_PATTERN_LAYERS: Int = 16

        @JvmField val CODEC: Codec<ElytraCosmetic> = RecordCodecBuilder.create { it.group(
            Trim.CODEC.optionalFieldOf("trim").forGetter { c -> Optional.ofNullable(c.trim) },
            Codec.intRange(0, 0xFFFFFF).optionalFieldOf("color").forGetter { c -> Optional.ofNullable(c.color) },
            Codec.BOOL.optionalFieldOf("rainbow", false).forGetter(ElytraCosmetic::rainbow),
            PatternLayer.CODEC.sizeLimitedListOf(MAX_PATTERN_LAYERS).optionalFieldOf("patterns", emptyList()).forGetter(ElytraCosmetic::patterns),
            Codec.BOOL.optionalFieldOf("banner_style", false).forGetter(ElytraCosmetic::bannerStyle),
            Codec.BOOL.optionalFieldOf("glow", false).forGetter(ElytraCosmetic::glow),
            Codec.BOOL.optionalFieldOf("gateway", false).forGetter(ElytraCosmetic::gateway),
            Codec.BOOL.optionalFieldOf("animation", false).forGetter(ElytraCosmetic::animation),
        ).apply(it) { trim, color, rainbow, patterns, bannerStyle, glow, gateway, animation ->
            ElytraCosmetic(trim.getOrNull(), color.getOrNull(), rainbow, patterns, bannerStyle, glow, gateway, animation)
        } }
    }
}
