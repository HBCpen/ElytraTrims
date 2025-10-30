package dev.kikugie.elytratrims.item

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

fun ItemStack.hasCustomFlag(key: String) =
    get(DataComponents.CUSTOM_DATA)?.copyTag()?.contains(key) == true

fun ItemStack.setCustomFlag(key: String, value: Boolean) =
    if (value) addCustomFlag(key)
    else removeCustomFlag(key)

private fun ItemStack.addCustomFlag(key: String) {
    val data = (get(DataComponents.CUSTOM_DATA) ?: CustomData.EMPTY).copyTag()
    data.putBoolean(key, true)
    CustomData.set(DataComponents.CUSTOM_DATA, this, data)
}

private fun ItemStack.removeCustomFlag(key: String) {
    val data = get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return
    data.remove(key)
    CustomData.set(DataComponents.CUSTOM_DATA, this, data)
}
