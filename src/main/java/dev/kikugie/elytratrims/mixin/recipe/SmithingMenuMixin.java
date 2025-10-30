package dev.kikugie.elytratrims.mixin.recipe;

import dev.kikugie.elytratrims.recipe.ETSmithingRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {
	@Shadow
	@Final
	private Level level;

	@Shadow
	protected abstract SmithingRecipeInput createRecipeInput();

	@Inject(method = "onTake", at = @At(value = "RETURN"))
	private void advanceRandomTrim(Player player, ItemStack stack, CallbackInfo ci) {
		if (level instanceof ServerLevel serverLevel)
			ETSmithingRecipe.advance();
	}
}
