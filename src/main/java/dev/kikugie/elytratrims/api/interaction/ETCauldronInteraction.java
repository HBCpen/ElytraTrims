package dev.kikugie.elytratrims.api.interaction;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ETCauldronInteraction {
    boolean removeDecorations(@NotNull Player player, @NotNull InteractionHand hand, @NotNull ItemStack stack);
}
