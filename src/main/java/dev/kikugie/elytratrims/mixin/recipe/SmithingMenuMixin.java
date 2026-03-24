package dev.kikugie.elytratrims.mixin.recipe;

import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.elytratrims.recipe.ETSmithingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
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

	@Inject(method = "onTake", at = @At(value = "RETURN"))
	private void advanceRandomTrim(Player player, ItemStack stack, CallbackInfo ci) {
		if (level instanceof ServerLevel)
			ETSmithingRecipe.advance();
	}

	//? if >26 {
	@Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;recipeAccess()Lnet/minecraft/world/item/crafting/RecipeManager;"))
	private void workAroundMojangBeingABitchAndActivelyMakingTheCodeWorseToWorkWith(CallbackInfo ci, @Local(name = "serverLevel") ServerLevel level) {
		ETSmithingRecipe.BULLSHIT_I_HAVE_TO_DEAL_WITH.set(level.holderLookup(Registries.TRIM_PATTERN));
	}
	//?}
}
