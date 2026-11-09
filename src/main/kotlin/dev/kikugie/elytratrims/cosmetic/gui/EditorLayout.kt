package dev.kikugie.elytratrims.cosmetic.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Shared layout of the editor screens: a preview column with the pose button below it,
 * and a block of controls to its right with the footer buttons below. The whole content is centered on screen
 * and also fits Minecraft's smallest GUI size (320x240).
 */
class EditorLayout private constructor(
    val previewX0: Int,
    val previewX1: Int,
    val labelX: Int,
    val labelWidth: Int,
    val controlsX: Int,
    val controlsWidth: Int,
    val screenHeight: Int,
    private val rows: Int,
) {
    companion object {
        const val WIDGET_HEIGHT = 20
        const val MARGIN = 8
        const val GAP = 10
        const val CONTENT_TOP = 22
        private const val HINT_COLOR = 0xFFA0A0A0.toInt()

        /**
         * @param maxLabelWidth widest row label, or 0 for screens without labels.
         * @param maxControlsWidth upper bound for the controls block.
         * @param rows number of control rows, which are vertically centered; 0 to use the full height.
         */
        fun compute(width: Int, height: Int, maxLabelWidth: Int, maxControlsWidth: Int, rows: Int = 0): EditorLayout {
            val previewWidth = (width * 0.28F).toInt().coerceIn(80, 300)
            val labelWidth = if (maxLabelWidth == 0) 0 else (maxLabelWidth + 6).coerceAtMost(80)
            val controlsWidth = (width - 2 * MARGIN - previewWidth - GAP - labelWidth).coerceAtMost(maxControlsWidth)
            val left = (width - (previewWidth + GAP + labelWidth + controlsWidth)) / 2
            val labelX = left + previewWidth + GAP
            return EditorLayout(left, left + previewWidth, labelX, labelWidth, labelX + labelWidth, controlsWidth, height, rows)
        }

        fun forControls(width: Int, height: Int, rows: Int, maxLabelWidth: Int): EditorLayout =
            compute(width, height, maxLabelWidth, 260, rows)
    }

    val contentTop: Int get() = CONTENT_TOP
    val footerY: Int get() = screenHeight - 26
    /** Bottom of the content area above the footer. */
    val contentBottom: Int get() = footerY - 4
    /** The preview leaves room for the hint line between it and the pose button. */
    val previewBottom: Int get() = footerY - 13

    val half: Int get() = (controlsWidth - 4) / 2
    val secondColumn: Int get() = controlsX + controlsWidth - half

    /** Left edge and width of everything right of the preview, including labels. */
    val rightX: Int get() = labelX
    val rightWidth: Int get() = controlsX + controlsWidth - labelX

    private val rowStep: Int get() = if (rows == 0) 0 else ((contentBottom - CONTENT_TOP) / rows).coerceIn(WIDGET_HEIGHT + 1, WIDGET_HEIGHT + 4)
    private val rowsTop: Int get() {
        if (rows == 0) return CONTENT_TOP
        val rowsHeight = (rows - 1) * rowStep + WIDGET_HEIGHT
        return CONTENT_TOP + ((contentBottom - CONTENT_TOP - rowsHeight) / 2).coerceAtLeast(0)
    }

    fun rowY(row: Int): Int = rowsTop + row * rowStep

    fun footerWidth(count: Int): Int = ((rightWidth - 4 * (count - 1)) / count).coerceAtMost(90)

    /** X positions of [count] footer buttons, centered below the controls. */
    fun footerButtons(count: Int): IntArray {
        val width = footerWidth(count)
        val start = rightX + (rightWidth - (width * count + 4 * (count - 1))) / 2
        return IntArray(count) { start + it * (width + 4) }
    }

    /**
     * Draws the drag hint below the preview if it fits; it is also the pose button's tooltip.
     * Outside a world there is no preview to rotate, so it is left out.
     */
    fun extractPreviewHint(graphics: GuiGraphicsExtractor, font: Font) {
        val hint = tr("preview_hint")
        if (Minecraft.getInstance().player != null && font.width(hint) <= previewX1 - previewX0)
            graphics.centeredText(font, hint, (previewX0 + previewX1) / 2, previewBottom + 2, HINT_COLOR)
    }
}
