package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetics;
import dev.kikugie.fletching_table.mixin.MixinEnvironment;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Swaps the rendered elytra for a client-only decorated copy when the render state carries a cosmetic.
 * Runs at the head of the method, so both vanilla and the decoration renderers see the replaced stack.
 */
@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(EquipmentLayerRenderer.class)
public class ElytraCosmeticMixin {
    @Unique
    private static final String RENDER_METHOD =
        "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;" +
            "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;" +
            "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

    @ModifyVariable(method = RENDER_METHOD, at = @At("HEAD"), argsOnly = true)
    private ItemStack applyElytraCosmetic(
        ItemStack stack,
        @Local(argsOnly = true) EquipmentClientInfo.LayerType layerType,
        @Local(argsOnly = true) Object state
    ) {
        return ElytraCosmetics.applyToWings(layerType, state, stack);
    }
}
