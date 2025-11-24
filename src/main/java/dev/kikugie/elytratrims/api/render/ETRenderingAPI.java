package dev.kikugie.elytratrims.api.render;

import dev.kikugie.elytratrims.api.impl.ETRenderingAPIImpl;
import dev.kikugie.elytratrims.api.impl.ETRenderingAPIUtilsKt;
import kotlin.sequences.SequencesKt;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

@ApiStatus.NonExtendable
public interface ETRenderingAPI {
    @FunctionalInterface
    interface Callback {
        /**
         * Wraps the rendering call for a decorator rendering function.
         *
         * @param parameters Processed parameters to be used in rendering.
         * @param collector On 1.21.9+ the render call collector. On older versions always null for function compatibility.
         * @param operation The previous rendering function.
         *                  This may either be the original {@code renderToBuffer()} call,
         *                  or the call to a previous callback.
         * @return {@code true} if the something was rendered (same for the provided operation).
         * This value is used to determine if the original elytra renderer should be cancelled.
         */
        //? if >=1.21.9 {
        boolean render(@NotNull ETRenderParameters parameters,
                         @NotNull net.minecraft.client.renderer.SubmitNodeCollector collector,
                         @NotNull BiFunction<ETRenderParameters, net.minecraft.client.renderer.SubmitNodeCollector, Boolean> operation);
        //?} else {
        /*boolean render(@NotNull ETRenderParameters parameters,
                       @NotNull net.minecraft.client.renderer.MultiBufferSource collector,
                       @NotNull BiFunction<ETRenderParameters, net.minecraft.client.renderer.MultiBufferSource, Boolean> operation);
        *///?}
    }

    /**
     * @return Immutable copy of the registered decorators map.
     */
    static @NotNull List<ETDecorator> registeredDecorators() {
        return SequencesKt.toList(ETRenderingAPIImpl.getDecorators());
    }

    /**
     * Registers the decoration layer renderer if it's not already present.
     *
     * @param instance The decorator instance. It's recommended for it to be a singleton.
     * @throws IllegalArgumentException If the provided type is already registered.
     * @return The provided identifier.
     */
    static @NotNull ETRendererID registerDecorator(@NotNull ETDecorator instance) {
        return ETRenderingAPIImpl.registerDecorator(instance, List::addLast);
    }

    /**
     * Registers the decoration layer renderer before the specified one.
     *
     * @param instance The decorator instance. It's recommended for it to be a singleton.
     * @throws IllegalArgumentException If the provided type is already registered.
     * @throws IllegalStateException If the required decorator is not registered yet,
     * in which case you should add an is-mod-loaded check and trigger registration afterwards.
     * @return The provided identifier.
     */
    static @NotNull ETRendererID registerDecoratorBefore(@NotNull ETDecorator instance, @NotNull net.minecraft.resources.Identifier before) {
        return ETRenderingAPIImpl.registerDecorator(instance, (list, it) -> {
            int index = ETRenderingAPIUtilsKt.indexOfId(list, before);
            if (index < 0) throw new IllegalStateException("Decorator %s is not registered yet".formatted(before));
            else list.add(index, it);
        });
    }

    /**
     * Registers the decoration layer renderer after the specified one.
     *
     * @param instance The decorator instance. It's recommended for it to be a singleton.
     * @throws IllegalArgumentException If the provided type is already registered.
     * @throws IllegalStateException If the required decorator is not registered yet,
     * in which case you should add an is-mod-loaded check and trigger registration afterwards.
     * @return The provided identifier.
     */
    static @NotNull ETRendererID registerDecoratorAfter(@NotNull ETDecorator instance, @NotNull net.minecraft.resources.Identifier after) {
        return ETRenderingAPIImpl.registerDecorator(instance, (list, it) -> {
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
     */
    static void wrapRenderParameters(@NotNull UnaryOperator<ETRenderParameters>  parameters) {
        for (ETDecorator renderer : registeredDecorators()) wrapRenderParameters(renderer.getType(), parameters);
    }

    /**
     * Modifies parameters passed to the specified decorator rendering function.
     *
     * @param parameters Parameter modification function
     */
    static void wrapRenderParameters(@NotNull ETRendererID type, @NotNull UnaryOperator<ETRenderParameters> parameters) {
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
