package dev.kikugie.elytratrims.ep;

import cc.cassian.rrv.api.ReliableRecipeViewerClientPlugin;
import dev.kikugie.elytratrims.compat.rvv.ETRecipeViewerCompat;
import dev.kikugie.fletching_table.fabric.Entrypoint;

/*? if fabric*/@Entrypoint
public class ETRecipeViewerEntrypoint implements ReliableRecipeViewerClientPlugin {
    @Override
    public void onIntegrationInitialize() {
        ETRecipeViewerCompat.INSTANCE.initialize();
    }
}
