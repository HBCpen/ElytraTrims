package dev.kikugie.elytratrims.api;

import org.jetbrains.annotations.NotNull;

public interface ETDecorator {
    @NotNull ETRendererType getType();

    @NotNull ETRenderParameters prepare(@NotNull ETRenderParameters original);

    boolean render(@NotNull ETRenderParameters parameters);

    default void reset() {}
}
