package dev.kikugie.elytratrims.api.interaction;

import dev.kikugie.elytratrims.api.impl.ETInteractionAPIImpl;
import org.jetbrains.annotations.NotNull;

public interface ETInteractionAPI {
    static void registerCauldronInteraction(@NotNull net.minecraft.resources.Identifier identifier, @NotNull ETCauldronInteraction interaction) {
        ETInteractionAPIImpl.registerCauldronInteraction(identifier, interaction);
    }
}
