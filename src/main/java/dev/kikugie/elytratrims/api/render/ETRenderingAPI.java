package dev.kikugie.elytratrims.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kikugie.elytratrims.api.impl.ETRenderingAPIImpl;
import dev.kikugie.elytratrims.api.impl.ETRenderingAPIUtilsKt;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface ETRenderingAPI {
    @FunctionalInterface
    interface Callback {
        /**
         * Wraps the rendering call for a decorator rendering function.
         *
         * @param parameters Processed parameters to be used in rendering.
         * @param operation The previous rendering function.
         *                  This may either be the original {@code renderToBuffer()} call,
         *                  or the call to a previous callback.
         * @return {@code true} if the something was rendered (same for the provided operation).
         * This value is used to determine if the original elytra renderer should be cancelled.
         */
        boolean render(ETRenderParameters parameters, Function<ETRenderParameters, Boolean> operation);
    }

    /**
     * @return Immutable copy of the registered decorators map.
     */
    static @NotNull List<@NotNull ETDecorator> registeredDecorators() {
        return ETRenderingAPIImpl.getDecorators();
    }

    /**
     * Registers the decoration layer renderer if it's not already present.
     *
     * @param type The unique identifier for this decorator.
     * @param instance The decorator instance. It's recommended for it to be a singleton.
     * @throws IllegalArgumentException If the provided type is already registered.
     * @return The provided identifier.
     */
    static @NotNull ETRendererID registerDecorator(@NotNull ETRendererID type, @NotNull ETDecorator instance) {
        return ETRenderingAPIImpl.registerDecorator(type, instance, List::addLast);
    }

    /**
     * Registers the decoration layer renderer before the specified one.
     *
     * @param type The unique identifier for this decorator.
     * @param instance The decorator instance. It's recommended for it to be a singleton.
     * @throws IllegalArgumentException If the provided type is already registered.
     * @throws IllegalStateException If the required decorator is not registered yet,
     * in which case you should add an is-mod-loaded check and trigger registration afterwards.
     * @return The provided identifier.
     */
    static @NotNull ETRendererID registerDecoratorBefore(@NotNull ETRendererID type, @NotNull ETDecorator instance, ResourceLocation before) {
        return ETRenderingAPIImpl.registerDecorator(type, instance, (list, it) -> {
            int index = ETRenderingAPIUtilsKt.indexOfId(list, before);
            if (index < 0) throw new IllegalStateException("Decorator %s is not registered yet".formatted(before));
            else list.add(index, it);
        });
    }

    /**
     * Registers the decoration layer renderer after the specified one.
     *
     * @param type The unique identifier for this decorator.
     * @param instance The decorator instance. It's recommended for it to be a singleton.
     * @throws IllegalArgumentException If the provided type is already registered.
     * @throws IllegalStateException If the required decorator is not registered yet,
     * in which case you should add an is-mod-loaded check and trigger registration afterwards.
     * @return The provided identifier.
     */
    static @NotNull ETRendererID registerDecoratorAfter(@NotNull ETRendererID type, @NotNull ETDecorator instance, ResourceLocation after) {
        return ETRenderingAPIImpl.registerDecorator(type, instance, (list, it) -> {
            int index = ETRenderingAPIUtilsKt.indexOfId(list, after);
            if (index < 0) throw new IllegalStateException("Decorator %s is not registered yet".formatted(after));
            else if (index == list.size() - 1) list.addLast(it);
            else list.add(index + 1, it);
        });
    }

    /**
     * Modifies parameters passed to all decorator rendering functions.
     *
     * @param parameters Parameter modification function
     * @see ETRenderingAPIUtilsKt#copy(ETRenderParameters, Model, ItemStack, PoseStack, MultiBufferSource, TextureAtlasSprite, ResourceLocation, int, int, Function)
     */
    static void wrapRenderParameters(@NotNull UnaryOperator<@NotNull ETRenderParameters> parameters) {
        for (ETDecorator renderer : registeredDecorators()) wrapRenderParameters(renderer.getType(), parameters);
    }

    /**
     * Modifies parameters passed to the specified decorator rendering function.
     *
     * @param parameters Parameter modification function
     * @see ETRenderingAPIUtilsKt#copy(ETRenderParameters, Model, ItemStack, PoseStack, MultiBufferSource, TextureAtlasSprite, ResourceLocation, int, int, Function)
     */
    static void wrapRenderParameters(@NotNull ETRendererID type, @NotNull UnaryOperator<@NotNull ETRenderParameters> parameters) {
        ETRenderingAPIImpl.wrapRenderParameters(type, parameters);
    }

    /**
     * Wraps rendering calls for all registered decorators.
     * <br>
     * Wrappers stack when several are registered.
     *
     * @param callback Render wrapping function.
     */
    static void wrapRenderCall(@NotNull Callback callback) {
        for (ETDecorator renderer : registeredDecorators()) wrapRenderCall(renderer.getType(), callback);
    }

    /**
     * Wraps rendering calls for the specified type
     * <br>
     * Wrappers stack when several are registered.
     *
     * @param type The identifier for the decorator to wrap.
     * @param callback Render wrapping function.
     */
    static void wrapRenderCall(@NotNull ETRendererID type, @NotNull Callback callback) {
        ETRenderingAPIImpl.wrapRenderCall(type, callback);
    }
}
