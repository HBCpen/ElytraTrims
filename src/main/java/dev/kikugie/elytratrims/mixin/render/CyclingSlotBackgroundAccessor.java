package dev.kikugie.elytratrims.mixin.render;

import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(CyclingSlotBackground.class)
public interface CyclingSlotBackgroundAccessor {
    @Accessor int getIconIndex();
}
