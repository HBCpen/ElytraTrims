package dev.kikugie.elytratrims;

//? if neoforge {
/*import dev.kikugie.elytratrims.api.ETClientInitializer;
import dev.kikugie.elytratrims.api.ETCommonInitializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import java.util.ServiceLoader;

@Mod("elytratrims")
public class NeoforgeEntrypoint {
    public NeoforgeEntrypoint(IEventBus bus, ModContainer container) {
        ETEntrypointImpl.commonInit(ServiceLoader.load(ETCommonInitializer.class));
        if (ModData.INSTANCE.isClient())
            ETEntrypointImplKt.clientInit(ServiceLoader.load(ETClientInitializer.class));
    }
}
*///?}
