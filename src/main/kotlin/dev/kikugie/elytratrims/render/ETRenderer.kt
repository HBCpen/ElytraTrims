package dev.kikugie.elytratrims.render

import com.mojang.blaze3d.vertex.PoseStack
import dev.kikugie.elytratrims.MINECRAFT
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.render.api.ETParameters
import dev.kikugie.elytratrims.resource.image.Color4i
import net.minecraft.client.model.Model
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

@JvmField val RENDER_LOGGER: Logger = LoggerFactory.getLogger("ETRenderer")

val TextureAtlasSprite?.isMissing
    get() = this == null || contents().name() == MissingTextureAtlasSprite.getLocation()

val Identifier?.isMissing
    get() = this == MissingTextureAtlasSprite.getLocation()

fun Identifier.getSprite(identifier: Identifier, reporter: (Identifier) -> Unit): TextureAtlasSprite =
    MINECRAFT.atlasManager.getAtlasOrThrow(this).getSprite(identifier).also { if (it.isMissing) reporter(identifier) }

val ETParameters.effectiveLightCoords: Int
    get() = if (stack has ETItemFlag.GLOW) 0xFF00FF else lightCoords

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
context(params: ETParameters)
inline fun SubmitNodeCollector.submitModel(
    order: AtomicInteger,
    model: Model<*> = params.model,
    state: Any = params.state,
    matrices: PoseStack = params.matrices,
    renderType: RenderType,
    lightCoords: Int = params.lightCoords,
    overlay: Int = OverlayTexture.NO_OVERLAY,
    color: Color4i = params.color,
    sprite: TextureAtlasSprite? = null,
    outlineColour: Int = params.outlineColour,
    crumblingOverlay: CrumblingOverlay? = params.crumblingOverlay,
): Unit = order(order.getAndIncrement()).submitModel(
    model as Model<Any>,
    state,
    matrices,
    renderType,
    lightCoords,
    overlay,
    color.packed,
    sprite,
    outlineColour,
    crumblingOverlay
)
