package dev.kikugie.elytratrims

import com.bawnorton.trimica.api.BaseTextureInterceptor
import com.bawnorton.trimica.api.TrimicaApi
import dev.kikugie.elytratrims.api.ETClientInitializer
import dev.kikugie.elytratrims.api.ETCommonInitializer
import dev.kikugie.elytratrims.api.interaction.ETInteractionAPI.registerCauldronInteraction
import dev.kikugie.elytratrims.api.render.ETRenderingAPI.registerDecorator
import dev.kikugie.elytratrims.api.render.ETRenderingAPI.wrapRenderCall
import dev.kikugie.elytratrims.recipe.cauldron.ETCauldronInteraction
import dev.kikugie.elytratrims.recipe.cauldron.REMOVE_COLOR
import dev.kikugie.elytratrims.recipe.cauldron.REMOVE_GLOW
import dev.kikugie.elytratrims.recipe.cauldron.REMOVE_PATTERNS
import dev.kikugie.elytratrims.render.ElytraRenderLayers
import dev.kikugie.elytratrims.render.impl.*
import dev.kikugie.elytratrims.resource.ETTags
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.equipment.trim.ArmorTrim
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

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

    @JvmStatic fun clientEP() {
        registerDecorator(ETGatewayRenderer)
        registerDecorator(ETColorRenderer)
        registerDecorator(ETBadAppleRenderer)
        registerDecorator(ETPatternsRenderer)
        registerDecorator(ETTrimsRenderer)

        if (ModData.isModLoaded("sodium")) registerSodiumCompat()
        if (ModData.isModLoaded("trimica")) registerTrimicaCompat()
    }

    @JvmStatic fun commonEP() {
        registerCauldronInteraction(elytratrims("remove_dye"), REMOVE_COLOR)
        registerCauldronInteraction(elytratrims("remove_glow"), REMOVE_GLOW)
        registerCauldronInteraction(elytratrims("remove_pattern"), REMOVE_PATTERNS)
        // registerCauldronInteraction(elytratrims("remove_touhou"), REMOVE_TOUHOU)
    }

    @Suppress("UnstableApiUsage")
    private fun registerSodiumCompat() {
        val logger = LoggerFactory.getLogger("ETSodiumCompat")
        val reported = AtomicBoolean(false)

        wrapRenderCall(ETBadAppleRenderer.type) { parameters, collector, operation ->
            if (parameters.sprite != null && !reported.get()) parameters
                .runCatching { SpriteUtil.INSTANCE.markSpriteActive(sprite!!) }
                .onFailure { reported.set(true); logger.error("Failed to mark sprite as active", it) }
            operation.apply(parameters, collector)
        }
    }

    private fun registerTrimicaCompat() = TrimicaApi.getInstance().registerBaseTextureInterceptor(1000, object : BaseTextureInterceptor {
        override fun interceptItemTexture(expectedBaseTexture: Identifier?, itemWithTrim: ItemStack, armourTrim: ArmorTrim): Identifier? =
            if (!itemWithTrim.`is`(ETTags.ELYTRA_DECORATABLE)) expectedBaseTexture
            else with(armourTrim.pattern.value().assetId) {
                if (namespace != "minecraft") vanilla("textures/trims/items/wings_trim.png")
                else withPath { "textures/trims/items/wings/$it.png" }
            }
    })
}