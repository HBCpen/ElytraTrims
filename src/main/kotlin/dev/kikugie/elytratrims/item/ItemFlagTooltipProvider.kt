package dev.kikugie.elytratrims.item

import dev.kikugie.elytratrims.api.item.ETItemFlag
import dev.kikugie.elytratrims.translatable
import net.minecraft.core.component.DataComponentGetter
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipProvider
import java.util.function.Consumer

class ItemFlagTooltipProvider(val flag: ETItemFlag) : TooltipProvider {

    override fun addToTooltip(context: Item.TooltipContext, adder: Consumer<Component>, tooltipFlag: TooltipFlag, access: DataComponentGetter) {
        if (access[DataComponents.CUSTOM_DATA]?.copyTag()?.contains(flag.key.toString()) == true)
            adder.accept("flag.item.${flag.key.namespace}.${flag.key.path}".translatable())
    }

    companion object {
        @JvmField val GLOW = ItemFlagTooltipProvider(ETItemFlag.GLOW)
        @JvmField val COSMIC = ItemFlagTooltipProvider(ETItemFlag.GATEWAY)
        @JvmField val BAD_APPLE = ItemFlagTooltipProvider(ETItemFlag.BAD_APPLE)
    }
}