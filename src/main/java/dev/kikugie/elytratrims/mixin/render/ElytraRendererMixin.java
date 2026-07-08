package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kikugie.elytratrims.render.ETRendererKt;
import dev.kikugie.elytratrims.render.api.ETParameters;
import dev.kikugie.elytratrims.render.impl.ETBadAppleRenderer;
import dev.kikugie.elytratrims.render.impl.ETColorRenderer;
import dev.kikugie.elytratrims.render.impl.ETGatewayRenderer;
import dev.kikugie.elytratrims.render.impl.ETPatternsRenderer;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Unique
    private static final String CANCEL_METHOD =
        /*? if fabric {*/"Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I";
        /*?} else*///"Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;getArmorLayerTintColor(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;II)I";

    @Inject(method = RENDER_METHOD, at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private <S> void invokeETOverrideRenderers(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> equipmentAssetId,
        Model<? super S> model,
        S state,
        ItemStack itemStack,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        @Nullable Identifier playerTextureOverride,
        int outlineColor,
        int oldOrder,
        CallbackInfo ci,
        @Local(name = "layers") List<EquipmentClientInfo.Layer> layers,
        @Local(name = "nextOrder") LocalIntRef nextOrder,
        @Share("parameters") LocalRef<ETParameters> paramsRef,
        @Share("overridden") LocalBooleanRef overrideRef
    ) {
        if (layerType != EquipmentClientInfo.LayerType.WINGS) return;
        var order = new AtomicInteger(nextOrder.get());
        var params = new ETParameters();
        params.model = model;
        params.state = state;
        params.stack = itemStack;
        params.matrices = poseStack;
        params.lightCoords = lightCoords;
        params.outlineColour = outlineColor;
        params.texture = !layers.getFirst().usePlayerTexture() || playerTextureOverride == null
            ? layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, layers.getFirst()))
            : playerTextureOverride;

        paramsRef.set(params);
        if (ETGatewayRenderer.INSTANCE.render(params, submitNodeCollector, order))
            overrideRef.set(true);
        nextOrder.set(order.get());
    }

    @ModifyExpressionValue(method = RENDER_METHOD, at = @At(value = "INVOKE", target = CANCEL_METHOD))
    private int cancelDefaultRenderer(int original, @Share("overridden") LocalBooleanRef overrideRef) {
        return overrideRef.get() ? 0 : original;
    }

    @Inject(
        method = RENDER_METHOD,
        at = @At(value = "TAIL"),
        cancellable = true
    )
    private <S> void invokeETRenderers(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> equipmentAssetId,
        Model<? super S> model,
        S state,
        ItemStack itemStack,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        @Nullable Identifier playerTextureOverride,
        int outlineColor,
        int oldOrder,
        CallbackInfo ci,
        @Local(name = "nextOrder") LocalIntRef nextOrder,
        @Share("parameters") LocalRef<ETParameters> paramsRef
    ) {
        var params = paramsRef.get();
        if (params == null || layerType != EquipmentClientInfo.LayerType.WINGS)
            return;
        var order = new AtomicInteger(nextOrder.get());
        var proceed = true;
        proceed = ETBadAppleRenderer.INSTANCE.render(params, submitNodeCollector, order);
        proceed = ETColorRenderer.INSTANCE.render(params, submitNodeCollector, order) && proceed;
        proceed = ETPatternsRenderer.INSTANCE.render(params, submitNodeCollector, order) && proceed;

        if (!proceed) ci.cancel();
        nextOrder.set(order.get());
    }
    
    //? if >=26.3 {
    /*@WrapWithCondition(
        method = RENDER_METHOD,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;" +
                "Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;" +
                "IIILnet/minecraft/client/renderer/texture/UvMapping;" +
                "ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            ordinal = 2
        )
    )
    private <S> boolean cancelMissingTrimRender(
        OrderedSubmitNodeCollector instance,
        Model<S> model,
        Object o,
        PoseStack poseStack,
        RenderType renderType,
        int a,
        int b,
        int c,
        net.minecraft.client.renderer.texture.UvMapping uvMapping,
        int d,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
        @Local(name = "textureHandle") net.minecraft.client.resources.palette.PalettedTextureManager.Handle textureHandle
    ) {
        return !ETRendererKt.isMissing(textureHandle.textureLocation());
    }
    *///?} else {
    @WrapWithCondition(
        method = RENDER_METHOD,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;" +
                "Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;" +
                "IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;" +
                "ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            ordinal = 2
        )
    )
    private <S> boolean cancelMissingTrimRender(
        OrderedSubmitNodeCollector instance,
        Model<? super S> model,
        S s,
        PoseStack poseStack,
        RenderType renderType,
        int a,
        int b,
        int c,
        net.minecraft.client.renderer.texture.TextureAtlasSprite textureAtlasSprite,
        int i,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
        @Local(name = "sprite") net.minecraft.client.renderer.texture.TextureAtlasSprite sprite
    ) {
        return !ETRendererKt.isMissing(sprite.contents().name());
    }
    //?}
}