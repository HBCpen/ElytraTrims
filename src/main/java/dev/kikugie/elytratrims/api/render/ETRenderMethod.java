package dev.kikugie.elytratrims.api.render;

/**
 * Represents when the corresponding {@link ETDecorator} is invoked.
 */
public enum ETRenderMethod {
    /**
     * Renders the decorator before the main model.
     */
    PRE,
    /**
     * Renders the decorator in place of the main model.
     * If any decorator of this type returns `true`,
     * the elytra model won't be rendered.
     */
    OVERRIDE,
    /**
     * Renders the decorator after the main model.
     * If any decorator of this type returns `true`,
     * the vanilla trim rendering method will be cancelled.
     */
    POST
}
