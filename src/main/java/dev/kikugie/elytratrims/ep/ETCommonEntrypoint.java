package dev.kikugie.elytratrims.ep;

import dev.kikugie.elytratrims.ETEntrypointImplKt;
import dev.kikugie.elytratrims.api.ETCommonInitializer;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;

/*? if fabric*/@Entrypoint
public class ETCommonEntrypoint implements ETCommonInitializer {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public void onInitializeCommonET() {
        ETEntrypointImplKt.commonEP();
    }
}
