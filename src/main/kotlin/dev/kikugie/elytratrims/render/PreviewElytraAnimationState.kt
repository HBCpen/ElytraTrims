package dev.kikugie.elytratrims.render

import kotlin.math.PI
import kotlin.math.pow

class PreviewElytraAnimationState {
    private companion object {
        const val TRANSITION_TICKS: Float = 10F
        const val ELYTRA_ROTATION: Float = (PI / 12).toFloat()
    }

    private var currentProgress: Float = 0F
    private var targetProgress: Float = 0F

    var bodyRot: Float = 0F
        private set
    var elytraXRot: Float = 0F
        private set
    var elytraZRot: Float = 0F
        private set

    fun tick(delta: Float) {
        if (currentProgress == targetProgress) return
        currentProgress =
            if (currentProgress < targetProgress) (currentProgress + delta).coerceAtMost(targetProgress)
            else (currentProgress - delta).coerceAtLeast(targetProgress)

        val normalizedProgress = interpolate(currentProgress / TRANSITION_TICKS)
        bodyRot = 180 * normalizedProgress
        elytraXRot = ELYTRA_ROTATION * normalizedProgress
        elytraZRot = -ELYTRA_ROTATION * normalizedProgress
    }

    fun update(isElytra: Boolean) {
        targetProgress = if (isElytra) TRANSITION_TICKS else 0F
    }

    private fun interpolate(value: Float): Float = when (value) {
        0F, 1F -> value
        else if (value < 0.5F) -> (2.0.pow((20 * value - 10).toDouble()) / 2).toFloat()
        else -> ((2 - 2.0.pow((-20 * value + 10).toDouble())) / 2).toFloat()
    }
}
