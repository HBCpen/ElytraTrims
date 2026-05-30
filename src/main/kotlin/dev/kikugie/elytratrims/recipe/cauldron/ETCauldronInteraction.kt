package dev.kikugie.elytratrims.recipe.cauldron

import dev.kikugie.elytratrims.api.impl.ETInteractionAPIImpl
import dev.kikugie.elytratrims.mixin.recipe.CauldronInteractionDispatcherAccessor
import dev.kikugie.elytratrims.resource.ETTags
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

object ETCauldronInteraction : CauldronInteraction {
    @Suppress("CAST_NEVER_SUCCEEDS")
    @JvmStatic fun register() {
        val water = Interactions.WATER as CauldronInteractionDispatcherAccessor
        water.`elytratrims$put`(ETTags.ELYTRA_DECORATABLE, ::interact)
    }

    override fun interact(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, stack: ItemStack): InteractionResult {
        if (!stack.`is`(ETTags.ELYTRA_DECORATABLE))
            return InteractionResult.PASS
        val copy = stack.copy()
        var cleaned = false

        for (it in ETInteractionAPIImpl.interactions)
            cleaned = it.removeDecorations(player, hand, copy) || cleaned
        return if (!cleaned) InteractionResult.TRY_WITH_EMPTY_HAND else InteractionResult.SUCCESS.also {
            if (level.isClientSide) return@also
            player.setItemInHand(hand, copy)
            player.awardStat(Stats.CLEAN_ARMOR)
            LayeredCauldronBlock.lowerFillLevel(state, level, pos)
        }
    }

    private fun composeInteractions(existing: CauldronInteraction) = CauldronInteraction { state, world, pos, player, hand, stack ->
        val result: InteractionResult = existing.interact(state, world, pos, player, hand, stack)
        if (result.consumesAction()) result else interact(state, world, pos, player, hand, stack)
    }
}