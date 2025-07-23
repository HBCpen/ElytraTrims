package dev.kikugie.elytratrims.ep;

import dev.kikugie.elytratrims.api.ETCommonInitializer;
import dev.kikugie.elytratrims.api.interaction.ETInteractionAPI;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;

import static dev.kikugie.elytratrims.CommonsKt.elytratrims;
import static dev.kikugie.elytratrims.recipe.cauldron.CauldronInteractionsKt.*;

@Entrypoint
public class ETCommonEntrypoint implements ETCommonInitializer {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public void onInitializeCommonET() {
        ETInteractionAPI.registerCauldronInteraction(elytratrims("remove_dye"), REMOVE_COLOR);
        ETInteractionAPI.registerCauldronInteraction(elytratrims("remove_glow"), REMOVE_GLOW);
        ETInteractionAPI.registerCauldronInteraction(elytratrims("remove_pattern"), REMOVE_PATTERN);
    }
}
