package dev.kikugie.elytratrims;

//? if fabric {
import dev.kikugie.elytratrims.api.ETClientInitializer;
import dev.kikugie.elytratrims.api.impl.EntrypointUtilsKt;
import dev.kikugie.elytratrims.render.ElytraRenderLayers;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.ArrayList;

@Entrypoint
public class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        var $ = ElytraRenderLayers.GATEWAY_MASKED; // Instantiate it
        var containers = FabricLoader.getInstance()
            .getEntrypointContainers(ETClientInitializer.EP_NAME, ETClientInitializer.class);
        var entrypoints = new ArrayList<ETClientInitializer>(containers.size());
        for (EntrypointContainer<ETClientInitializer> it : containers)
            entrypoints.add(it.getEntrypoint());
        for (ETClientInitializer it : EntrypointUtilsKt.sortClient(entrypoints))
            it.onInitializeClientET();
    }
}
//?}