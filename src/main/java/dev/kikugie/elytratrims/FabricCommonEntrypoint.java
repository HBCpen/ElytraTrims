package dev.kikugie.elytratrims;

//? if fabric {
import dev.kikugie.elytratrims.api.ETCommonInitializer;
import dev.kikugie.elytratrims.recipe.ETCauldronInteraction;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

@Entrypoint
public class FabricCommonEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        ETCauldronInteraction.register();
        var entrypoints = FabricLoader.getInstance()
            .getEntrypointContainers(ETCommonInitializer.EP_NAME, ETCommonInitializer.class);
        for (EntrypointContainer<ETCommonInitializer> it : entrypoints)
            it.getEntrypoint().onInitializeCommonET();
    }
}
//?}