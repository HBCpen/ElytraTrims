package dev.kikugie.elytratrims.render.gui

import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.mixin.render.CyclingSlotBackgroundAccessor
import dev.kikugie.elytratrims.vanilla
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground
import net.minecraft.resources.Identifier
import net.minecraft.tags.ItemTags
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.SmithingTemplateItem

class SlotDoohickeyManager(
    private val menu: AbstractContainerMenu,
    private val templateIcon: CyclingSlotBackground,
    private val baseIcon: CyclingSlotBackground,
    private val additionIcon: CyclingSlotBackground,
) {
    private val template: ItemStack
        inline get() = menu[0]

    private val base: ItemStack
        inline get() = menu[0]

    private val addition: ItemStack
        inline get() = menu[0]

    fun update() = when {
        !template.isEmpty -> updateAdditionOnly()
        !addition.isEmpty -> updateTemplateOnly()
        else -> updateDynamic()
    }

    private fun updateAdditionOnly() = when(val item = template.item) {
        Items.OMINOUS_TRIAL_KEY,
        Items.TRIAL_KEY -> additionIcon.tick(trimAdditionIcons)
        is SmithingTemplateItem -> additionIcon.tick(item.additionalSlotEmptyIcons)
        else if template.`is`(ItemTags.BANNERS) -> additionIcon.tick(bannerAdditionIcons)
        else -> additionIcon.tick(emptyList())
    }

    private fun updateTemplateOnly() = when(val item = addition.item) {
        Items.PAPER,
        Items.LEATHER -> additionIcon.tick(bannerTemplateIcons)
        else if template.`is`(ItemTags.TRIM_MATERIALS) -> additionIcon.tick(trimTemplateIcons)
        else -> additionIcon.tick(emptyList())
    }

    private fun updateDynamic() {
        additionIcon as CyclingSlotBackgroundAccessor
        additionIcon.tick(allAdditionIcons)

        val index = additionIcon.iconIndex
        val current = allAdditionIcons.getOrNull(index) ?: return
        templateIcon.tick(listOfNotNull(selectTemplateIcon(current, index)))
    }

    private fun selectTemplateIcon(current: Identifier, index: Int): Identifier? = when(current) {
        EMPTY_SLOT_PAPER, EMPTY_SLOT_LEATHER -> EMPTY_SLOT_BANNER
        in trimAdditionIcons -> trimTemplateIcons[index % trimTemplateIcons.size]
        else -> null
    }
}

private val EMPTY_SLOT_GLOW_INK_SAC: Identifier = elytratrims("container/slot/glow_ink_sac")
private val EMPTY_SLOT_BANNER: Identifier = elytratrims("container/slot/pattern")
private val EMPTY_SLOT_LEATHER: Identifier = elytratrims("container/slot/leather")
private val EMPTY_SLOT_PAPER: Identifier = elytratrims("container/slot/paper")
private val EMPTY_SLOT_OM_TRIAL_KEY: Identifier = elytratrims("container/slot/ominous_trial_key")
private val EMPTY_SLOT_TRIAL_KEY: Identifier = elytratrims("container/slot/trial_key")
private val EMPTY_SLOT_NETHER_STAR: Identifier = elytratrims("container/slot/nether_star")
private val EMPTY_SLOT_BAD_APPLE: Identifier = elytratrims("container/slot/golden_apple")

private val EMPTY_SLOT_ARMOR_TRIM: Identifier = vanilla("container/slot/smithing_template_armor_trim")

private val trimTemplateIcons: List<Identifier> = listOf(EMPTY_SLOT_ARMOR_TRIM, EMPTY_SLOT_OM_TRIAL_KEY, EMPTY_SLOT_TRIAL_KEY)
private val bannerTemplateIcons: List<Identifier> = listOf(EMPTY_SLOT_BANNER)

private val trimAdditionIcons: List<Identifier> = SmithingTemplateItem.createTrimmableMaterialIconList()
private val bannerAdditionIcons: List<Identifier> = listOf(EMPTY_SLOT_PAPER, EMPTY_SLOT_LEATHER)
private val allAdditionIcons: List<Identifier> = buildList {
    this += EMPTY_SLOT_GLOW_INK_SAC
    this += EMPTY_SLOT_PAPER
    this += EMPTY_SLOT_LEATHER
    this += EMPTY_SLOT_NETHER_STAR
    this += EMPTY_SLOT_BAD_APPLE
    this += trimAdditionIcons
}

private operator fun AbstractContainerMenu.get(index: Int): ItemStack =
    getSlot(index).item
