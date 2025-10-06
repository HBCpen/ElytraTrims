package dev.kikugie.elytratrims.api.render;

import dev.kikugie.elytratrims.api.impl.ETRenderingAPIUtilsKt;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ETRenderingAPIUtils {
    /**
     * Gets the increased light map value to be used if elytra has the glow tag.
     *
     * @param parameters Used render parameters.
     * @return The effective light map value.
     */
    static int getEffectiveLight(ETRenderParameters parameters) {
        return ETRenderingAPIUtilsKt.getEffectiveLight(parameters);
    }

    /**
     * Shortcut to the standard render call.
     *
     * @param parameters Used render parameters.
     */
    static void submitToCollector(ETRenderParameters parameters, SubmitNodeCollector collector) {
        ETRenderingAPIUtilsKt.submitToCollector(parameters, collector);
    }
}
