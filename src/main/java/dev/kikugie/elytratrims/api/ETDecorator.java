package dev.kikugie.elytratrims.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Renderer for the elytra decorations.
 * Instances should be registered in {@link ETRenderingAPI#registerDecorator(ETRendererID, ETDecorator)}
 * as singletons.
 */
public interface ETDecorator {
    /**
     * The renderer type as registered in {@link ETRenderingAPI#registerDecorator(ETRendererID, ETDecorator)}.
     */
    @NotNull ETRendererID getType();

    /**
     * Configures render parameters before they are processed by {@link ETRenderingAPI#wrapRenderParameters(UnaryOperator)} functions.
     * If you expect the {@link #render(ETRenderParameters)} call to be cancelled, you can return the original instance.
     * Otherwise, you must specify {@link ETRenderParameters#converter()}, as by default it doesn't know how to
     * create the vertex consumer and will throw {@link UnsupportedOperationException} at runtime.
     *
     * @param original The process parameters provided by the base elytra renderer.
     * @return The modified instance.
     * @see dev.kikugie.elytratrims.api.impl.ETRenderingAPIUtilsKt#copy(ETRenderParameters, Model, ItemStack, PoseStack, MultiBufferSource, TextureAtlasSprite, ResourceLocation, int, int, Function)
     * @see ETRenderingAPIUtils
     */
    @NotNull ETRenderParameters prepare(@NotNull ETRenderParameters original);

    /**
     * Executes the rendering for the model.
     * This function is passed to the wrappers added with {@link ETRenderingAPI#wrapRenderCall(ETRenderingAPI.Callback)}.
     *
     * @param parameters Processed render parameters.
     * @return {@code true} if something was rendered, or {@code false} if it was cancelled.
     */
    boolean render(@NotNull ETRenderParameters parameters);

    /**
     * Resets the internal state of the renderer.
     * <br>
     * This may be used to clear cached and known missing sprites.
     */
    default void reset() {}
}
