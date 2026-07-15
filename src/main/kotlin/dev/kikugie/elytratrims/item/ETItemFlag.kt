package dev.kikugie.elytratrims.item

import dev.kikugie.elytratrims.elytratrims
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

@JvmRecord
data class ETItemFlag(val key: Identifier) {
    operator fun get(stack: ItemStack): Boolean {
        return stack.hasCustomFlag(this.key.toString())
    }

    operator fun set(stack: ItemStack, value: Boolean) {
        stack.setCustomFlag(this.key.toString(), value)
    }

    companion object {
        @JvmField val GLOW: ETItemFlag = ETItemFlag(elytratrims("glow"))
        @JvmField val BANNER: ETItemFlag = ETItemFlag(elytratrims("banner"))
        @JvmField val GATEWAY: ETItemFlag = ETItemFlag(elytratrims("gateway"))
        @JvmField val BAD_APPLE: ETItemFlag = ETItemFlag(elytratrims("animation"))

        /**
         * Enables debug rendering in the smithing table menu.
         * <br></br>
         * Currently makes missing textures only appear in the elytra preview.
         */
        @JvmField val DEBUG: ETItemFlag = ETItemFlag(elytratrims("debug"))
    }
}
