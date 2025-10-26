package dev.kikugie.elytratrims.render

import kotlin.math.pow

private fun interpolate(value: Float): Float {
    if (value == 0f || value == 1f) return value
    return (if (value < 0.5f)
        2.0.pow((20 * value - 10).toDouble()) / 2
    else
        (2 - 2.0.pow((-20 * value + 10).toDouble())) / 2).toFloat()
}

data class SmithingTableRenderData(var rotation: Float = 0F, var position: Float = 0F, var target: Float = 0F,) {
    fun tick(delta: Float) {
        if (position == target) return
        else if (position < target) position = (position + delta).coerceAtMost(target)
        else position = (position - delta).coerceAtLeast(0F)

        val progress = interpolate(position / DURATION_TICKS)
        rotation = 180 * progress
    }

    fun update(isElytra: Boolean) {
        target = if (isElytra) DURATION_TICKS else 0F
    }

    companion object {
        const val DURATION_TICKS: Float = 10F
    }
}
