package dev.kikugie.elytratrims

import dev.kikugie.elytratrims.api.ETClientInitializer
import dev.kikugie.elytratrims.api.ETCommonInitializer
import dev.kikugie.elytratrims.recipe.cauldron.ETCauldronInteraction
import dev.kikugie.elytratrims.render.ElytraRenderLayers
import dev.kikugie.elytratrims.resource.reload.ETDecoratorResetter
import net.minecraft.server.packs.resources.ReloadableResourceManager

object ETEntrypointImpl {
    @JvmStatic fun clientInit(entrypoints: Iterable<ETClientInitializer>) {
        entrypoints.sortedByDescending(ETClientInitializer::priority)
            .forEach(ETClientInitializer::onInitializeClientET)

        ElytraRenderLayers.GATEWAY_MASKED
        // FIXME: - Forge? - Forge.
//        (MINECRAFT.resourceManager as? ReloadableResourceManager)?.run {
//            registerReloadListener(ETDecoratorResetter)
//        }
    }

    @JvmStatic fun commonInit(entrypoints: Iterable<ETCommonInitializer>) {
        entrypoints.sortedByDescending(ETCommonInitializer::priority)
            .forEach(ETCommonInitializer::onInitializeCommonET)

        ETCauldronInteraction.register()
    }
}