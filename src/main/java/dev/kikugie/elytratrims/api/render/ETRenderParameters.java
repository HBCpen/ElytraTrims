package dev.kikugie.elytratrims.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Combines parameters that are used in the elytra rendering process.
 */
public record ETRenderParameters(
    @NotNull Model/*? if >=1.21.9 >>*/<Object> elytra,
    @Nullable Object object,
    @NotNull ItemStack stack,
    @NotNull PoseStack matrices,
    @NotNull RenderType render,
    @Nullable TextureAtlasSprite sprite,
    @NotNull ResourceLocation texture,
    int light,
    int color,
    int overlay,
    int outline,
    /*? if >=1.21.9 {*/@NotNull java.util.concurrent.atomic.AtomicInteger order
    /*?} else*///@NotNull VertexConsumerFactory converter
) {
    //? if >=1.21.9 {
    public static final VertexConsumerFactory DUMMY = (a, b) -> { throw new UnsupportedOperationException("Expected override"); };

    @FunctionalInterface
    public interface VertexConsumerFactory {
        @NotNull com.mojang.blaze3d.vertex.VertexConsumer create(
            @NotNull ETRenderParameters parameters,
            @NotNull net.minecraft.client.renderer.MultiBufferSource source
        );
    }
    //?}
}
