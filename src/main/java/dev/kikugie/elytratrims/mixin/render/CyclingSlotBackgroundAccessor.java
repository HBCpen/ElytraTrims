package dev.kikugie.elytratrims.mixin.render;

import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CyclingSlotBackground.class)
public interface CyclingSlotBackgroundAccessor {
    @Accessor int getIconIndex();
}
