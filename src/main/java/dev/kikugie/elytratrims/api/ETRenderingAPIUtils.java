package dev.kikugie.elytratrims.api;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kikugie.elytratrims.api.impl.ETRenderingAPIUtilsKt;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public final class ETRenderingAPIUtils {
    /**
     * Gets the increased light map value to be used if elytra has the glow tag.
     *
     * @param parameters Used render parameters.
     * @return The effective light map value.
     */
    public static int getEffectiveLight(ETRenderParameters parameters) {
        return ETRenderingAPIUtilsKt.getEffectiveLight(parameters);
    }

    /**
     * Shortcut to the standard render call.
     *
     * @param parameters Used render parameters.
     */
    public static void renderToBuffer(ETRenderParameters parameters) {
        ETRenderingAPIUtilsKt.renderToBuffer(parameters);
    }

    /**
     * Returns new render parameters with the specified converter, as mentioned in {@link ETDecorator#prepare(ETRenderParameters)}.
     * For fine-tuned modifications, create a new instance yourself, or use {@link ETRenderingAPIUtilsKt#copy(ETRenderParameters, Model, ItemStack, PoseStack, MultiBufferSource, TextureAtlasSprite, ResourceLocation, int, int, Function)}
     * when working in Kotlin.
     *
     * @param parameters The original parameters.
     * @param converter VertexConsumer builder.
     * @return The modified parameters instance.
     */
    public static ETRenderParameters withConverter(ETRenderParameters parameters, Function<ETRenderParameters, VertexConsumer> converter) {
        return ETRenderingAPIUtilsKt.withConverter(parameters, converter);
    }
}
