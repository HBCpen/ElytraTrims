package dev.kikugie.elytratrims.compat.rvv

import cc.cassian.rrv.api.recipe.ItemView
import cc.cassian.rrv.api.recipe.ReliableClientRecipe
import cc.cassian.rrv.client.recipe.ClientRecipeManager
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import dev.kikugie.elytratrims.MINECRAFT
import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.recipe.ETSmithingRecipe
import dev.kikugie.elytratrims.translatable
import dev.kikugie.elytratrims.vanilla
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SmithingRecipe
import net.minecraft.world.item.crafting.SmithingTrimRecipe
import net.minecraft.world.item.equipment.trim.TrimPattern

object ETRecipeViewerCompat {
    private val SMITHING_CATEGORY: Identifier = vanilla("smithing")

    fun initialize() {
        ItemView.addClientReloadCallback(::reloadRecipes)
        ItemView.addClientRecipeProvider(::collectRecipes)
    }

    private fun reloadRecipes() {
        for (id in ETSmithingRecipe.DECORATION_RECIPES)
            ItemView.excludeRecipe(SMITHING_CATEGORY, id)
    }

    private fun collectRecipes(output: MutableList<ReliableClientRecipe>) {
        if (MINECRAFT.level == null) return

        val smithing = ClientRecipeManager.INSTANCE.getRecipesForType(RecipeType.SMITHING)
            .associate { it.id.identifier() to it.value }

        fun add(id: Identifier, builder: (Identifier, SmithingRecipe) -> ETDecoratingRecipe) {
            val recipe = smithing[id] ?: return
            output += builder(id, recipe)
        }

        add(ETSmithingRecipe.GLOW_EFFECT) { id, recipe -> flagRecipe(id, recipe, ETItemFlag.GLOW) }
        add(ETSmithingRecipe.GATEWAY_EFFECT) { id, recipe -> flagRecipe(id, recipe, ETItemFlag.GATEWAY) }
        add(ETSmithingRecipe.ANIMATION_EFFECT) { id, recipe -> flagRecipe(id, recipe, ETItemFlag.BAD_APPLE) }
        add(ETSmithingRecipe.SHIELD_PATTERN) { id, recipe -> patternRecipe(id, recipe, hd = false) }
        add(ETSmithingRecipe.BANNER_PATTERN) { id, recipe -> patternRecipe(id, recipe, hd = true) }

        val patterns = trimPatterns()
        if (patterns.isEmpty()) return
        add(ETSmithingRecipe.RANDOM_TRIM) { id, recipe -> randomTrimRecipe(id, recipe, patterns) }
        add(ETSmithingRecipe.RANDOM_TRIM_REROLL) { id, recipe -> randomTrimRecipe(id, recipe, patterns) }
    }

    private fun flagRecipe(id: Identifier, recipe: SmithingRecipe, flag: ETItemFlag): ETDecoratingRecipe {
        val base = SlotContent.of(recipe.baseIngredient())
        val results = base.validContents.map { it.copy().apply { flag[this] = true } }
        return ETDecoratingRecipe(
            id = id,
            template = SlotContent.of(recipe.templateIngredient().orElse(null)),
            base = base,
            addition = SlotContent.of(recipe.additionIngredient().orElse(null)),
            result = SlotContent.of(results),
            resultDriver = base,
            description = description(id),
        )
    }

    private fun patternRecipe(id: Identifier, recipe: SmithingRecipe, hd: Boolean): ETDecoratingRecipe {
        val base = SlotContent.of(recipe.baseIngredient())
        val results = base.validContents.map { it.copy().apply { ETItemFlag.BANNER[this] = hd } }
        return ETDecoratingRecipe(
            id = id,
            template = SlotContent.of(recipe.templateIngredient().orElse(null)),
            base = base,
            addition = SlotContent.of(recipe.additionIngredient().orElse(null)),
            result = SlotContent.of(results),
            resultDriver = base,
            description = description(id),
        )
    }

    private fun randomTrimRecipe(id: Identifier, recipe: SmithingRecipe, patterns: List<Holder<TrimPattern>>): ETDecoratingRecipe {
        val base = SlotContent.of(recipe.baseIngredient())
        val addition = SlotContent.of(recipe.additionIngredient().orElse(null))
        val elytra = base.validContents.firstOrNull() ?: ItemStack.EMPTY
        val results = addition.validContents.flatMap { material ->
            patterns.map { applyTrim(elytra, material, it) }
        }
        return ETDecoratingRecipe(
            id = id,
            template = SlotContent.of(recipe.templateIngredient().orElse(null)),
            base = base,
            addition = addition,
            result = SlotContent.of(results),
            resultDriver = addition,
            resultVariants = patterns.size,
            description = description(id),
        )
    }

    private fun trimPatterns(): List<Holder<TrimPattern>> =
        MINECRAFT.level?.registryAccess()?.lookupOrThrow(Registries.TRIM_PATTERN)?.listElements()?.toList().orEmpty()

    private fun applyTrim(base: ItemStack, material: ItemStack, pattern: Holder<TrimPattern>): ItemStack =
        //? if <26 {
        /*SmithingTrimRecipe.applyTrim(MINECRAFT.level!!.registryAccess(), base, material, pattern)
        *///?} else
        SmithingTrimRecipe.applyTrim(base, material, pattern)

    private fun description(id: Identifier): Text = "recipe.${id.namespace}.${id.path}".translatable()
}
