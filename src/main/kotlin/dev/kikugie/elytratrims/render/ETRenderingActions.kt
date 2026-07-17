package dev.kikugie.elytratrims.render

import com.mojang.blaze3d.vertex.PoseStack
import dev.kikugie.elytratrims.api.render.*
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import net.minecraft.client.model.Model
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.EquipmentClientInfo
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.BiConsumer
import java.util.function.Consumer

private typealias ParametersCallback = Consumer<ETRenderParameters>

private val PRE_RENDERERS: MutableList<ETDecorator> = mutableListOf()
private val REPL_RENDERERS: MutableList<ETDecorator> = mutableListOf()
private val POST_RENDERERS: MutableList<ETDecorator> = mutableListOf()

private val PARAMETER_WRAPPERS: MutableMap<ETRendererID, ParametersCallback> = mutableMapOf()
private val RENDERING_WRAPPERS: MutableMap<ETRendererID, ETRenderingAPI.Callback> = mutableMapOf()

private val REGISTRATION_LOGGER: Logger = LoggerFactory.getLogger("ETRenderingActions")

fun allDecorators(): Sequence<ETDecorator> = sequence {
    yieldAll(PRE_RENDERERS)
    yieldAll(REPL_RENDERERS)
    yieldAll(POST_RENDERERS)
}

fun decoratorsOfType(method: ETRenderMethod): Sequence<ETDecorator> =
    matchDecoratorList(method).asSequence()

fun List<ETDecorator>.indexOfId(identifier: Identifier) =
    indexOfFirst { it.type.identifier == identifier }

fun registerDecorator(dec: ETDecorator, action: BiConsumer<MutableList<ETDecorator>, ETDecorator>) {
    val list = matchDecoratorList(dec.type.method)
    val match = list.find { it.type == dec.type }
    require(match == null) { "Decorator ${dec.type.identifier} is already registered as ${match!!::class.qualifiedName}" }

    REGISTRATION_LOGGER.info("Registering decorator ${dec.type.identifier} (${dec.type.method})")
    action.accept(list, dec)
}

fun wrapParametersCallback(id: ETRendererID, callback: ParametersCallback) {
    REGISTRATION_LOGGER.info("Registering parameters callback for ${id.identifier} (${id.method})")
    PARAMETER_WRAPPERS.compute(id) { _, it -> it?.andThen(callback) ?: callback }
}

fun wrapRenderCallback(id: ETRendererID, callback: ETRenderingAPI.Callback) {
    REGISTRATION_LOGGER.info("Registering rendering callback for ${id.identifier} (${id.method})")
    RENDERING_WRAPPERS.compute(id) { _, current ->
        if (current == null) return@compute callback

        ETRenderingAPI.Callback { parameters, collector, function ->
            callback.render(parameters, collector) { it, _ -> current.render(it, collector, function) }
        }
    }
}

fun ETRenderParameters.prepare(id: ETRendererID): ETRenderParameters = apply {
    PARAMETER_WRAPPERS[id]?.accept(this)
}

fun ETRenderParameters.render(id: ETRendererID, collector: SubmitNodeCollector): Boolean =
    RENDERING_WRAPPERS[id]?.render(this, collector, ETRenderParameters::submitToCollector)
        ?: this.submitToCollector(collector)

fun ETRenderParameters.submitToCollector(collector: SubmitNodeCollector): Boolean {
    collector.order(order.getAndIncrement()).submitModel(
        model, state, matrices, renderType, lightCoords, OverlayTexture.NO_OVERLAY,
        tintedColor, sprite, outlineColor, /*? if <26.3*///null
    )
    return true
}

private fun matchDecoratorList(method: ETRenderMethod): MutableList<ETDecorator> = when (method) {
    ETRenderMethod.PRE -> PRE_RENDERERS
    ETRenderMethod.OVERRIDE -> REPL_RENDERERS
    ETRenderMethod.POST -> POST_RENDERERS
}

fun EquipmentLayerRenderer.buildParameters(
    type: EquipmentClientInfo.LayerType, layers: MutableList<EquipmentClientInfo.Layer>, info: EquipmentClientInfo?,
    model: Model<Any>, state: Any, itemStack: ItemStack, poseStack: PoseStack, playerTextureOverride: Identifier?,
    lightCoords: Int, outlineColor: Int, order: Int,
): ETRenderParameters? {
    if (type != EquipmentClientInfo.LayerType.WINGS || layers.isEmpty()) return null

    val params = ETRenderParameters(order, model, state, itemStack, poseStack, ElytraTrimsAtlas.elytraDecorations())
    params.lightCoords = lightCoords
    params.tintedColor = -1
    params.outlineColor = outlineColor
    params.baseTexture = if (!layers.first().usePlayerTexture && playerTextureOverride != null) playerTextureOverride
    else layerTextureLookup.apply(EquipmentLayerRenderer.LayerTextureKey(type, layers.first()))

    //? if >=26.3 {
    val trim = itemStack.get(DataComponents.TRIM)
    if (trim != null && info != null) params.trimTextureHandle = trimTextureLookup
        .apply(EquipmentLayerRenderer.TrimTextureKey(trim, type, info))
    //?}

    return params
}

fun renderDecorators(method: ETRenderMethod, parameters: ETRenderParameters?, collector: SubmitNodeCollector?): Boolean {
    return collector != null && parameters != null
        && decoratorsOfType(method).fold(false) { acc, dec -> dec.render(parameters, collector) || acc }
}
