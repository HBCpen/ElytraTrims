package dev.kikugie.elytratrims.recipe

import dev.kikugie.elytratrims.api.item.ETItemFlag
import dev.kikugie.elytratrims.item.ETFlags
import dev.kikugie.elytratrims.item.ItemFlagAccessor.Companion.flags
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.mixin.recipe.SmithingTransformRecipeAccessor
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.*
import net.minecraft.world.item.equipment.trim.ArmorTrim
import net.minecraft.world.item.equipment.trim.TrimMaterials
import kotlin.jvm.optionals.getOrElse

abstract class ETSmithingRecipe(delegate: SmithingRecipe) : SmithingTransformRecipe(
    delegate.templateIngredient(),
    delegate.baseIngredient(),
    delegate.additionIngredient(),
    (delegate as SmithingTransformRecipeAccessor).result
) {
    companion object {
        private infix fun RecipeHolder<*>.replace(recipe: Recipe<*>) = RecipeHolder(id, recipe)

        @JvmStatic
        fun modify(entry: RecipeHolder<Recipe<*>>): RecipeHolder<Recipe<*>> = when (val recipe = entry.value) {
            is SmithingRecipe -> when (entry.id.location().toString()) {
                "elytratrims:apply_random_trim" -> entry replace RandomTrimRecipe(recipe)
                "elytratrims:apply_shield_pattern" -> entry replace PatternsRecipe(recipe, false)
                "elytratrims:apply_banner_pattern" -> entry replace PatternsRecipe(recipe, true)
                "elytratrims:apply_animation_effect" -> entry replace FlagRecipe(recipe, true, ETFlags.BAD_APPLE)
                "elytratrims:apply_gateway_effect" -> entry replace FlagRecipe(recipe, true, ETFlags.GATEWAY)
                "elytratrims:apply_glow_effect" -> entry replace FlagRecipe(recipe, true, ETFlags.GLOW)
                else -> entry
            }

            else -> entry
        }
    }

    open class FlagRecipe(delegate: SmithingRecipe, private val setter: Boolean, private val flag: ETItemFlag) : ETSmithingRecipe(delegate) {
        override fun assemble(input: SmithingRecipeInput, provider: HolderLookup.Provider): ItemStack? = input.base.copy().apply {
            flags[flag] = setter
        }
    }

    class PatternsRecipe(delegate: SmithingRecipe, setter: Boolean) : FlagRecipe(delegate, setter, ETFlags.BANNER) {
        override fun assemble(input: SmithingRecipeInput, provider: HolderLookup.Provider): ItemStack? =
            super.assemble(input, provider)?.apply { patterns.copy(input.template) }
    }

    class RandomTrimRecipe(delegate: SmithingRecipe) : ETSmithingRecipe(delegate) {
        override fun assemble(input: SmithingRecipeInput, provider: HolderLookup.Provider): ItemStack {
            val material = TrimMaterials.getFromIngredient(provider, input.addition)
                .getOrElse { return ItemStack.EMPTY }

            val present = input.base.get(DataComponents.TRIM)?.pattern
            val variants = provider.lookupOrThrow(Registries.TRIM_PATTERN)
                .listElements().toList()

            if (present != null && (variants.isEmpty() || variants.size == 1 && variants.first() == present))
                return ItemStack.EMPTY

            val applied: ArmorTrim
            while (true) {
                val choice = variants.random()
                if (choice != present) {
                    applied = ArmorTrim(material, choice)
                    break
                }
            }

            return input.base.copyWithCount(1).apply {
                set(DataComponents.TRIM, applied)
            }
        }
    }
}

