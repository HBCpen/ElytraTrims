package dev.kikugie.elytratrims.api.render;

/**
 * Represents a unique identifier for {@link ETDecorator}s.
 * @param identifier Display name for the identifier.
 * @param method The rendering stage.
 */
public record ETRendererID(net.minecraft.resources.Identifier identifier, ETRenderMethod method) {}
