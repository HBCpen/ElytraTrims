package dev.kikugie.elytratrims.ep;

import dev.kikugie.elytratrims.ModData;
import dev.kikugie.elytratrims.api.ETClientInitializer;
import dev.kikugie.elytratrims.api.render.ETRenderingAPI;
import dev.kikugie.elytratrims.render.impl.*;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

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

        if (ModData.INSTANCE.isModLoaded("sodium")) registerSodiumCompat();
    }

    private static void registerSodiumCompat() {
        var logger = LoggerFactory.getLogger("ETSodiumCompat");
        var hasReported = new AtomicBoolean(false);

        ETRenderingAPI.wrapRenderCall(ETBadAppleRenderer.type, ((parameters, operation) -> {
            if (parameters.sprite() != null && !hasReported.get()) try {
                //noinspection UnstableApiUsage
                SpriteUtil.INSTANCE.markSpriteActive(parameters.sprite());
            } catch (Throwable e) {
                hasReported.set(true);
                logger.error("Failed to mark sprite as active", e);
            }
            return operation.apply(parameters);
        }));
    }
}
