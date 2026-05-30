@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.elytratrims.resource.image

import com.mojang.blaze3d.platform.NativeImage
import java.lang.ref.Cleaner
import kotlin.math.abs

private val IMAGE_CLEANER: Cleaner = Cleaner.create()

@JvmInline
value class EditableImage(val native: NativeImage) : AutoCloseable by native {
    constructor(width: Int, height: Int, format: NativeImage.Format = NativeImage.Format.RGBA)
        : this(NativeImage(format, width, height, true))

    init {
        IMAGE_CLEANER.register(native, CleaningAction(native))
    }

    val width: Int inline get() = native.width
    val height: Int inline get() = native.height

    inline operator fun get(x: Int, y: Int): Color4i = Color4i(native.getPixel(x, y))
    inline operator fun set(x: Int, y: Int, color: Color4i): Unit = native.setPixel(x, y, color.packed)

    inline fun contains(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

    private class CleaningAction(val native: NativeImage) : Runnable {
        override fun run(): Unit = native.close()
    }
}

inline fun NativeImage.asEditable(): EditableImage = EditableImage(this)

inline fun EditableImage.getOrTransparent(x: Int, y: Int): Color4i =
    if (contains(x, y)) this[x, y] else Color4i.TRANSPARENT

fun EditableImage.copy(
    other: EditableImage,
    dx: Int = 0,
    dy: Int = 0,
    sx: Int = 0,
    sy: Int = 0,
    sw: Int = other.width,
    sh: Int = other.height,
) {
    for (oy in 0..<abs(sh))
        for (ox in 0..<abs(sw)) {
            val x = dx + ox
            val y = dy + oy

            if (!contains(x, y)) continue
            val cx = if (sw >= 0) sx + ox else sx - sw - ox - 1
            val cy = if (sh >= 0) sy + oy else sy - sh - oy - 1

            val color = other.getOrTransparent(cx, cy)
            if (color.a > 0) this[x, y] = color
        }
}

fun EditableImage.scale(factor: Int) {
    for (y in (height - 1 downTo 0)) for (x in (width - 1 downTo 0)) {
        val sx = x * factor;
        val sy = y * factor;
        val color = this[sx, sy]
        for (dy in 0..<factor) for (dx in 0..<factor)
            this[sx + dx, sy + dy] = color
    }
}

fun EditableImage.mask(mask: EditableImage) {
    val xScale = mask.width / width.toDouble()
    val yScale = mask.height / height.toDouble()

    for (y in 0..<height) for (x in 0..<width) {
        var isVisible = false
        check@ for (my in (y * yScale).toInt()..<(y * yScale + 1).toInt())
            for (mx in (x * xScale).toInt()..<(x * xScale + 1).toInt())
                if (mask[mx, my].a > 0) {
                    isVisible = true
                    break@check
                }

        if (!isVisible) this[x, y] = Color4i.TRANSPARENT
    }
}

fun EditableImage.saturate() {
    var diff = 0
    for (y in 0..<height) for (x in 0..<width) {
        val (r, g, b, a) = this[x, y]
        if (a > 0) diff = maxOf(diff, r, g, b)
    }
    diff = 255 - diff

    for (y in 0..<height) for (x in 0..<width) {
        val (r, g, b, a) = this[x, y]
        if (a == 0) continue

        val sat = maxOf(r, g, b) + diff
        this[x, y] = Color4i(sat, sat, sat, a)
    }
}

fun EditableImage.outline(color: Color4i): EditableImage {
    val canvas = EditableImage(width, height, native.format())
    for (y in 0..<height) for (x in 0..<width) {
        if (this[x, y].a == 0) continue
        if (checkOffsetOverlay(x, y, 1, 0)
            || checkOffsetOverlay(x, y, 0, 1)
            || checkOffsetOverlay(x, y, -1, 0)
            || checkOffsetOverlay(x, y, 0, -1))
            canvas[x, y] = color
    }
    return canvas
}

private fun EditableImage.checkOffsetOverlay(x: Int, y: Int, dx: Int, dy: Int): Boolean =
    getOrTransparent(x + dx, y + dy).a == 0