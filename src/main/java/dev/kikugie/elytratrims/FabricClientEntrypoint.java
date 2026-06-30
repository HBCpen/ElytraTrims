package dev.kikugie.elytratrims;

//? if fabric {
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;

@Entrypoint
public class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ETEntrypointImplKt.clientInit();
    }
}
//?}