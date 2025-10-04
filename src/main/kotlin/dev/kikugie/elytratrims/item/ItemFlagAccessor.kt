package dev.kikugie.elytratrims.item

import dev.kikugie.elytratrims.api.item.ETItemFlag
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

private fun ItemStack.hasCustomFlag(key: String) =
    get(DataComponents.CUSTOM_DATA)?.copyTag()?.contains(key) == true

private fun ItemStack.setCustomFlag(key: String, value: Boolean) =
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

@JvmInline @JvmExposeBoxed @OptIn(ExperimentalStdlibApi::class)
value class ItemFlagAccessor(private val stack: ItemStack) {
    companion object {
        val ItemStack.flags: ItemFlagAccessor
            inline get() = ItemFlagAccessor(this)
    }

    operator fun get(flag: ETItemFlag): Boolean = stack.hasCustomFlag(flag.key.toString())
    operator fun set(flag: ETItemFlag, value: Boolean) = stack.setCustomFlag(flag.key.toString(), value)
    fun remove(flag: ETItemFlag) = set(flag, false)
}