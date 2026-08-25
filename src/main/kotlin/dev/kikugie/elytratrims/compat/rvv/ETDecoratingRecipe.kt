package dev.kikugie.elytratrims.compat.rvv

import cc.cassian.rrv.api.recipe.ReliableClientRecipe
import cc.cassian.rrv.api.recipe.ReliableClientRecipeType
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import dev.kikugie.elytratrims.MINECRAFT
import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.compat.rvv.ETDecoratingRecipeType.ADDITION_SLOT
import dev.kikugie.elytratrims.compat.rvv.ETDecoratingRecipeType.BASE_SLOT
import dev.kikugie.elytratrims.compat.rvv.ETDecoratingRecipeType.RESULT_SLOT
import dev.kikugie.elytratrims.compat.rvv.ETDecoratingRecipeType.TEMPLATE_SLOT
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.SmithingScreen
import net.minecraft.resources.Identifier
import java.util.function.Supplier

class ETDecoratingRecipe(
    private val id: Identifier,
    private val template: SlotContent,
    private val base: SlotContent,
    private val addition: SlotContent,
    private val result: SlotContent,
    private val resultDriver: SlotContent,
    private val resultVariants: Int = 1,
    private val description: Text? = null,
) : ReliableClientRecipe {
    override fun getType(): ReliableClientRecipeType = ETDecoratingRecipeType
    override fun getId(): Identifier = id

    override fun getIngredients(): List<SlotContent> = listOf(template, base, addition)
    override fun getResults(): List<SlotContent> = listOf(result)

    override fun bindSlots(slots: RecipeViewMenu.SlotFillContext): Unit = with(slots) {
        bindOptionalSlot(TEMPLATE_SLOT, template, RecipeViewMenu.OptionalSlotRenderer.DEFAULT)
        bindOptionalSlot(BASE_SLOT, base, RecipeViewMenu.OptionalSlotRenderer.DEFAULT)
        bindOptionalSlot(ADDITION_SLOT, addition, RecipeViewMenu.OptionalSlotRenderer.DEFAULT)
        bindDependentSlot(RESULT_SLOT, Supplier(::resultIndex), result)

        if (description != null) addAdditionalStackModifier(RESULT_SLOT) { _, tooltip ->
            tooltip.add(description)
        }
    }

    private fun resultIndex(): Int {
        if (resultVariants <= 1) return resultDriver.index()
        val time = MINECRAFT.level?.gameTime ?: 0L
        val variant = (time / VARIANT_DURATION % resultVariants).toInt()
        return resultDriver.index() * resultVariants + variant
    }

    override fun supportsItemTransfer(): Boolean = true

    override fun getTransferClasses(): List<Class<out AbstractContainerScreen<*>>> =
        listOf<Class<out AbstractContainerScreen<*>>>(SmithingScreen::class.java)

    override fun mapRecipeItems(transferMap: ReliableClientRecipe.RecipeTransferMap, screen: AbstractContainerScreen<*>) {
        transferMap.linkSlots(TEMPLATE_SLOT, 0)
        transferMap.linkSlots(BASE_SLOT, 1)
        transferMap.linkSlots(ADDITION_SLOT, 2)
    }

    private companion object {
        const val VARIANT_DURATION = 25L
    }
}
