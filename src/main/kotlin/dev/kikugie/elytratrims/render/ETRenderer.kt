package dev.kikugie.elytratrims.render

import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.MINECRAFT
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf
import org.joml.Vector3f
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.atan

@JvmField val RENDER_LOGGER: Logger = LoggerFactory.getLogger("ETRenderer")

val TextureAtlasSprite?.isMissing
    get() = this == null || contents().name() == MissingTextureAtlasSprite.getLocation()

fun Identifier.getSprite(identifier: Identifier, reporter: (Identifier) -> Unit): TextureAtlasSprite =
    /*? if >=1.21.9 {*/MINECRAFT.atlasManager.getAtlasOrThrow(this).getSprite(identifier)
    /*?} else*///MINECRAFT.getTextureAtlas(this).apply(identifier)
        .also { if (it.isMissing) reporter(identifier) }

fun renderEntityInInventoryFollowsMouse(
    guiGraphics: GuiGraphics,
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
    val centerX = (x1 + x2) / 2.0f
    val centerY = (y1 + y2) / 2.0f
    guiGraphics.enableScissor(x1, y1, x2, y2)

    val mouseHorizontalAngle = atan(((centerX - mouseX) / 40.0f).toDouble()).toFloat()
    val mouseVerticalAngle = atan(((centerY - mouseY) / 40.0f).toDouble()).toFloat()

    val baseRotation = Quaternionf().rotateZ(Math.PI.toFloat())
    val pitchRotation = Quaternionf().rotateX(mouseVerticalAngle * 20.0f * (Math.PI / 180.0).toFloat())
    baseRotation.mul(pitchRotation)

    // Store original entity rotations
    val originalBodyYaw = entity.yBodyRot
    val originalYaw = entity.yRot
    val originalPitch = entity.xRot
    val originalHeadYawOld = entity.yHeadRotO
    val originalHeadYaw = entity.yHeadRot

    // Apply new rotations based on mouse position and horizontal angle
    entity.yBodyRot = 180.0f + mouseHorizontalAngle * 20.0f + horizontalAngle
    entity.yRot = 180.0f + mouseHorizontalAngle * 40.0f + horizontalAngle
    entity.xRot = -mouseVerticalAngle * 20.0f
    entity.yHeadRot = entity.yRot
    entity.yHeadRotO = entity.yRot

    val entityScale = entity.scale
    val translationOffset = Vector3f(0.0f, entity.bbHeight / 2.0f + yOffset * entityScale, 0.0f)
    val renderScale = scale / entityScale

    renderEntityInInventory(guiGraphics, x1, y1, x2, y2, renderScale, translationOffset, baseRotation, pitchRotation, entity)

    // Restore original entity rotations
    entity.yBodyRot = originalBodyYaw
    entity.yRot = originalYaw
    entity.xRot = originalPitch
    entity.yHeadRotO = originalHeadYawOld
    entity.yHeadRot = originalHeadYaw

    guiGraphics.disableScissor()
}