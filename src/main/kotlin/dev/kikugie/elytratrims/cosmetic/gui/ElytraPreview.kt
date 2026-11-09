package dev.kikugie.elytratrims.cosmetic.gui

import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetics
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Pose
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI

/**
 * Rotatable preview of the local player wearing a given cosmetic, shared by the editor screens
 * so the view and pose stay the same when switching between them.
 */
class ElytraPreview {
    enum class PreviewPose(val key: String, val defaultPitch: Float) {
        STANDING("standing", 10F),
        SNEAKING("sneaking", 10F),
        GLIDING("gliding", 55F);

        val label: Text get() = Text.translatable("screen.elytratrims.cosmetic.pose.$key")
    }

    var pose: PreviewPose = PreviewPose.STANDING
        private set

    /** Body rotation; 0 shows the back of the player. */
    private var yaw: Float = 30F
    /** Camera tilt in degrees; positive looks down onto the player. */
    private var pitch: Float = pose.defaultPitch
    private var dragging: Boolean = false

    // Area of the last layout
    private var x0 = 0
    private var y0 = 0
    private var x1 = 0
    private var y1 = 0

    fun setArea(x0: Int, y0: Int, x1: Int, y1: Int) {
        this.x0 = x0
        this.y0 = y0
        this.x1 = x1
        this.y1 = y1
    }

    fun poseButton(x: Int, y: Int, width: Int, height: Int): CycleButton<PreviewPose> =
        CycleButton.builder(PreviewPose::label, pose)
            .withValues(PreviewPose.entries)
            .create(x, y, width, height, Text.translatable("screen.elytratrims.cosmetic.pose")) { _, value ->
                pose = value
                pitch = value.defaultPitch
            }

    fun extract(graphics: GuiGraphicsExtractor, cosmetic: ElytraCosmetic?, partialTick: Float) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return extractNeedsWorld(graphics, minecraft.font)
        val state = minecraft.entityRenderDispatcher.getRenderer(player).createRenderState(player, partialTick)
        if (state !is AvatarRenderState) return

        state.shadowPieces.clear()
        state.outlineColor = 0
        state.nameTag = null
        state.bodyRot = yaw
        state.yRot = 0F
        state.xRot = 0F
        state.walkAnimationPos = 0F
        state.walkAnimationSpeed = 0F
        state.boundingBoxWidth /= state.scale
        state.boundingBoxHeight /= state.scale
        state.scale = 1F
        applyPose(state)
        state.setData(ElytraCosmetics.RENDER_KEY, cosmetic?.takeUnless { it.isEmpty })
        // Show the elytra even when the player isn't wearing one right now, keeping a worn chestplate visible.
        state.setData(ElytraCosmetics.VIRTUAL_WINGS_KEY, !state.chestEquipment.has(DataComponents.GLIDER))

        val width = (x1 - x0).toFloat()
        val height = (y1 - y0).toFloat()
        // Rough extents of the model in blocks, including spread wings.
        val (extentX, extentY) = when (pose) {
            PreviewPose.GLIDING -> 3.4F to 2.8F
            // Sneaking spreads the wings sideways.
            PreviewPose.SNEAKING -> 2.8F to 2.3F
            else -> 1.6F to 2.3F
        }
        val size = minOf(width / extentX, height / extentY)
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()
        val rotation = Quaternionf().rotateZ(PI.toFloat()).rotateX(-pitchRad)
        val camera = Quaternionf().rotateX(-pitchRad)
        graphics.entity(state, size, centerOffset(rotation), rotation, camera, x0, y0, x1, y1)
    }

    /** Without a player, e.g. when opened from ModMenu on the title screen, there is nothing to draw. */
    private fun extractNeedsWorld(graphics: GuiGraphicsExtractor, font: Font) {
        val lines = font.split(Text.translatable("screen.elytratrims.cosmetic.preview_needs_world"), (x1 - x0 - 8).coerceAtLeast(1))
        val top = (y0 + y1) / 2 - lines.size * font.lineHeight / 2
        lines.forEachIndexed { i, line -> graphics.centeredText(font, line, (x0 + x1) / 2, top + i * font.lineHeight, PanelDrawing.HINT_COLOR) }
    }

    /**
     * Translation that puts the middle of the body at the center of the preview. Entities rotate around their feet,
     * so this follows the renderer's rotations: the body yaw, the gliding tilt and finally the view [rotation].
     */
    private fun centerOffset(rotation: Quaternionf): Vector3f {
        val center = Vector3f(0F, if (pose == PreviewPose.SNEAKING) 0.75F else 0.9F, 0F)
        val body = Quaternionf().rotateY(Math.toRadians(180.0 - yaw).toFloat())
        if (pose == PreviewPose.GLIDING) body.rotateX(Math.toRadians(-90.0).toFloat())
        body.transform(center)
        rotation.transform(center)
        return center.negate()
    }

    private fun applyPose(state: AvatarRenderState) {
        state.swimAmount = 0F
        state.isVisuallySwimming = false
        state.isCrouching = pose == PreviewPose.SNEAKING
        state.isFallFlying = pose == PreviewPose.GLIDING
        state.shouldApplyFlyingYRot = false
        // Mirrors the targets of the vanilla ElytraAnimationState.
        when (pose) {
            PreviewPose.STANDING -> {
                state.pose = Pose.STANDING
                state.fallFlyingTimeInTicks = 0F
                setElytraRotation(state, (PI / 12).toFloat(), 0F, (-PI / 12).toFloat())
            }
            PreviewPose.SNEAKING -> {
                state.pose = Pose.CROUCHING
                state.fallFlyingTimeInTicks = 0F
                setElytraRotation(state, (PI * 2 / 9).toFloat(), 0.08726646F, (-PI / 4).toFloat())
            }
            PreviewPose.GLIDING -> {
                state.pose = Pose.FALL_FLYING
                state.fallFlyingTimeInTicks = 20F
                setElytraRotation(state, (PI / 9).toFloat(), 0F, (-PI / 2).toFloat())
            }
        }
    }

    private fun setElytraRotation(state: AvatarRenderState, x: Float, y: Float, z: Float) {
        state.elytraRotX = x
        state.elytraRotY = y
        state.elytraRotZ = z
    }

    fun isInside(x: Double, y: Double): Boolean = x >= x0 && x < x1 && y >= y0 && y < y1

    /** Starts dragging when clicked inside the preview; returns whether the click was consumed. */
    fun mouseClicked(event: MouseButtonEvent): Boolean {
        dragging = isInside(event.x(), event.y())
        return dragging
    }

    fun mouseDragged(dx: Double, dy: Double): Boolean {
        if (!dragging) return false
        yaw = (yaw - dx.toFloat() * 2F) % 360F
        pitch = (pitch + dy.toFloat() * 1.5F).coerceIn(-80F, 89F)
        return true
    }

    fun mouseReleased() {
        dragging = false
    }
}
