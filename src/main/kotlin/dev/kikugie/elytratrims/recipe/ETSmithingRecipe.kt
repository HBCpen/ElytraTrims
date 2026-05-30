package dev.kikugie.elytratrims.recipe

import dev.kikugie.elytratrims.api.item.ETItemFlag
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.mixin.recipe.SmithingTransformRecipeAccessor
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.*
import net.minecraft.world.item.equipment.trim.ArmorTrim
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimMaterials
import net.minecraft.world.item.equipment.trim.TrimPattern
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random

abstract class ETSmithingRecipe(delegate: SmithingRecipe) : SmithingTransformRecipe(
    //? if >26
    Recipe.CommonInfo(true),
    delegate.templateIngredient(),
    delegate.baseIngredient(),
    delegate.additionIngredient(),
    (delegate as SmithingTransformRecipeAccessor).result
) {
    companion object {
        @JvmField val RANDOM_TRIM = elytratrims("apply_random_trim")
        //? if >26
        @JvmField val BULLSHIT_I_HAVE_TO_DEAL_WITH: ThreadLocal<HolderLookup<TrimPattern>> = ThreadLocal()

        internal var seed: Int = Random.nextInt()
        private infix fun RecipeHolder<*>.replace(recipe: Recipe<*>) = RecipeHolder(id, recipe)

        @JvmStatic
        fun modify(entry: RecipeHolder<Recipe<*>>): RecipeHolder<Recipe<*>> = when (val recipe = entry.value) {
            is SmithingRecipe -> when (entry.id.identifier().toString()) {
                "elytratrims:apply_random_trim" -> entry replace RandomTrimRecipe(recipe, true)
                "elytratrims:apply_random_trim_reroll" -> entry replace RandomTrimRecipe(recipe, false)
                "elytratrims:apply_shield_pattern" -> entry replace PatternsRecipe(recipe, false)
                "elytratrims:apply_banner_pattern" -> entry replace PatternsRecipe(recipe, true)
                "elytratrims:apply_animation_effect" -> entry replace FlagRecipe(recipe, true, ETItemFlag.BAD_APPLE)
                "elytratrims:apply_gateway_effect" -> entry replace FlagRecipe(recipe, true, ETItemFlag.GATEWAY)
                "elytratrims:apply_glow_effect" -> entry replace FlagRecipe(recipe, true, ETItemFlag.GLOW)
                else -> entry
            }

            else -> entry
        }

        @JvmStatic
        fun advance() {
            seed = Random(seed).nextInt()
        }
    }

    open class FlagRecipe(delegate: SmithingRecipe, private val setter: Boolean, private val flag: ETItemFlag) : ETSmithingRecipe(delegate) {
        override fun assemble(input: SmithingRecipeInput, /*? if <26 >> ')'*//*provider: HolderLookup.Provider*/): ItemStack =
            input.base.copy().apply { flag[this] = setter }
    }

    class PatternsRecipe(delegate: SmithingRecipe, setter: Boolean) : FlagRecipe(delegate, setter, ETItemFlag.BANNER) {
        override fun assemble(input: SmithingRecipeInput, /*? if <26 >> ')'*//*provider: HolderLookup.Provider*/): ItemStack =
            super.assemble(input, /*? if <26 >> ')'*//*provider*/).apply { patterns.copy(input.template) }
    }

    class RandomTrimRecipe(delegate: SmithingRecipe, val consistent: Boolean) : ETSmithingRecipe(delegate) {
        //? if <26 {
        /*override fun assemble(input: SmithingRecipeInput, provider: HolderLookup.Provider): ItemStack {
            val trim = input.base.get(DataComponents.TRIM)
            val material = TrimMaterials.getFromIngredient(provider, input.addition).getOrNull()
                ?: return ItemStack.EMPTY
            val patterns = provider.lookupOrThrow(Registries.TRIM_PATTERN)
                .listElements().toList()

            return input.base.copyWithCount(1).apply { set(DataComponents.TRIM, getRandomTrim(material, trim, patterns)) }
        }
        *///?} else {
        override fun assemble(input: SmithingRecipeInput): ItemStack {
            val trim = input.base.get(DataComponents.TRIM)
            val material = input.addition.get(DataComponents.PROVIDES_TRIM_MATERIAL)
                ?: return ItemStack.EMPTY
            val patterns = BULLSHIT_I_HAVE_TO_DEAL_WITH.get()?.listElements()?.toList()
                ?: return ItemStack.EMPTY

            return input.base.copyWithCount(1).apply { set(DataComponents.TRIM, getRandomTrim(material, trim, patterns)) }
        }
        //?}

        private fun getRandomTrim(material: Holder<TrimMaterial>, trim: ArmorTrim?, variants: List<Holder<TrimPattern>>): ArmorTrim {
            val rng = if (consistent) Random(seed) else Random
            repeat(9) {
                val pattern = variants.random(rng)
                if (pattern != trim?.pattern || material != trim.material)
                    return ArmorTrim(material, pattern)
            }
            return ArmorTrim(material, variants.random(rng))
        }
    }
}

