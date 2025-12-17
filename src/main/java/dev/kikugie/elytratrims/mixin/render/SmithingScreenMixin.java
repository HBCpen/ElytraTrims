package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.elytratrims.api.item.ETItemFlag;
import dev.kikugie.elytratrims.render.SmithingTableRenderData;
import dev.kikugie.elytratrims.resource.ETTags;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(SmithingScreen.class)
public class SmithingScreenMixin {
	@Shadow
	@Final
	private ArmorStandRenderState armorStandPreview;
	@Shadow
	@Final
	private static int ARMOR_STAND_Y_ROT;

	@Unique
	private final SmithingTableRenderData renderData = new SmithingTableRenderData();

	@ModifyExpressionValue(method = "updateArmorStandPreview", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack rotateIfHasElytra(ItemStack stack) {
		renderData.update(stack.is(ETTags.ELYTRA_DECORATABLE));
		ETItemFlag.DEBUG.set(stack, true);
		return stack;
	}

	@Inject(method = "renderBg", at = @At("HEAD"))
	private void rotateArmorStand(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		renderData.tick(partialTick);
		armorStandPreview.bodyRot = ARMOR_STAND_Y_ROT + renderData.getRotation();
	}
}
