package dev.kikugie.elytratrims.ep;

import dev.kikugie.elytratrims.ETEntrypointImplKt;
import dev.kikugie.elytratrims.api.ETClientInitializer;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;

/*? if fabric*/@Entrypoint
public class ETClientEntrypoint implements ETClientInitializer {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public void onInitializeClientET() {
        ETEntrypointImplKt.clientEP();
    }
}
