package dev.kikugie.elytratrims.mixin.recipe;

import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingTransformRecipe.class)
public interface SmithingTransformRecipeAccessor {
	//~ if >26 'net.minecraft.world.item.crafting.TransmuteResult' -> 'net.minecraft.world.item.ItemStackTemplate'
	@Accessor net.minecraft.world.item.ItemStackTemplate getResult();
}
