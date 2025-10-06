package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kikugie.elytratrims.api.render.ETRenderMethod;
import dev.kikugie.elytratrims.api.render.ETRenderParameters;
import dev.kikugie.elytratrims.api.impl.ETRenderingAPIImpl;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.Reference;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(EquipmentLayerRenderer.class)
public class ElytraRendererMixin {
    @Shadow
    @Final
    private Function<EquipmentLayerRenderer.LayerTextureKey, ResourceLocation> layerTextureLookup;

    @Unique
    private static final String RENDER_METHOD = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;" +
        "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;" +
        "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V";

    @Unique
    private static final String CANCEL_METHOD =
        /*? if fabric {*/"Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I";
        /*?} else*//*"Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;getArmorLayerTintColor(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;II)I";*/

    @Inject(method = RENDER_METHOD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"))
    private <S> void invokeETOverrideRenderers(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> resourceKey,
        Model<? super S> model,
        S object,
        ItemStack itemStack,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int i,
        ResourceLocation resourceLocation,
        int j,
        int k,
        CallbackInfo ci,
        @Local List<EquipmentClientInfo.Layer> layers,
        @Local(ordinal = 3) LocalIntRef m,
        @Share("parameters") LocalRef<ETRenderParameters> parameters,
        @Share("overridden") LocalBooleanRef overridden
    ) {
        if (layerType != EquipmentClientInfo.LayerType.WINGS) return;
        var base = layers.getFirst(); // Checked for isEmpty in the source
        ResourceLocation texture = base.usePlayerTexture() && resourceLocation != null
            ? resourceLocation : layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, base));
        AtomicInteger order = new AtomicInteger(m.get());
        ETRenderParameters params = new ETRenderParameters((Model<Object>) model, object, itemStack, poseStack, RenderType.armorEntityGlint(), null, texture, i, 0xFFFFFFFF, j, k, order);
        parameters.set(params);
        // The result for the pre-render doesn't matter
        ETRenderingAPIImpl.render(params, submitNodeCollector, ETRenderMethod.PRE);
        if (ETRenderingAPIImpl.render(params, submitNodeCollector, ETRenderMethod.OVERRIDE))
            overridden.set(true);
        m.set(order.get());
    }

    @ModifyExpressionValue(method = RENDER_METHOD, at = @At(value = "INVOKE", target = CANCEL_METHOD))
    private int cancelDefaultRenderer(int original, @Share("overridden") LocalBooleanRef overridden) {
        return overridden.get() ? 0 : original;
    }

    @Inject(
        method = RENDER_METHOD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"),
        cancellable = true
    )
    private <S> void invokeETRenderers(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> resourceKey,
        Model<? super S> model,
        S object,
        ItemStack itemStack,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int i,
        ResourceLocation resourceLocation,
        int j,
        int k,
        CallbackInfo ci,
        @Local(ordinal = 3) LocalIntRef m,
        @Share("parameters") LocalRef<ETRenderParameters> parameters
    ) {
        parameters.get().order().set(m.get());
        if (layerType != EquipmentClientInfo.LayerType.WINGS) return;
        if (ETRenderingAPIImpl.render(parameters.get(), submitNodeCollector, ETRenderMethod.POST))
            ci.cancel();
        m.set(parameters.get().order().get());
    }
}