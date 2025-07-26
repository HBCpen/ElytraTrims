package dev.kikugie.elytratrims.recipe.cauldron

import dev.kikugie.elytratrims.api.interaction.ETCauldronInteraction
import dev.kikugie.elytratrims.item.ColorAccess.Companion.color
import dev.kikugie.elytratrims.item.ETFlags
import dev.kikugie.elytratrims.item.ItemFlagAccessor.Companion.flags
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.item.invoke
import dev.kikugie.elytratrims.runIf

@JvmField val REMOVE_GLOW = ETCauldronInteraction { _, _, stack ->
    runIf(stack.flags[ETFlags.GLOW]) { stack.flags[ETFlags.GLOW] = false }
}

@JvmField val REMOVE_COLOR = ETCauldronInteraction { _, _, stack ->
    runIf(stack.color() != 0) { stack.color.clear() }
}

@JvmField val REMOVE_PATTERNS = ETCauldronInteraction { _, _, stack ->
    runIf(!stack.patterns()?.layers.isNullOrEmpty()) { stack.patterns.clear() }
}

@Deprecated("No longer removable")
@JvmField val REMOVE_TOUHOU = ETCauldronInteraction { _, _, stack ->
    runIf(stack.flags[ETFlags.BAD_APPLE]) { stack.flags.remove(ETFlags.BAD_APPLE) }
}