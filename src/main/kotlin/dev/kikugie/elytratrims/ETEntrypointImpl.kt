package dev.kikugie.elytratrims

import dev.kikugie.elytratrims.debug.registerServerDebugCommands
import dev.kikugie.elytratrims.recipe.cauldron.ETCauldronInteractions
import dev.kikugie.elytratrims.render.ElytraRenderLayers

fun clientInit() {
    // Inits the value
    ElytraRenderLayers.GATEWAY_MASKED
    if (ModData.isModLoaded("trimica")) registerTrimicaCompat()
    if (ModData.isModLoaded("iris")) registerIrisCompat()
}

fun commonInit() {
    ETCauldronInteractions.register()
    //? if fabric {
    if (ModData.isDevEnv) net.fabricmc.fabric.api.command.v2
        .CommandRegistrationCallback.EVENT.register(::registerServerDebugCommands)
    //?}
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
