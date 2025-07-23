package dev.kikugie.elytratrims.recipe.cauldron

import dev.kikugie.elytratrims.api.interaction.ETCauldronInteraction
import dev.kikugie.elytratrims.item.ETFlag
import dev.kikugie.elytratrims.item.banner
import dev.kikugie.elytratrims.item.color
import dev.kikugie.elytratrims.item.flags
import dev.kikugie.elytratrims.resource.image.Color4i
import dev.kikugie.elytratrims.runIf

@JvmField val REMOVE_GLOW = ETCauldronInteraction { _, _, stack ->
    runIf(stack.flags[ETFlag.GLOW]) { stack.flags[ETFlag.GLOW] = false }
}

@JvmField val REMOVE_COLOR = ETCauldronInteraction { _, _, stack ->
    runIf(stack.color.value != Color4i(0)) { stack.color.clear() }
}

@JvmField val REMOVE_PATTERN = ETCauldronInteraction { _, _, stack ->
    runIf(stack.banner.base != null || stack.banner.patterns.isNotEmpty()) { stack.banner.clear() }
}
