package dev.kikugie.elytratrims.api;

import dev.kikugie.elytratrims.api.impl.ETRenderingAPIImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface ETRenderingAPI {
    @FunctionalInterface
    interface Callback {
        boolean render(ETRenderParameters parameters, Function<ETRenderParameters, Boolean> operation);
    }

    static @NotNull Map<@NotNull ETRendererType, @NotNull ETDecorator> getRegisteredDecorators() {
        return ETRenderingAPIImpl.getDecorators();
    }

    static void wrapRenderParameters(@NotNull UnaryOperator<@NotNull ETRenderParameters> parameters) {
        for (ETRendererType type : getRegisteredDecorators().keySet()) wrapRenderParameters(type, parameters);
    }

    static void wrapRenderParameters(@NotNull ETRendererType type, @NotNull UnaryOperator<@NotNull ETRenderParameters> parameters) {
        ETRenderingAPIImpl.wrapRenderParameters(type, parameters);
    }

    static void wrapRenderCall(@NotNull Callback callback) {
        for (ETRendererType type : getRegisteredDecorators().keySet()) wrapRenderCall(type, callback);
    }

    static void wrapRenderCall(@NotNull ETRendererType type, @NotNull Callback callback) {
        ETRenderingAPIImpl.wrapRenderCall(type, callback);
    }

    static @NotNull ETRendererType registerDecorator(@NotNull ETRendererType type, @NotNull ETDecorator instance) {
        return ETRenderingAPIImpl.registerDecorator(type, instance);
    }
}
