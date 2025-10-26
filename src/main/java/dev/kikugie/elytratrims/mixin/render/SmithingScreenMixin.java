package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.elytratrims.render.ETRendererKt;
import dev.kikugie.elytratrims.render.SmithingTableRenderData;
import dev.kikugie.elytratrims.resource.ETTags;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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
	private @Nullable ArmorStand armorStandPreview;

	@Unique
	private final SmithingTableRenderData renderData = new SmithingTableRenderData();

	@Inject(method = "containerTick", at = @At("RETURN"))
	private void tickArmorStand(CallbackInfo ci) {
		if (armorStandPreview != null)
			armorStandPreview.tick();
	}

	@Inject(method = "updateArmorStandPreview", at = @At(value = "RETURN"))
	private void rotateIfHasElytra(ItemStack stack, CallbackInfo ci) {
		boolean isElytra = !stack.isEmpty() && stack.is(ETTags.ELYTRA_DECORATABLE);
		renderData.update(isElytra);
	}

	@WrapOperation(method = "renderBg", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;IIIIFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V"))
	private void renderArmorStand(GuiGraphics gui, int x1, int y1, int x2, int y2, float scale, Vector3f translation, Quaternionf angle, @Nullable Quaternionf ignored, LivingEntity stand, Operation<Void> original, @Local(argsOnly = true) float partial, @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY) {
		renderData.tick(partial);
		ETRendererKt.renderEntityInInventoryFollowsMouse(gui, x1, y1, x2, y2, (int) scale, 0F, mouseX, mouseY, stand, renderData.getRotation());
	}
}
