package dev.kikugie.elytratrims;

//? if fabric {
import dev.kikugie.elytratrims.api.ETCommonInitializer;
import dev.kikugie.elytratrims.api.impl.EntrypointUtilsKt;
import dev.kikugie.elytratrims.recipe.ETCauldronInteraction;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.ArrayList;

@Entrypoint
public class FabricCommonEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        ETCauldronInteraction.register();
        var containers = FabricLoader.getInstance()
            .getEntrypointContainers(ETCommonInitializer.EP_NAME, ETCommonInitializer.class);
        var entrypoints = new ArrayList<ETCommonInitializer>(containers.size());
        for (EntrypointContainer<ETCommonInitializer> it : containers)
            entrypoints.add(it.getEntrypoint());
        for (ETCommonInitializer it : EntrypointUtilsKt.sortCommon(entrypoints))
            it.onInitializeCommonET();
    }
}
//?}