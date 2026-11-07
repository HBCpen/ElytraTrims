package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetics;
import dev.kikugie.fletching_table.mixin.MixinEnvironment;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws an elytra on players whose cosmetic asks for it although none is worn, e.g. on top of a chestplate.
 * Only the wings layer sees the replaced stack, so the armor layer still draws the real chestplate.
 */
@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(WingsLayer.class)
public class WingsLayerMixin {
    @ModifyExpressionValue(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;chestEquipment:Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack drawVirtualWings(ItemStack chest, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) HumanoidRenderState state) {
        return ElytraCosmetics.wingsStack(state, chest);
    }
}
