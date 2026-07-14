package dev.kikugie.elytratrims.api.render;

import dev.kikugie.elytratrims.render.ETRenderingActionsKt;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.NonExtendable
public interface ETRenderingAPIUtils {
    static void submitToCollector(@NotNull ETRenderParameters parameters, @NotNull SubmitNodeCollector collector) {
        ETRenderingActionsKt.submitToCollector(parameters, collector);
    }

    static void prepareParameters(@NotNull ETRendererID id, @NotNull ETRenderParameters parameters) {
        ETRenderingActionsKt.prepare(parameters, id);
    }

    static boolean renderDecorator(@NotNull ETRendererID id, @NotNull ETRenderParameters parameters, @NotNull SubmitNodeCollector collector) {
        return ETRenderingActionsKt.render(parameters, id, collector);
    }
}
