package dev.kikugie.elytratrims.api.render;

import net.minecraft.resources.ResourceLocation;

/**
 * Represents a unique identifier for {@link ETDecorator}s.
 * @param identifier Display name for the identifier.
 * @param method The rendering stage.
 */
public record ETRendererID(ResourceLocation identifier, ETRenderMethod method) {}
