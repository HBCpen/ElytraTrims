package dev.kikugie.elytratrims.cosmetic.gui

import com.mojang.blaze3d.platform.InputConstants
import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings.Design
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.CELL_WIDTH
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.DANGER_COLOR
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.HINT_COLOR
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.HOVER_COLOR
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.PANEL_COLOR
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.ROW_HEIGHT
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.SELECTED_COLOR
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.TEXT_COLOR
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.inside
import dev.kikugie.elytratrims.cosmetic.gui.PanelDrawing.outline
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.locale.Language
import net.minecraft.network.chat.CommonComponents

/**
 * Saved designs: save the cosmetic being edited under a name, load one back into the editor,
 * or share designs as text through the clipboard. Changes to the list are saved immediately.
 *
 * @param current the cosmetic being edited, which is saved and shown when no design is selected.
 * @param onLoad receives the design to load into the editor.
 */
class DesignsScreen(
    private val parent: Screen,
    private val preview: ElytraPreview,
    private val current: ElytraCosmetic,
    private val onLoad: (ElytraCosmetic) -> Unit,
) : Screen(tr("designs.title")) {
    private companion object {
        const val STATUS_TICKS = 60
    }

    private val designs: MutableList<Design> = CosmeticSettings.data.designs.toMutableList()
    /** Index into [designs], or -1. */
    var selected: Int = -1
        private set
    /** Row whose delete button was clicked once and waits for confirmation, or -1. */
    private var pendingDelete: Int = -1
    private var scroll = 0
    private var status: Text? = null
    private var statusTicks = 0
    private var nameText: String = ""

    private lateinit var layout: EditorLayout
    private var blockX = 0
    private var blockWidth = 0
    private var listTop = 0
    private var listBottom = 0
    private var listRows = 1

    private lateinit var nameBox: EditBox
    private lateinit var loadButton: Button

    val designNames: List<String> get() = designs.map(Design::name)

    override fun init() {
        layout = EditorLayout.compute(width, height, 0, 300)
        preview.setArea(layout.previewX0, layout.contentTop, layout.previewX1, layout.previewBottom)
        val l = layout
        val h = EditorLayout.WIDGET_HEIGHT
        blockX = l.controlsX
        blockWidth = l.controlsWidth

        // Name and save button
        val saveWidth = (blockWidth / 3).coerceIn(60, 100)
        nameBox = addRenderableWidget(EditBox(font, blockX, l.contentTop, blockWidth - saveWidth - 4, h, tr("designs.name")).apply {
            setMaxLength(Design.MAX_NAME_LENGTH)
            setHint(tr("designs.name"))
            setValue(nameText)
            setResponder { nameText = it }
        })
        addRenderableWidget(Button.builder(tr("designs.save")) { saveCurrent() }
            .bounds(blockX + blockWidth - saveWidth, l.contentTop, saveWidth, h)
            .tooltip(Tooltip.create(tr("designs.save.tooltip"))).build())

        // List, then load / copy / paste aligned below it
        listTop = l.contentTop + h + 4
        val actionY = l.contentBottom - h
        listRows = ((actionY - 4 - listTop) / ROW_HEIGHT).coerceAtLeast(1)
        listBottom = listTop + listRows * ROW_HEIGHT

        val third = (blockWidth - 8) / 3
        val actionsY = listBottom + 4
        loadButton = addRenderableWidget(Button.builder(tr("designs.load")) { loadSelected() }
            .bounds(blockX, actionsY, third, h).build())
        addRenderableWidget(Button.builder(tr("designs.copy")) { copyToClipboard() }
            .bounds(blockX + third + 4, actionsY, blockWidth - 2 * third - 8, h)
            .tooltip(Tooltip.create(tr("designs.copy.tooltip"))).build())
        addRenderableWidget(Button.builder(tr("designs.paste")) { pasteFromClipboard() }
            .bounds(blockX + blockWidth - third, actionsY, third, h)
            .tooltip(Tooltip.create(tr("designs.paste.tooltip"))).build())

        addRenderableWidget(preview.poseButton(l.previewX0, l.footerY, l.previewX1 - l.previewX0, h)
            .also { it.setTooltip(Tooltip.create(tr("preview_hint"))) })
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK) { onClose() }
            .bounds(blockX, l.footerY, blockWidth, h).build())

        clampScroll()
        updateButtons()
    }

    override fun onClose() {
        minecraft.gui.setScreen(parent)
    }

    override fun isPauseScreen(): Boolean = false

    override fun tick() {
        if (statusTicks > 0 && --statusTicks == 0) status = null
    }

    // Rendering

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        graphics.centeredText(font, status ?: title, width / 2, 8, TEXT_COLOR)

        graphics.fill(blockX, listTop, blockX + blockWidth, listBottom, PANEL_COLOR)
        if (designs.isEmpty()) graphics.centeredText(font, tr("designs.empty"), blockX + blockWidth / 2, listTop + 6, HINT_COLOR)
        for (row in 0 until listRows) {
            val index = row + scroll
            val design = designs.getOrNull(index) ?: break
            val y = listTop + row * ROW_HEIGHT
            if (index == selected) outline(graphics, blockX, y, blockWidth, ROW_HEIGHT, SELECTED_COLOR)
            else if (inside(mouseX, mouseY, blockX, y, blockWidth, ROW_HEIGHT)) graphics.fill(blockX, y, blockX + blockWidth, y + ROW_HEIGHT, HOVER_COLOR)

            val name = Language.getInstance().getVisualOrder(font.substrByWidth(Text.literal(design.name), deleteX() - blockX - 6))
            graphics.text(font, name, blockX + 4, y + (ROW_HEIGHT - font.lineHeight) / 2 + 1, TEXT_COLOR)

            val hovered = inside(mouseX, mouseY, deleteX(), y, CELL_WIDTH, ROW_HEIGHT)
            val confirming = index == pendingDelete
            PanelDrawing.cell(graphics, font, deleteX(), y, if (confirming) "?" else "✕", true, hovered, DANGER_COLOR)
            if (hovered) graphics.setTooltipForNextFrame(tr(if (confirming) "designs.delete_confirm" else "designs.delete"), mouseX, mouseY)
        }
        PanelDrawing.scrollbar(graphics, blockX + blockWidth - 2, listTop, listBottom - listTop, scroll, designs.size, listRows)

        layout.extractPreviewHint(graphics, font)
        preview.extract(graphics, designs.getOrNull(selected)?.cosmetic ?: current, partialTick)
    }

    // Input

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val x = event.x().toInt()
        val y = event.y().toInt()
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && inside(x, y, blockX, listTop, blockWidth, listBottom - listTop)) {
            val index = (y - listTop) / ROW_HEIGHT + scroll
            if (index !in designs.indices) return true
            if (x >= deleteX() && x < deleteX() + CELL_WIDTH) {
                if (pendingDelete == index) delete(index) else pendingDelete = index
            } else {
                pendingDelete = -1
                selected = index
                if (doubleClick) loadSelected()
            }
            updateButtons()
            return true
        }
        pendingDelete = -1
        return preview.mouseClicked(event) || super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean =
        preview.mouseDragged(dx, dy) || super.mouseDragged(event, dx, dy)

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        preview.mouseReleased()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!inside(x.toInt(), y.toInt(), blockX, listTop, blockWidth, listBottom - listTop)) return super.mouseScrolled(x, y, scrollX, scrollY)
        scroll -= scrollY.toInt().coerceIn(-1, 1)
        clampScroll()
        return true
    }

    /** Screen position of a design's row, or of its delete button. Used by tests. */
    fun rowCenter(index: Int, deleteButton: Boolean = false): Pair<Int, Int>? {
        val row = index - scroll
        if (row < 0 || row >= listRows) return null
        return (if (deleteButton) deleteX() + CELL_WIDTH / 2 else blockX + 30) to listTop + row * ROW_HEIGHT + ROW_HEIGHT / 2
    }

    // Actions

    private fun saveCurrent() {
        val name = nameText.trim().ifEmpty { tr("designs.default_name", designs.size + 1).string }.take(Design.MAX_NAME_LENGTH)
        val existing = designs.indexOfFirst { it.name == name }
        if (existing >= 0) designs[existing] = Design(name, current) else designs += Design(name, current)
        selected = if (existing >= 0) existing else designs.lastIndex
        persist()
        scrollTo(selected)
        showStatus(tr(if (existing >= 0) "designs.overwritten" else "designs.saved", name))
    }

    private fun loadSelected() {
        val design = designs.getOrNull(selected) ?: return
        onLoad(design.cosmetic)
        minecraft.gui.setScreen(parent)
    }

    private fun delete(index: Int) {
        designs.removeAt(index)
        pendingDelete = -1
        selected = when {
            selected == index -> -1
            selected > index -> selected - 1
            else -> selected
        }
        persist()
        clampScroll()
    }

    private fun copyToClipboard() {
        val design = designs.getOrNull(selected) ?: Design(nameText.trim().ifEmpty { tr("designs.default_name", 1).string }, current)
        minecraft.keyboardHandler.clipboard = Design.encode(design)
        showStatus(tr("designs.copied", design.name))
    }

    private fun pasteFromClipboard() {
        val design = Design.decode(minecraft.keyboardHandler.clipboard) ?: return showStatus(tr("designs.paste_failed"))
        designs += design.copy(name = uniqueName(design.name))
        selected = designs.lastIndex
        persist()
        scrollTo(selected)
        showStatus(tr("designs.pasted", designs[selected].name))
    }

    // Helpers

    private fun uniqueName(name: String): String {
        if (designs.none { it.name == name }) return name
        return (2..Int.MAX_VALUE).asSequence()
            .map { "${name.take(Design.MAX_NAME_LENGTH - 4)} ($it)" }
            .first { candidate -> designs.none { it.name == candidate } }
    }

    private fun persist() {
        CosmeticSettings.updateDesigns(designs.toList())
        updateButtons()
    }

    private fun updateButtons() {
        loadButton.active = selected in designs.indices
    }

    private fun deleteX(): Int = blockX + blockWidth - 3 - CELL_WIDTH

    private fun scrollTo(index: Int) {
        if (index < scroll) scroll = index
        if (index >= scroll + listRows) scroll = index - listRows + 1
        clampScroll()
    }

    private fun clampScroll() {
        scroll = scroll.coerceIn(0, (designs.size - listRows).coerceAtLeast(0))
    }

    private fun showStatus(text: Text) {
        status = text
        statusTicks = STATUS_TICKS
    }
}
