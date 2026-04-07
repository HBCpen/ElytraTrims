package dev.kikugie.elytratrims.mixin.recipe;

import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.elytratrims.resource.ETTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.DyeRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DyeRecipe.class)
public class DyeRecipeMixin {
    @ModifyArg(
        method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/TransmuteRecipe;createWithOriginalComponents(Lnet/minecraft/world/item/ItemStackTemplate;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"),
        index = 0
    )
    private ItemStackTemplate hijackTheRecipeToMakeItDynamic(ItemStackTemplate result, @Local(name = "targetStack") ItemStack targetStack) {
        return targetStack.is(ETTags.ELYTRA_DECORATABLE) ? ItemStackTemplate.fromNonEmptyStack(targetStack) : result;
    }
}
