package dev.kikugie.elytratrims.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Combines parameters that are used in the elytra rendering process.
 */
public record ETRenderParameters(
    @NotNull Model<Object> elytra,
    @NotNull Object object,
    @NotNull ItemStack stack,
    @NotNull PoseStack matrices,
    @NotNull RenderType render,
    @Nullable TextureAtlasSprite sprite,
    @NotNull ResourceLocation texture,
    int light,
    int color,
    int overlay,
    int outline,
    AtomicInteger order
) {}
