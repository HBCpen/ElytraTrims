package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kikugie.elytratrims.render.api.ETParameters;
import dev.kikugie.elytratrims.render.impl.*;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Debug(export = true)
@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(EquipmentLayerRenderer.class)
public class ElytraRendererMixin {
    @Shadow
    @Final
    private Function<EquipmentLayerRenderer.LayerTextureKey, Identifier> layerTextureLookup;

    @Unique
    private static final String RENDER_METHOD =
        "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;" +
            "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;" +
            "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

    @ModifyArg(method = RENDER_METHOD, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;order(I)Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;"), index = 0)
    private int storeGlobalRenderOrder(int order, @Share("elytratrims$order") LocalIntRef orderRef) {
        orderRef.set(order + 1);
        return order;
    }

    @Inject(method = RENDER_METHOD, at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private void configureElytraTrimsParameters(
        EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAssetId, Model<?> model, Object state,
        ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
        @Nullable Identifier playerTextureOverride, int outlineColor, int order, CallbackInfo ci,
        @Local(name = "layers") List<EquipmentClientInfo.Layer> layers,
        @Share("elytratrims$parameters") LocalRef<ETParameters> paramsRef,
        @Share("elytratrims$order") LocalIntRef orderRef
    ) {
        ETParameters params = buildParameters(layerType, layers, model, state, itemStack, poseStack, playerTextureOverride, lightCoords, outlineColor);
        paramsRef.set(params);
        orderRef.set(order);
    }

    @ModifyExpressionValue(method = RENDER_METHOD, at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<Object> renderElytraTrimsDecorationsPre(
        Iterator<Object> original,
        @Share("elytratrims$parameters") LocalRef<ETParameters> paramsRef,
        @Share("elytratrims$order") LocalIntRef orderRef,
        @Local(argsOnly = true) SubmitNodeCollector collector,
        @Local(name = "hasTrim") LocalBooleanRef hasTrimRef
    ) {
        ETParameters params = paramsRef.get();
        if (params == null) return original;

        AtomicInteger order = new AtomicInteger(orderRef.get());
        boolean overwriteOriginal = renderReplDecorations(collector, params, order);

        orderRef.set(order.get());
        hasTrimRef.set(false);

        return overwriteOriginal ? Collections.emptyIterator() : original;
    }

    @Inject(method = RENDER_METHOD, at = @At("RETURN"))
    private void renderElytraTrimsDecorationsPost(
        CallbackInfo ci,
        @Share("elytratrims$parameters") LocalRef<ETParameters> paramsRef,
        @Share("elytratrims$order") LocalIntRef orderRef,
        @Local(argsOnly = true) SubmitNodeCollector collector
    ) {
        ETParameters params = paramsRef.get();
        if (params == null) return;

        AtomicInteger order = new AtomicInteger(orderRef.get());
        renderPostDecorations(collector, params, order);
    }

    @Unique
    private ETParameters buildParameters(EquipmentClientInfo.LayerType type, List<EquipmentClientInfo.Layer> layers, Model<?> model, Object state, ItemStack itemStack, PoseStack poseStack, Identifier playerTextureOverride, int lightCoords, int outlineColor) {
        if (type != EquipmentClientInfo.LayerType.WINGS || layers.isEmpty()) return null;

        ETParameters params = new ETParameters();
        params.model = model;
        params.state = state;
        params.stack = itemStack;
        params.matrices = poseStack;
        params.lightCoords = lightCoords;
        params.outlineColour = outlineColor;
        params.texture = !layers.getFirst().usePlayerTexture() || playerTextureOverride == null
            ? layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(type, layers.getFirst()))
            : playerTextureOverride;
        return params;
    }

    @Unique
    private static boolean renderReplDecorations(SubmitNodeCollector collector, ETParameters parameters, AtomicInteger order) {
        if (collector == null || parameters == null || order == null) return false;

        return ETGatewayRenderer.INSTANCE.render(parameters, collector, order);
    }

    @Unique
    private static boolean renderPostDecorations(SubmitNodeCollector collector, ETParameters parameters, AtomicInteger order) {
        if (collector == null || parameters == null || order == null) return false;

        boolean rendered;
        rendered = ETBadAppleRenderer.INSTANCE.render(parameters, collector, order);
        rendered = ETColorRenderer.INSTANCE.render(parameters, collector, order) || rendered;
        rendered = ETPatternsRenderer.INSTANCE.render(parameters, collector, order) || rendered;
        rendered = ETTrimsRenderer.INSTANCE.render(parameters, collector, order) || rendered;
        return rendered;
    }
}