package dev.kikugie.elytratrims.api.impl

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.item.ETFlags
import dev.kikugie.elytratrims.item.ItemFlagAccessor.Companion.flags
import net.minecraft.client.model.Model
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.item.ItemStack
import java.util.function.Function

@JvmField val NEEDS_OVERWRITE: Function<ETRenderParameters, VertexConsumer> = Function {
    throw UnsupportedOperationException("Please specify the VertexConsumer converter")
}

fun ETRenderParameters.getEffectiveLight() =
    if (stack.flags[ETFlags.GLOW]) 0xFF00FF else light

fun ETRenderParameters.renderToBuffer() =
    elytra.renderToBuffer(matrices, converter.apply(this), light, OverlayTexture.NO_OVERLAY, color)

fun ETRenderParameters.withConverter(converter: Function<ETRenderParameters, VertexConsumer>) =
    copy(converter = converter)

fun ETRenderParameters.copy(
    elytra: Model = elytra(),
    stack: ItemStack = stack(),
    matrices: PoseStack = matrices(),
    source: MultiBufferSource = source(),
    sprite: TextureAtlasSprite? = sprite(),
    texture: Identifier = texture(),
    light: Int = light(),
    color: Int = color(),
    converter: Function<ETRenderParameters, VertexConsumer> = converter()
) = ETRenderParameters(elytra, stack, matrices, source, sprite, texture, light, color, converter)

fun List<ETDecorator>.indexOfId(identifier: Identifier) =
    indexOfFirst { it.type.identifier == identifier }