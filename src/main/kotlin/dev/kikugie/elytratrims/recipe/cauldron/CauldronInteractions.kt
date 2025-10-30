package dev.kikugie.elytratrims.recipe.cauldron

import dev.kikugie.elytratrims.api.interaction.ETCauldronInteraction
import dev.kikugie.elytratrims.api.item.ETItemFlag
import dev.kikugie.elytratrims.item.ColorAccess.Companion.color
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.item.invoke
import dev.kikugie.elytratrims.runIf

@JvmField val REMOVE_GLOW = ETCauldronInteraction { _, _, stack ->
    runIf(ETItemFlag.GLOW[stack]) { ETItemFlag.GLOW[stack] = false }
}

@JvmField val REMOVE_COLOR = ETCauldronInteraction { _, _, stack ->
    runIf(stack.color() != 0) { stack.color.clear() }
}

@JvmField val REMOVE_PATTERNS = ETCauldronInteraction { _, _, stack ->
    runIf(!stack.patterns()?.layers.isNullOrEmpty()) { stack.patterns.clear() }
}

@Deprecated("No longer removable")
@JvmField val REMOVE_TOUHOU = ETCauldronInteraction { _, _, stack ->
    runIf(ETItemFlag.BAD_APPLE[stack]) { ETItemFlag.BAD_APPLE[stack] = false }
}