package dev.kikugie.elytratrims.api.interaction;

import dev.kikugie.elytratrims.api.impl.ETInteractionAPIImpl;
import net.minecraft.resources.ResourceLocation;

public interface ETInteractionAPI {
    static void registerCauldronInteraction(ResourceLocation identifier, ETCauldronInteraction interaction) {
        ETInteractionAPIImpl.registerCauldronInteraction(identifier, interaction);
    }
}
