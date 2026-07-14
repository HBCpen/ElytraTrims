package dev.kikugie.elytratrims.api.render;

import dev.kikugie.elytratrims.render.ETRenderingActionsKt;
import kotlin.sequences.SequencesKt;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@ApiStatus.NonExtendable
public interface ETRenderingAPI {
    static @NotNull List<@NotNull ETDecorator> registeredDecorators() {
        return SequencesKt.toList(ETRenderingActionsKt.allDecorators());
    }

    static @NotNull ETRendererID registerDecorator(@NotNull ETDecorator instance) {
        ETRenderingActionsKt.registerDecorator(instance, List::addLast);
        return instance.getType();
    }

    static @NotNull ETRendererID registerDecoratorBefore(@NotNull ETDecorator instance, @NotNull net.minecraft.resources.Identifier before) {
        ETRenderingActionsKt.registerDecorator(instance, (list, it) -> {
            int index = ETRenderingActionsKt.indexOfId(list, before);
            if (index < 0) throw new IllegalStateException("Decorator %s is not registered yet".formatted(before));
            else list.add(index, it);
        });
        return instance.getType();
    }

    static @NotNull ETRendererID registerDecoratorAfter(@NotNull ETDecorator instance, @NotNull net.minecraft.resources.Identifier after) {
        ETRenderingActionsKt.registerDecorator(instance, (list, it) -> {
            int index = ETRenderingActionsKt.indexOfId(list, after);
            if (index < 0) throw new IllegalStateException("Decorator %s is not registered yet".formatted(after));
            else if (index == list.size() - 1) list.addLast(it);
            else list.add(index + 1, it);
        });
        return instance.getType();
    }

    static void wrapRenderParameters(@NotNull ETRendererID type, @NotNull Consumer<ETRenderParameters> action) {
        ETRenderingActionsKt.wrapParametersCallback(type, action);
    }

    static void wrapRenderParameters(@NotNull Consumer<ETRenderParameters> action) {
        var iterator = ETRenderingActionsKt.allDecorators().iterator();
        while (iterator.hasNext()) wrapRenderParameters(iterator.next().getType(), action);
    }

    static void wrapRenderCall(@NotNull ETRendererID type, @NotNull Callback callback) {
        ETRenderingActionsKt.wrapRenderCallback(type, callback);
    }

    static void wrapRenderCall(@NotNull Callback callback) {
        var iterator = ETRenderingActionsKt.allDecorators().iterator();
        while (iterator.hasNext()) wrapRenderCall(iterator.next().getType(), callback);
    }

    @FunctionalInterface
    interface Callback {
        boolean render(
            @NotNull ETRenderParameters parameters,
            @NotNull SubmitNodeCollector collector,
            @NotNull BiFunction<@NotNull ETRenderParameters, @NotNull SubmitNodeCollector, @NotNull Boolean> operation
        );
    }
}
