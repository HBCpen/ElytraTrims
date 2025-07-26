package dev.kikugie.elytratrims.api.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ETFeatureAccess<T> {
    T get();
    void set(T value);
    void copy(@NotNull ItemStack stack);
    void clear();
}
