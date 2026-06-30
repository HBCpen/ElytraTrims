package dev.kikugie.elytratrims;

//? if fabric {
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;

@Entrypoint
public class FabricCommonEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        ETEntrypointImplKt.commonInit();
    }
}
//?}