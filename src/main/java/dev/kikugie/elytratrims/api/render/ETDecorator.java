package dev.kikugie.elytratrims.api.render;

import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * Renderer for the elytra decorations.
 * Instances should be registered in {@link ETRenderingAPI#registerDecorator(ETDecorator)}
 * as singletons.
 */
public interface ETDecorator {
    /**
     * The renderer type as registered in {@link ETRenderingAPI#registerDecorator(ETDecorator)}.
     */
    @NotNull ETRendererID getType();

    /**
     * Configures render parameters before they are processed by {@link ETRenderingAPI#wrapRenderParameters(UnaryOperator)} functions.
     * If you expect the render call to be cancelled, you can return the original instance.
     *
     * @param original The process parameters provided by the base elytra renderer.
     * @return The modified instance.
     */
    @NotNull ETRenderParameters prepare(@NotNull ETRenderParameters original);

    /**
     * Executes the rendering for the model.
     * This function is passed to the wrappers added with {@link ETRenderingAPI#wrapRenderCall(ETRenderingAPI.Callback)}.
     *
     * @param parameters Processed render parameters.
     * @return {@code true} if something was rendered, or {@code false} if it was cancelled.
     */
    //? if >=1.21.9 {
    boolean render(@NotNull ETRenderParameters parameters, @NotNull net.minecraft.client.renderer.SubmitNodeCollector collector);
    //?} else
    //boolean render(@NotNull ETRenderParameters parameters, @NotNull net.minecraft.client.renderer.MultiBufferSource source);

    /**
     * Resets the internal state of the renderer.
     * <br>
     * This may be used to clear cached and known missing sprites.
     */
    default void reset() {}
}
