package dev.kikugie.elytratrims.api.render;

import dev.kikugie.elytratrims.render.ETRenderingActionsKt;
import kotlin.sequences.SequencesKt;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@ApiStatus.NonExtendable
public interface ETRenderingAPI {
    static List<ETDecorator> registeredDecorators() {
        return SequencesKt.toList(ETRenderingActionsKt.allDecorators());
    }

    static ETRendererID registerDecorator(ETDecorator instance) {
        ETRenderingActionsKt.registerDecorator(instance, List::addLast);
        return instance.getType();
    }

    static ETRendererID registerDecoratorBefore(ETDecorator instance, Identifier before) {
        ETRenderingActionsKt.registerDecorator(instance, (list, it) -> {
            int index = ETRenderingActionsKt.indexOfId(list, before);
            if (index < 0) throw new IllegalStateException("Decorator %s is not registered yet".formatted(before));
            else list.add(index, it);
        });
        return instance.getType();
    }

    static ETRendererID registerDecoratorAfter(ETDecorator instance, Identifier after) {
        ETRenderingActionsKt.registerDecorator(instance, (list, it) -> {
            int index = ETRenderingActionsKt.indexOfId(list, after);
            if (index < 0) throw new IllegalStateException("Decorator %s is not registered yet".formatted(after));
            else if (index == list.size() - 1) list.addLast(it);
            else list.add(index + 1, it);
        });
        return instance.getType();
    }

    static void wrapRenderParameters(ETRendererID type, Consumer<ETRenderParameters> action) {
        ETRenderingActionsKt.wrapParametersCallback(type, action);
    }

    @Deprecated // The new API modifies provided parameters in-place
    static void wrapRenderParameters(ETRendererID type, UnaryOperator<ETRenderParameters> action) {
        ETRenderingActionsKt.wrapParametersCallback(type, it -> it.copyFrom(action.apply(it)));
    }

    static void wrapRenderParameters(Consumer<ETRenderParameters> action) {
        var iterator = ETRenderingActionsKt.allDecorators().iterator();
        while (iterator.hasNext()) ETRenderingActionsKt.wrapParametersCallback(iterator.next().getType(), action);
    }

    @Deprecated // The new API modifies provided parameters in-place
    static void wrapRenderParameters(UnaryOperator<ETRenderParameters> action) {
        var iterator = ETRenderingActionsKt.allDecorators().iterator();
        while (iterator.hasNext()) ETRenderingActionsKt.wrapParametersCallback(iterator.next().getType(), it -> it.copyFrom(action.apply(it)));
    }

    static void wrapRenderCall(ETRendererID type, Callback callback) {
        ETRenderingActionsKt.wrapRenderCallback(type, callback);
    }

    static void wrapRenderCall(Callback callback) {
        var iterator = ETRenderingActionsKt.allDecorators().iterator();
        while (iterator.hasNext()) wrapRenderCall(iterator.next().getType(), callback);
    }

    @FunctionalInterface
    interface Callback {
        boolean render(
            ETRenderParameters parameters,
            SubmitNodeCollector collector,
            BiFunction<ETRenderParameters, SubmitNodeCollector, Boolean> operation
        );
    }
}
