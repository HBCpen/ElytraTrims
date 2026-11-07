package dev.kikugie.elytratrims.cosmetic.gui

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Drawing helpers for the custom list and grid panels of the editor screens. */
internal object PanelDrawing {
    const val TEXT_COLOR = -1
    const val HINT_COLOR = 0xFFA0A0A0.toInt()
    const val DISABLED_COLOR = 0xFF606060.toInt()
    const val SELECTED_COLOR = 0xFFFFFFFF.toInt()
    const val DANGER_COLOR = 0xFFFF5555.toInt()
    const val HOVER_COLOR = 0x40FFFFFF
    const val PANEL_COLOR = 0x60000000
    const val ROW_HEIGHT = 16
    /** Width of the small buttons at the right of list rows. */
    const val CELL_WIDTH = 14

    fun inside(x: Int, y: Int, x0: Int, y0: Int, w: Int, h: Int): Boolean = x >= x0 && x < x0 + w && y >= y0 && y < y0 + h

    fun outline(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        graphics.fill(x, y, x + w, y + 1, color)
        graphics.fill(x, y + h - 1, x + w, y + h, color)
        graphics.fill(x, y, x + 1, y + h, color)
        graphics.fill(x + w - 1, y, x + w, y + h, color)
    }

    /** A small glyph button inside a list row. */
    fun cell(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int, glyph: String, active: Boolean, hovered: Boolean, color: Int = TEXT_COLOR) {
        if (active && hovered) graphics.fill(x, y + 1, x + CELL_WIDTH, y + ROW_HEIGHT - 1, HOVER_COLOR)
        graphics.centeredText(font, glyph, x + CELL_WIDTH / 2, y + (ROW_HEIGHT - font.lineHeight) / 2 + 1, if (active) color else DISABLED_COLOR)
    }

    fun scrollbar(graphics: GuiGraphicsExtractor, x: Int, y: Int, height: Int, offset: Int, total: Int, visible: Int) {
        if (total <= visible) return
        val thumb = (height * visible / total).coerceAtLeast(6)
        val thumbY = y + (height - thumb) * offset / (total - visible)
        graphics.fill(x, thumbY, x + 2, thumbY + thumb, HINT_COLOR)
    }
}
