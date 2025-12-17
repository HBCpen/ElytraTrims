package dev.kikugie.elytratrims.api.impl

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.api.item.ETItemFlag
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import net.minecraft.client.model.Model
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.item.ItemStack
import java.util.concurrent.atomic.AtomicInteger


fun ETRenderParameters.getEffectiveLight() =
    if (ETItemFlag.GLOW[stack]) 0xFF00FF else light

fun ETRenderParameters.submitToCollector(collector: SubmitNodeCollector) {
    collector.order(order.getAndIncrement()).submitModel(elytra, `object`, matrices, render, light, overlay, color, sprite, outline, null)
}

fun ETRenderParameters.copy(
    elytra: Model<Any> = this.elytra(),
    obj: Any = this.`object`(),
    stack: ItemStack = this.stack(),
    matrices: PoseStack = this.matrices(),
    render: RenderType = this.render(),
    sprite: TextureAtlasSprite? = this.sprite(),
    texture: Identifier = this.texture(),
    light: Int = this.light(),
    color: Int = this.color(),
    overlay: Int = this.overlay(),
    outline: Int = this.outline(),
    argument: AtomicInteger = this.order()
): ETRenderParameters = ETRenderParameters(elytra, obj, stack, matrices, render, sprite, texture, light, color, overlay, outline, argument)

fun List<ETDecorator>.indexOfId(identifier: Identifier) =
    indexOfFirst { it.type.identifier == identifier }