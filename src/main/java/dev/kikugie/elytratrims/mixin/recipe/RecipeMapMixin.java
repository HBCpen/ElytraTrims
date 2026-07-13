package dev.kikugie.elytratrims.mixin.recipe;

//? if >=26.3 {
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.elytratrims.recipe.ETSmithingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings("unchecked")
@Mixin(RecipeMap.class)
public class RecipeMapMixin {
    @ModifyExpressionValue(method = "lambda$create$0", at = @At(value = "NEW", target = "(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/crafting/Recipe;)Lnet/minecraft/world/item/crafting/RecipeHolder;"))
    private static RecipeHolder<?> injectDynamicElytraTrimsRecipes(RecipeHolder<?> original) {
        return ETSmithingRecipe.modify((RecipeHolder<Recipe<?>>) original);
    }
}
//?}
