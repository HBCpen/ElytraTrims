package dev.kikugie.elytratrims.api.render;

import net.minecraft.client.renderer.SubmitNodeCollector;

public interface ETDecorator {
    ETRendererID getType();

    boolean render(ETRenderParameters parameters, SubmitNodeCollector collector);
}
