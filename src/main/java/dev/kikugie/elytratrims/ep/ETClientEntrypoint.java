package dev.kikugie.elytratrims.ep;

import dev.kikugie.elytratrims.api.ETClientInitializer;
import dev.kikugie.elytratrims.api.render.ETRenderingAPI;
import dev.kikugie.elytratrims.render.impl.*;

/*? if fabric*/@dev.kikugie.fletching_table.annotation.fabric.Entrypoint
public class ETClientEntrypoint implements ETClientInitializer {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public void onInitializeClientET() {
        ETRenderingAPI.registerDecorator(ETGatewayRenderer.INSTANCE);
        ETRenderingAPI.registerDecorator(ETColorRenderer.INSTANCE);
        ETRenderingAPI.registerDecorator(ETBadAppleRenderer.INSTANCE);
        ETRenderingAPI.registerDecorator(ETPatternsRenderer.INSTANCE);
        ETRenderingAPI.registerDecorator(ETTrimsRenderer.INSTANCE);
    }
}
