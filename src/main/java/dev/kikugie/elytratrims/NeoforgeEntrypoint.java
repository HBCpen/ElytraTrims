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
    private static final ServiceLoader<ETClientInitializer> CLIENT_EPS = ServiceLoader.load(ETClientInitializer.class);
    private static final ServiceLoader<ETCommonInitializer> COMMON_EPS = ServiceLoader.load(ETCommonInitializer.class);

    public NeoforgeEntrypoint(IEventBus bus, ModContainer container) {
        for (ETCommonInitializer it : COMMON_EPS)
            it.onInitializeCommonET();
        if (ModData.INSTANCE.isClient()) for (ETClientInitializer it : CLIENT_EPS)
            it.onInitializeClientET();
    }
}
*///?}
