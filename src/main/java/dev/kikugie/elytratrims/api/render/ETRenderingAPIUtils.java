package dev.kikugie.elytratrims.api.render;

import dev.kikugie.elytratrims.api.impl.ETRenderingAPIUtilsKt;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.NonExtendable
public interface ETRenderingAPIUtils {
    /**
     * Gets the increased light map value to be used if elytra has the glow tag.
     *
     * @param parameters Used render parameters.
     * @return The effective light map value.
     */
    static int getEffectiveLight(@NotNull ETRenderParameters parameters) {
        return ETRenderingAPIUtilsKt.getEffectiveLight(parameters);
    }

    /**
     * Shortcut to the standard render call.
     *
     * @param parameters Used render parameters.
     */
    //? if >=1.21.9 {
    static void submitToCollector(@NotNull ETRenderParameters parameters, @NotNull net.minecraft.client.renderer.SubmitNodeCollector collector) {
        ETRenderingAPIUtilsKt.submitToCollector(parameters, collector);
    }
    //?} else {
    /*static void submitToCollector(@NotNull ETRenderParameters parameters, @NotNull net.minecraft.client.renderer.MultiBufferSource collector) {
        ETRenderingAPIUtilsKt.submitToCollector(parameters, collector);
    }
    *///?}
}
