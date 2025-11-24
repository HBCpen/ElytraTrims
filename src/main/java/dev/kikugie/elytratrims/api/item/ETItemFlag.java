package dev.kikugie.elytratrims.api.item;

import dev.kikugie.elytratrims.item.ItemFlagAccessorKt;
import net.minecraft.world.item.ItemStack;

import static dev.kikugie.elytratrims.CommonsKt.elytratrims;

public record ETItemFlag(net.minecraft.resources.Identifier key) {
	public static final ETItemFlag GLOW = new ETItemFlag(elytratrims("glow"));
	public static final ETItemFlag BANNER = new ETItemFlag(elytratrims("banner"));
	public static final ETItemFlag GATEWAY = new ETItemFlag(elytratrims("gateway"));
	public static final ETItemFlag BAD_APPLE = new ETItemFlag(elytratrims("animation"));

	/**
	 * Enables debug rendering in the smithing table menu.
	 * <br>
	 * Currently makes missing textures only appear in the elytra preview.
	 */
	public static final ETItemFlag DEBUG = new ETItemFlag(elytratrims("debug"));

	public boolean get(ItemStack stack) {
		return ItemFlagAccessorKt.hasCustomFlag(stack, key().toString());
	}

	public void set(ItemStack stack, boolean value) {
		ItemFlagAccessorKt.setCustomFlag(stack, key().toString(), value);
	}
}
