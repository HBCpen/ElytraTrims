package dev.kikugie.elytratrims.mixin.recipe;

//? if <26.3 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.elytratrims.recipe.ETSmithingRecipe;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Unique
    private static final String RECIPE_LAMBDA =
        //$ if fabric '"lambda$prepare$0";' else '"lambda$prepare$1;"'
        "lambda$prepare$0";

    @SuppressWarnings("unchecked")
    @ModifyExpressionValue(method = RECIPE_LAMBDA, at = @At(value = "NEW", target = "(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/crafting/Recipe;)Lnet/minecraft/world/item/crafting/RecipeHolder;"))
    private static RecipeHolder<?> injectDynamicElytraTrimsRecipes(RecipeHolder<?> original) {
        return ETSmithingRecipe.modify((RecipeHolder<Recipe<?>>) original);
    }
}
*///?}
