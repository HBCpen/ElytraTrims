package dev.kikugie.elytratrims.compat.rvv

import cc.cassian.rrv.api.recipe.ReliableClientRecipeType
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu
import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.translatable
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object ETDecoratingRecipeType : ReliableClientRecipeType {
    const val TEMPLATE_SLOT: Int = 0
    const val BASE_SLOT: Int = 1
    const val ADDITION_SLOT: Int = 2
    const val RESULT_SLOT: Int = 3

    private val ID: Identifier = elytratrims("decorating")
    private val BACKGROUND: Identifier = Identifier.fromNamespaceAndPath("rrv", "textures/gui/type/smithing.png")

    override fun getId(): Identifier = ID
    override fun getDisplayName(): Text = "view.elytratrims.type.decorating".translatable()
    override fun getIcon(): ItemStack = ItemStack(Items.ELYTRA)

    override fun getGuiTexture(): Identifier = BACKGROUND
    override fun getDisplayWidth(): Int = 114
    override fun getDisplayHeight(): Int = 24
    override fun getSlotCount(): Int = 4

    override fun placeSlots(slots: RecipeViewMenu.SlotDefinition): Unit = with(slots) {
        addItemSlot(TEMPLATE_SLOT, 3, 4)
        addItemSlot(BASE_SLOT, 22, 4)
        addItemSlot(ADDITION_SLOT, 40, 4)
        addItemSlot(RESULT_SLOT, 94, 4)
    }

    override fun getCraftReferences(): List<ItemStack> =
        listOf(ItemStack(Items.SMITHING_TABLE))
}
