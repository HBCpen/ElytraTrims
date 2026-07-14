package dev.kikugie.elytratrims.api.render;

import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jetbrains.annotations.NotNull;

public interface ETDecorator {
    @NotNull ETRendererID getType();

    boolean render(@NotNull ETRenderParameters parameters, @NotNull SubmitNodeCollector collector);
}
