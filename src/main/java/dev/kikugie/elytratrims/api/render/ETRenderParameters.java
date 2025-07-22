package dev.kikugie.elytratrims.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Combines parameters that are used in the elytra rendering process.
 */
public record ETRenderParameters(
    @NotNull Model elytra,
    @NotNull ItemStack stack,
    @NotNull PoseStack matrices,
    @NotNull MultiBufferSource source,
    @Nullable TextureAtlasSprite sprite,
    @NotNull ResourceLocation texture,
    int light,
    int color,
    @NotNull Function<@NotNull ETRenderParameters, @NotNull VertexConsumer> converter
) {
}
