package dev.kikugie.elytratrims.recipe.cauldron

import dev.kikugie.elytratrims.item.ColorAccess.Companion.color
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.mixin.recipe.CauldronInteractionDispatcherAccessor
import dev.kikugie.elytratrims.resource.ETTags
import dev.kikugie.elytratrims.resource.image.isVisible
import dev.kikugie.elytratrims.runIf
import net.minecraft.core.BlockPos
import net.minecraft.core.cauldron.CauldronInteraction
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LayeredCauldronBlock
import net.minecraft.world.level.block.state.BlockState

//~ if >=26.1 'CauldronInteraction' -> 'CauldronInteractions'
private typealias Interactions = net.minecraft.core.cauldron.CauldronInteractions

fun interface ETCauldronInteraction : CauldronInteraction {
    fun clean(player: Player, hand: InteractionHand, stack: ItemStack): Boolean

    override fun interact(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        stack: ItemStack
    ): InteractionResult = when {
        !stack.`is`(ETTags.ELYTRA_DECORATABLE) -> InteractionResult.PASS
        !clean(player, hand, stack) -> InteractionResult.TRY_WITH_EMPTY_HAND
        else -> InteractionResult.SUCCESS.also {
            if (level.isClientSide) return@also
            player.setItemInHand(hand, stack.copy())
            player.awardStat(Stats.CLEAN_ARMOR)
            LayeredCauldronBlock.lowerFillLevel(state, level, pos)
        }
    }
}

object ETCauldronInteractions {
    @JvmField val REMOVE_GLOW = ETCauldronInteraction { _, _, stack ->
        runIf(stack has ETItemFlag.GLOW) { ETItemFlag.GLOW[stack] = false }
    }

    @JvmField val REMOVE_COLOR = ETCauldronInteraction { _, _, stack ->
        runIf(stack.color.get().isVisible) { stack.color.clear() }
    }

    @JvmField val REMOVE_PATTERNS = ETCauldronInteraction { _, _, stack ->
        runIf(stack.patterns.get()?.layers.orEmpty().isNotEmpty()) { stack.patterns.clear() }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @JvmStatic fun register() {
        val water = Interactions.WATER as CauldronInteractionDispatcherAccessor
        water.`elytratrims$put`(ETTags.ELYTRA_DECORATABLE, REMOVE_GLOW)
        water.`elytratrims$put`(ETTags.ELYTRA_DECORATABLE, REMOVE_PATTERNS)
        water.`elytratrims$put`(ETTags.ELYTRA_DECORATABLE, REMOVE_COLOR)
    }
}