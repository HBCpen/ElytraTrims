package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kikugie.elytratrims.api.render.ETRenderMethod;
import dev.kikugie.elytratrims.api.render.ETRenderParameters;
import dev.kikugie.elytratrims.api.impl.ETRenderingAPIImpl;
import dev.kikugie.elytratrims.render.ETRendererKt;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;

@Debug(export = true)
@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(EquipmentLayerRenderer.class)
public class ElytraRendererMixin {
    @Shadow
    @Final
    private Function<EquipmentLayerRenderer.LayerTextureKey, ResourceLocation> layerTextureLookup;

    @Unique
    private static final String RENDER_METHOD =
        //? if >=1.21.9 {
        "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;" +
        "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;" +
        "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V";
        //?} else {
        /*"renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;" +
            "Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;" +
            "Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V";
        *///?}

    @Unique
    private static final String CANCEL_METHOD =
        /*? if fabric {*/"Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I";
        /*?} else*//*"Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;getArmorLayerTintColor(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;II)I";*/

    @Inject(method = RENDER_METHOD, at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    //? if >=1.21.9 {
    private <S> void invokeETOverrideRenderers(
        EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack,
        PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int i, ResourceLocation resourceLocation, int j,
        int k, CallbackInfo ci, @Local List<EquipmentClientInfo.Layer> layers, @Local(ordinal = 4) com.llamalad7.mixinextras.sugar.ref.LocalIntRef m,
        @Share("parameters") LocalRef<ETRenderParameters> parameters, @Share("overridden") LocalBooleanRef overridden
    ) {
        if (layerType != EquipmentClientInfo.LayerType.WINGS) return;
        var order = new java.util.concurrent.atomic.AtomicInteger(m.get());
        var texture = !layers.getFirst().usePlayerTexture() || resourceLocation == null
            ? layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, layers.getFirst()))
            : resourceLocation;
        @SuppressWarnings("unchecked") var params = new ETRenderParameters((Model<Object>) model,
            object, itemStack, poseStack, RenderType.armorEntityGlint(),
            null, texture, i, 0xFFFFFFFF, j, k, order);

        parameters.set(params);
        // The result for the pre-render doesn't matter
        ETRenderingAPIImpl.render(params, submitNodeCollector, ETRenderMethod.PRE);
        if (ETRenderingAPIImpl.render(params, submitNodeCollector, ETRenderMethod.OVERRIDE))
            overridden.set(true);
        m.set(order.get());
    }
    //?} else {
    /*private void invokeETOverrideRenderers(
        EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAsset, Model armorModel,
        ItemStack item, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight,
        ResourceLocation playerTexture, CallbackInfo ci, @Local List<EquipmentClientInfo.Layer> layers,
        @Share("parameters") LocalRef<ETRenderParameters> parameters, @Share("overridden") LocalBooleanRef overridden
    ) {
        if (layerType != EquipmentClientInfo.LayerType.WINGS) return;
        var texture = !layers.getFirst().usePlayerTexture() || playerTexture == null
            ? layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, layers.getFirst()))
            : playerTexture;
        var params = new ETRenderParameters(armorModel,
            null, item, poseStack, RenderType.armorEntityGlint(),
            null, texture, packedLight, 0xFFFFFFFF, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0, ETRenderParameters.DUMMY);

        parameters.set(params);
        // The result for the pre-render doesn't matter
        ETRenderingAPIImpl.render(params, bufferSource, ETRenderMethod.PRE);
        if (ETRenderingAPIImpl.render(params, bufferSource, ETRenderMethod.OVERRIDE))
            overridden.set(true);
    }
    *///?}

    @ModifyExpressionValue(method = RENDER_METHOD, at = @At(value = "INVOKE", target = CANCEL_METHOD))
    private int cancelDefaultRenderer(int original, @Share("overridden") LocalBooleanRef overridden) {
        return overridden.get() ? 0 : original;
    }

    @Inject(
        method = RENDER_METHOD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"),
        cancellable = true
    )
    //? if >=1.21.9 {
    private <S> void invokeETRenderers(
        EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack,
        PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int i, ResourceLocation resourceLocation, int j,
        int k, CallbackInfo ci, @Local(ordinal = 4) com.llamalad7.mixinextras.sugar.ref.LocalIntRef m,
        @Share("parameters") LocalRef<ETRenderParameters> parameters
    ) {
        if (parameters.get() == null || layerType != EquipmentClientInfo.LayerType.WINGS)
            return;
        parameters.get().order().set(m.get());
        if (ETRenderingAPIImpl.render(parameters.get(), submitNodeCollector, ETRenderMethod.POST))
            ci.cancel();
        m.set(parameters.get().order().get());
    }
    //?} else {
    /*private void invokeETRenderers(
        EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAsset, Model armorModel,
        ItemStack item, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight,
        ResourceLocation playerTexture, CallbackInfo ci, @Share("parameters") LocalRef<ETRenderParameters> parameters
    ) {
        if (parameters.get() == null || layerType != EquipmentClientInfo.LayerType.WINGS)
            return;
        if (ETRenderingAPIImpl.render(parameters.get(), bufferSource, ETRenderMethod.POST))
            ci.cancel();
    }
    *///?}

    @Inject(
        method = RENDER_METHOD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/RenderType;"),
        cancellable = true
    )
    private <S> void cancelMissingTrimRender(CallbackInfo ci, @Local TextureAtlasSprite sprite) {
        if (ETRendererKt.isMissing(sprite)) ci.cancel();
    }
}