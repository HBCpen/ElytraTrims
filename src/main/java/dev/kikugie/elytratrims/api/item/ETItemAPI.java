package dev.kikugie.elytratrims.api.item;

import dev.kikugie.elytratrims.item.ItemFlagAccessor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ETItemAPI {
    static ItemFlagAccessor getFlagAccessor(ItemStack stack) {
        return new ItemFlagAccessor(stack);
    }
}
