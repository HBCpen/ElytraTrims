package dev.kikugie.elytratrims;

//? if fabric {
import dev.kikugie.elytratrims.api.ETClientInitializer;
import dev.kikugie.elytratrims.render.ElytraRenderLayers;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

@Entrypoint
public class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        var $ = ElytraRenderLayers.GATEWAY_MASKED; // Instantiate it
        var entrypoints = FabricLoader.getInstance()
            .getEntrypointContainers(ETClientInitializer.EP_NAME, ETClientInitializer.class);
        for (EntrypointContainer<ETClientInitializer> it : entrypoints)
            it.getEntrypoint().onInitializeClientET();
    }
}
//?}