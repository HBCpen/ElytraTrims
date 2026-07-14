package dev.kikugie.elytratrims.render

import com.mojang.blaze3d.vertex.PoseStack
import dev.kikugie.elytratrims.render.api.ETParameters
import dev.kikugie.elytratrims.render.impl.*
import net.minecraft.client.model.Model
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer
import net.minecraft.client.resources.model.EquipmentClientInfo
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import java.util.concurrent.atomic.AtomicInteger

// Public API!
fun EquipmentLayerRenderer.buildParameters(
    type: EquipmentClientInfo.LayerType, layers: MutableList<EquipmentClientInfo.Layer>, info: EquipmentClientInfo?,
    model: Model<*>, state: Any, itemStack: ItemStack, poseStack: PoseStack, playerTextureOverride: Identifier?,
    lightCoords: Int, outlineColor: Int
): ETParameters? {
    if (type != EquipmentClientInfo.LayerType.WINGS || layers.isEmpty()) return null

    val params = ETParameters()
    params.model = model
    params.state = state
    params.stack = itemStack
    params.matrices = poseStack
    params.lightCoords = lightCoords
    params.outlineColour = outlineColor
    params.texture = if (!layers.first().usePlayerTexture && playerTextureOverride != null) playerTextureOverride
    else layerTextureLookup.apply(EquipmentLayerRenderer.LayerTextureKey(type, layers.first()))

    //? if >=26.3 {
    val trim = itemStack.get(DataComponents.TRIM)
    if (trim != null && info != null) params.trimTextureHandle = trimTextureLookup
        .apply(EquipmentLayerRenderer.TrimTextureKey(trim, type, info))
    //?}

    return params
}

// Public API!
fun renderReplDecorations(collector: SubmitNodeCollector?, parameters: ETParameters?, order: AtomicInteger?): Boolean {
    if (collector == null || parameters == null || order == null) return false

    return ETGatewayRenderer.render(parameters, collector, order)
}

// Public API!
fun renderPostDecorations(collector: SubmitNodeCollector?, parameters: ETParameters?, order: AtomicInteger?): Boolean {
    if (collector == null || parameters == null || order == null) return false

    var rendered: Boolean
    rendered = ETBadAppleRenderer.render(parameters, collector, order)
    rendered = ETColorRenderer.render(parameters, collector, order) || rendered
    rendered = ETPatternsRenderer.render(parameters, collector, order) || rendered
    rendered = ETTrimsRenderer.render(parameters, collector, order) || rendered
    return rendered
}
