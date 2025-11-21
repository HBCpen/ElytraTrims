package dev.kikugie.elytratrims.api.interaction;

import dev.kikugie.elytratrims.api.impl.ETInteractionAPIImpl;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public interface ETInteractionAPI {
    static void registerCauldronInteraction(@NotNull ResourceLocation identifier, @NotNull ETCauldronInteraction interaction) {
        ETInteractionAPIImpl.registerCauldronInteraction(identifier, interaction);
    }
}
