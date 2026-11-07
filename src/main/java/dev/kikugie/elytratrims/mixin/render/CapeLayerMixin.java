package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetics;
import dev.kikugie.fletching_table.mixin.MixinEnvironment;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides the cape while virtual wings are drawn, like vanilla does for a worn elytra.
 */
@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(CapeLayer.class)
public class CapeLayerMixin {
    @ModifyExpressionValue(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;)Z", ordinal = 0)
    )
    private boolean hideCapeBehindVirtualWings(boolean hasWings, @Local(argsOnly = true) AvatarRenderState state) {
        return hasWings || ElytraCosmetics.hasVirtualWings(state);
    }
}
