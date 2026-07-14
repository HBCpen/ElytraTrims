package dev.kikugie.elytratrims;

//? if fabric {
import dev.kikugie.elytratrims.api.ETClientInitializer;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.ArrayList;

@Entrypoint
public class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        var containers = FabricLoader.getInstance()
            .getEntrypointContainers(ETClientInitializer.EP_NAME, ETClientInitializer.class);
        var entrypoints = new ArrayList<ETClientInitializer>(containers.size());
        for (EntrypointContainer<ETClientInitializer> it : containers)
            entrypoints.add(it.getEntrypoint());
        ETEntrypointImplKt.clientInit(entrypoints);
    }
}
//?}