package dev.kikugie.elytratrims.api.render;

import dev.kikugie.elytratrims.render.ETRenderingActionsKt;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.NonExtendable
public interface ETRenderingAPIUtils {
    static void submitToCollector(ETRenderParameters parameters, SubmitNodeCollector collector) {
        ETRenderingActionsKt.submitToCollector(parameters, collector);
    }

    static void prepareParameters(ETRendererID id, ETRenderParameters parameters) {
        ETRenderingActionsKt.prepare(parameters, id);
    }

    static boolean renderDecorator(ETRendererID id, ETRenderParameters parameters, SubmitNodeCollector collector) {
        return ETRenderingActionsKt.render(parameters, id, collector);
    }
}
