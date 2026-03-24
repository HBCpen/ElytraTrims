package dev.kikugie.elytratrims.render

import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.MINECRAFT
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import org.joml.Quaternionf
import org.joml.Vector3f
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.atan

@JvmField val RENDER_LOGGER: Logger = LoggerFactory.getLogger("ETRenderer")

val TextureAtlasSprite?.isMissing
    get() = this == null || contents().name() == MissingTextureAtlasSprite.getLocation()

fun Identifier.getSprite(identifier: Identifier, reporter: (Identifier) -> Unit): TextureAtlasSprite =
    MINECRAFT.atlasManager.getAtlasOrThrow(this).getSprite(identifier).also { if (it.isMissing) reporter(identifier) }

fun renderEntityInInventoryFollowsMouse(
    guiGraphics: GuiGraphicsExtractor,
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    scale: Int,
    yOffset: Float,
    mouseX: Float,
    mouseY: Float,
    entity: LivingEntity,
    horizontalAngle: Float
) {
    val f = (x1 + x2).toFloat() / 2.0f
    val g = (y1 + y2).toFloat() / 2.0f
    val h = atan(((f - mouseX) / 40.0f).toDouble()).toFloat()
    val i = atan(((g - mouseY) / 40.0f).toDouble()).toFloat()
    val quaternionf = (Quaternionf()).rotateZ(Math.PI.toFloat())
    val quaternionf2 = (Quaternionf()).rotateX(i * 20.0f * (Math.PI.toFloat() / 180f))
    quaternionf.mul(quaternionf2)
    val entityRenderState = InventoryScreen.extractRenderState(entity)
    if (entityRenderState is LivingEntityRenderState) {
        entityRenderState.bodyRot = 180.0f + h * 20.0f + horizontalAngle
        entityRenderState.yRot = h * 20.0f + horizontalAngle
        if (entityRenderState.pose != Pose.FALL_FLYING) {
            entityRenderState.xRot = -i * 20.0f
        } else {
            entityRenderState.xRot = 0.0f
        }

        entityRenderState.boundingBoxWidth /= entityRenderState.scale
        entityRenderState.boundingBoxHeight /= entityRenderState.scale
        entityRenderState.scale = 1.0f
    }

    val vector3f = Vector3f(0.0f, entityRenderState.boundingBoxHeight / 2.0f + yOffset, 0.0f)
    //~ if >26 submitEntityRenderState -> entity
    guiGraphics.entity(entityRenderState, scale.toFloat(), vector3f, quaternionf, quaternionf2, x1, y1, x2, y2)
}