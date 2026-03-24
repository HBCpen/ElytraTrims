package dev.kikugie.elytratrims

import dev.kikugie.elytratrims.api.ETClientInitializer
import dev.kikugie.elytratrims.api.ETCommonInitializer
import dev.kikugie.elytratrims.api.interaction.ETInteractionAPI.registerCauldronInteraction
import dev.kikugie.elytratrims.api.render.ETRenderingAPI.registerDecorator
import dev.kikugie.elytratrims.debug.registerServerDebugCommands
import dev.kikugie.elytratrims.recipe.cauldron.ETCauldronInteraction
import dev.kikugie.elytratrims.recipe.cauldron.REMOVE_COLOR
import dev.kikugie.elytratrims.recipe.cauldron.REMOVE_GLOW
import dev.kikugie.elytratrims.recipe.cauldron.REMOVE_PATTERNS
import dev.kikugie.elytratrims.render.ElytraRenderLayers
import dev.kikugie.elytratrims.render.impl.*

object ETEntrypointImpl {
    @JvmStatic fun clientInit(entrypoints: Iterable<ETClientInitializer>) {
        entrypoints.sortedByDescending(ETClientInitializer::priority)
            .forEach(ETClientInitializer::onInitializeClientET)

        // Inits the value
        ElytraRenderLayers.GATEWAY_MASKED
    }

    @JvmStatic fun commonInit(entrypoints: Iterable<ETCommonInitializer>) {
        entrypoints.sortedByDescending(ETCommonInitializer::priority)
            .forEach(ETCommonInitializer::onInitializeCommonET)

        ETCauldronInteraction.register()
        //? if fabric {
        if (ModData.isDevEnv) net.fabricmc.fabric.api.command.v2
            .CommandRegistrationCallback.EVENT.register(::registerServerDebugCommands)
        //?}
    }

    @JvmStatic fun clientEP() {
        registerDecorator(ETGatewayRenderer)
        registerDecorator(ETColorRenderer)
        registerDecorator(ETBadAppleRenderer)
        registerDecorator(ETPatternsRenderer)
        registerDecorator(ETTrimsRenderer)

        if (ModData.isModLoaded("trimica")) registerTrimicaCompat()
        if (ModData.isModLoaded("iris")) registerIrisCompat()
    }

    @JvmStatic fun commonEP() {
        registerCauldronInteraction(elytratrims("remove_dye"), REMOVE_COLOR)
        registerCauldronInteraction(elytratrims("remove_glow"), REMOVE_GLOW)
        registerCauldronInteraction(elytratrims("remove_pattern"), REMOVE_PATTERNS)
        // registerCauldronInteraction(elytratrims("remove_touhou"), REMOVE_TOUHOU)
    }

    private fun registerTrimicaCompat() {
        //? if trimica {
        /*com.bawnorton.trimica.api.TrimicaApi.getInstance().registerBaseTextureInterceptor(
            1000, object : com.bawnorton.trimica.api.BaseTextureInterceptor {
                override fun interceptItemTexture(expectedBaseTexture: Identifier?, itemWithTrim: ItemStack, armourTrim: ArmorTrim): Identifier? =
                    if (!itemWithTrim.`is`(ETTags.ELYTRA_DECORATABLE)) expectedBaseTexture
                    else with(armourTrim.pattern.value().assetId) {
                        if (namespace != "minecraft") vanilla("textures/trims/items/wings_trim.png")
                        else withPath { "textures/trims/items/wings/$it.png" }
                    }
            })
        *///?}
    }

    private fun registerIrisCompat() {
        //? if iris {
        /*IrisApi.getInstance().assignPipeline(ElytraRenderLayers.GATEWAY_MASKED, IrisProgram.ENTITIES_TRANSLUCENT)
        *///?}
    }
}