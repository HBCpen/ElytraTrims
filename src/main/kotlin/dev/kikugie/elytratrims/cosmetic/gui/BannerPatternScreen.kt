package dev.kikugie.elytratrims.cosmetic.gui

import com.mojang.blaze3d.platform.InputConstants
import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.cosmetic.CosmeticCatalog
import dev.kikugie.elytratrims.cosmetic.CosmeticStacks
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic.PatternLayer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.Sheets
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.locale.Language
import net.minecraft.network.chat.CommonComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.DyeColor
import kotlin.math.roundToInt

/**
 * Loom-like editor for the banner pattern layers of a cosmetic.
 *
 * Pick a color, then click a pattern to stack a new layer on top. Selecting an existing layer in the list
 * makes the palette and pattern grid edit that layer instead. The list shows the front-most layer first,
 * below the "new layer" row, like layers in an image editor.
 *
 * @param base the cosmetic being edited, used for the preview.
 * @param onDone receives the edited layers and pattern style when the editor is confirmed.
 */
class BannerPatternScreen(
    private val parent: Screen,
    private val preview: ElytraPreview,
    private val base: ElytraCosmetic,
    private val onDone: (List<PatternLayer>, Boolean) -> Unit,
) : Screen(tr("patterns.title")) {
    companion object {
        /** Selection index of the "new layer" row. */
        const val NEW_LAYER = -1

        private const val TEXT_COLOR = -1
        private const val HINT_COLOR = 0xFFA0A0A0.toInt()
        private const val SELECTED_COLOR = 0xFFFFFFFF.toInt()
        private const val HOVER_COLOR = 0x40FFFFFF
        private const val PANEL_COLOR = 0x60000000
        private const val ROW_HEIGHT = 16
        private const val STATUS_TICKS = 60
    }

    private val registries: RegistryAccess? get() = minecraft.level?.registryAccess()
    private val patterns: List<Identifier> by lazy { CosmeticCatalog.bannerPatterns(registries) }

    private val layers: MutableList<PatternLayer> = base.patterns.toMutableList()
    private var bannerStyle: Boolean = base.bannerStyle
    /** Index into [layers], or [NEW_LAYER]. */
    var selected: Int = NEW_LAYER
        private set
    private var color: DyeColor = DyeColor.WHITE

    private var layerScroll = 0
    private var gridScroll = 0
    private var status: Text? = null
    private var statusTicks = 0

    private lateinit var layout: EditorLayout
    private var hintLines: List<net.minecraft.util.FormattedCharSequence> = emptyList()

    // Panel geometry, computed in init()
    private var listX = 0
    private var listY = 0
    private var listWidth = 0
    private var listRows = 0
    private var paletteX = 0
    private var paletteY = 0
    private var swatchSize = 0
    private var gridX = 0
    private var gridY = 0
    private var gridColumns = 1
    private var gridRows = 1
    private var tileWidth = 16
    private var tileHeight = 24
    private var iconWidth = 10

    private lateinit var upButton: Button
    private lateinit var downButton: Button
    private lateinit var removeButton: Button
    private lateinit var clearButton: Button

    val currentLayers: List<PatternLayer> get() = layers.toList()

    override fun init() {
        layout = EditorLayout.compute(width, height, 0, 420)
        preview.setArea(layout.previewX0, layout.contentTop, layout.previewX1, layout.previewBottom)
        val l = layout
        val h = EditorLayout.WIDGET_HEIGHT

        hintLines = font.split(tr("patterns.hint"), l.controlsWidth).take(2)
        val panelTop = l.contentTop + hintLines.size * font.lineHeight + 4

        // Layer list with its buttons below
        listX = l.controlsX
        listY = panelTop
        listWidth = (l.controlsWidth * 0.4F).toInt().coerceIn(100, 180)
        val buttonsY = l.contentBottom - 2 * h - 4
        listRows = ((buttonsY - 4 - listY) / ROW_HEIGHT).coerceAtLeast(1)

        val third = (listWidth - 8) / 3
        upButton = addRenderableWidget(Button.builder(Text.literal("▲")) { moveSelected(1) }
            .bounds(listX, buttonsY, third, h).tooltip(Tooltip.create(tr("patterns.move_up"))).build())
        downButton = addRenderableWidget(Button.builder(Text.literal("▼")) { moveSelected(-1) }
            .bounds(listX + third + 4, buttonsY, third, h).tooltip(Tooltip.create(tr("patterns.move_down"))).build())
        removeButton = addRenderableWidget(Button.builder(Text.literal("✕")) { removeSelected() }
            .bounds(listX + listWidth - third, buttonsY, third, h).tooltip(Tooltip.create(tr("remove_layer"))).build())
        val clearWidth = (listWidth - 4) / 3
        addRenderableWidget(Button.builder(tr("import")) { importFromHand() }
            .bounds(listX, buttonsY + h + 4, listWidth - clearWidth - 4, h).tooltip(Tooltip.create(tr("import.tooltip"))).build())
        clearButton = addRenderableWidget(Button.builder(tr("patterns.clear")) { clearLayers() }
            .bounds(listX + listWidth - clearWidth, buttonsY + h + 4, clearWidth, h).build())

        // Palette and pattern grid
        val gridPanelX = listX + listWidth + 6
        val gridPanelWidth = l.controlsX + l.controlsWidth - gridPanelX
        swatchSize = (gridPanelWidth / 8).coerceAtMost(16)
        paletteX = gridPanelX
        paletteY = panelTop
        gridX = gridPanelX
        gridY = paletteY + swatchSize * 2 + 6
        // Bigger tiles when there is room, so the patterns are easy to tell apart.
        iconWidth = ((gridPanelWidth - 4) / 10 - 6).coerceIn(10, 16)
        tileWidth = iconWidth + 6
        tileHeight = iconWidth * 2 + 4
        gridColumns = ((gridPanelWidth - 4) / tileWidth).coerceAtLeast(1)
        gridRows = ((l.contentBottom - gridY) / tileHeight).coerceAtLeast(1)

        // Preview and footer
        addRenderableWidget(preview.poseButton(l.previewX0, l.footerY, l.previewX1 - l.previewX0, h)
            .also { it.setTooltip(Tooltip.create(tr("preview_hint"))) })
        val footer = l.footerButtons(3)
        addRenderableWidget(CycleButton.booleanBuilder(tr("pattern_style.banner"), tr("pattern_style.shield"), bannerStyle)
            .create(footer[0], l.footerY, l.footerWidth(3), h, tr("pattern_style")) { _, value -> bannerStyle = value })
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL) { minecraft.gui.setScreen(parent) }
            .bounds(footer[1], l.footerY, l.footerWidth(3), h).build())
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }
            .bounds(footer[2], l.footerY, l.footerWidth(3), h).build())

        clampScroll()
        updateButtons()
    }

    override fun onClose() {
        onDone(layers.toList(), bannerStyle)
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
        hintLines.forEachIndexed { i, line -> graphics.text(font, line, layout.controlsX, layout.contentTop + i * font.lineHeight, HINT_COLOR) }

        layout.extractPreviewHint(graphics, font)
        extractLayerList(graphics, mouseX, mouseY)
        extractPalette(graphics, mouseX, mouseY)
        extractPatternGrid(graphics, mouseX, mouseY)
        preview.extract(graphics, base.copy(patterns = layers.toList(), bannerStyle = bannerStyle), partialTick)
    }

    private fun extractLayerList(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.fill(listX, listY, listX + listWidth, listY + listRows * ROW_HEIGHT, PANEL_COLOR)
        for (row in 0 until listRows) {
            val selectionIndex = selectionForEntry(row + layerScroll) ?: break
            val y = listY + row * ROW_HEIGHT
            val isNewRow = selectionIndex == NEW_LAYER
            val index = selectionIndex
            if (selectionIndex == selected) outline(graphics, listX, y, listWidth, ROW_HEIGHT, SELECTED_COLOR)
            else if (inside(mouseX, mouseY, listX, y, listWidth, ROW_HEIGHT)) graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, HOVER_COLOR)

            val text: Text = if (isNewRow) tr("patterns.new_layer") else {
                val layer = layers[index]
                extractPatternIcon(graphics, layer.pattern, layer.color, listX + 3, y + 2, 6, 12)
                Text.literal("${index + 1}. ").append(CosmeticCatalog.bannerPatternName(layer.pattern, layer.color, registries))
            }
            val textX = listX + if (isNewRow) 4 else 13
            val clipped = Language.getInstance().getVisualOrder(font.substrByWidth(text, listX + listWidth - textX - 2))
            graphics.text(font, clipped, textX, y + (ROW_HEIGHT - font.lineHeight) / 2 + 1, TEXT_COLOR)
        }
        scrollbar(graphics, listX + listWidth - 2, listY, listRows * ROW_HEIGHT, layerScroll, layers.size + 1, listRows)
    }

    private fun extractPalette(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        for ((i, dye) in DyeColor.entries.withIndex()) {
            val x = paletteX + (i % 8) * swatchSize
            val y = paletteY + (i / 8) * swatchSize
            graphics.fill(x + 1, y + 1, x + swatchSize - 1, y + swatchSize - 1, dye.textureDiffuseColor)
            if (dye == color) outline(graphics, x, y, swatchSize, swatchSize, SELECTED_COLOR)
            else if (inside(mouseX, mouseY, x, y, swatchSize, swatchSize)) {
                outline(graphics, x, y, swatchSize, swatchSize, HINT_COLOR)
                graphics.setTooltipForNextFrame(CosmeticCatalog.dyeName(dye), mouseX, mouseY)
            }
        }
    }

    private fun extractPatternGrid(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val current = layers.getOrNull(selected)?.pattern
        graphics.fill(gridX, gridY, gridX + gridColumns * tileWidth, gridY + gridRows * tileHeight, PANEL_COLOR)
        for (slot in 0 until gridColumns * gridRows) {
            val index = slot + gridScroll * gridColumns
            val pattern = patterns.getOrNull(index) ?: break
            val x = gridX + (slot % gridColumns) * tileWidth
            val y = gridY + (slot / gridColumns) * tileHeight
            val hovered = inside(mouseX, mouseY, x, y, tileWidth, tileHeight)
            if (pattern == current) outline(graphics, x, y, tileWidth, tileHeight, SELECTED_COLOR)
            else if (hovered) graphics.fill(x, y, x + tileWidth, y + tileHeight, HOVER_COLOR)
            extractPatternIcon(graphics, pattern, color, x + 3, y + 2, iconWidth, iconWidth * 2)
            if (hovered) graphics.setTooltipForNextFrame(CosmeticCatalog.bannerPatternName(pattern, color, registries), mouseX, mouseY)
        }
        val totalRows = (patterns.size + gridColumns - 1) / gridColumns
        scrollbar(graphics, gridX + gridColumns * tileWidth + 1, gridY, gridRows * tileHeight, gridScroll, totalRows, gridRows)
    }

    /** Draws a banner with a single pattern, on a gray that contrasts with the pattern color. */
    private fun extractPatternIcon(graphics: GuiGraphicsExtractor, pattern: Identifier, dye: DyeColor, x: Int, y: Int, w: Int, h: Int) {
        val rgb = dye.textureDiffuseColor
        val luminance = (0.299 * (rgb shr 16 and 0xFF) + 0.587 * (rgb shr 8 and 0xFF) + 0.114 * (rgb and 0xFF)) / 255
        val background = if (luminance > 0.55) 0xFF3C3C3C.toInt() else 0xFFB4B4B4.toInt()
        graphics.fill(x, y, x + w, y + h, background)
        val sprite = graphics.getSprite(Sheets.getBannerSprite(CosmeticStacks.bannerPattern(pattern, registries)))
        // The front of a banner texture is the 20x40 area at (1, 1) of a 64x64 texture.
        val spriteWidth = sprite.contents().width()
        val spriteHeight = sprite.contents().height()
        val textureWidth = (spriteWidth / (sprite.u1 - sprite.u0)).roundToInt()
        val textureHeight = (spriteHeight / (sprite.v1 - sprite.v0)).roundToInt()
        val scale = spriteWidth / 64F
        graphics.blit(
            RenderPipelines.GUI_TEXTURED, sprite.atlasLocation(), x, y,
            sprite.u0 * textureWidth + scale, sprite.v0 * textureHeight + scale, w, h,
            (20 * scale).roundToInt(), (40 * scale).roundToInt(), textureWidth, textureHeight, dye.textureDiffuseColor
        )
    }

    private fun scrollbar(graphics: GuiGraphicsExtractor, x: Int, y: Int, height: Int, offset: Int, total: Int, visible: Int) {
        if (total <= visible) return
        val thumb = (height * visible / total).coerceAtLeast(6)
        val thumbY = y + (height - thumb) * offset / (total - visible)
        graphics.fill(x, thumbY, x + 2, thumbY + thumb, HINT_COLOR)
    }

    private fun outline(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        graphics.fill(x, y, x + w, y + 1, color)
        graphics.fill(x, y + h - 1, x + w, y + h, color)
        graphics.fill(x, y, x + 1, y + h, color)
        graphics.fill(x + w - 1, y, x + w, y + h, color)
    }

    // Input

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            val x = event.x().toInt()
            val y = event.y().toInt()
            layerRowAt(x, y)?.let { selectLayer(it); return true }
            swatchAt(x, y)?.let { pickColor(it); return true }
            patternAt(x, y)?.let { pickPattern(it); return true }
        }
        return preview.mouseClicked(event) || super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean =
        preview.mouseDragged(dx, dy) || super.mouseDragged(event, dx, dy)

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        preview.mouseReleased()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        val step = -scrollY.toInt().coerceIn(-1, 1)
        when {
            inside(x.toInt(), y.toInt(), listX, listY, listWidth, listRows * ROW_HEIGHT) -> layerScroll += step
            inside(x.toInt(), y.toInt(), gridX, gridY, gridColumns * tileWidth + 4, gridRows * tileHeight) -> gridScroll += step
            else -> return super.mouseScrolled(x, y, scrollX, scrollY)
        }
        clampScroll()
        return true
    }

    /** Screen position of a pattern tile, or `null` if it is scrolled out of view. Used by tests. */
    fun patternTileCenter(pattern: Identifier): Pair<Int, Int>? {
        val slot = patterns.indexOf(pattern) - gridScroll * gridColumns
        if (slot < 0 || slot >= gridColumns * gridRows) return null
        return gridX + (slot % gridColumns) * tileWidth + tileWidth / 2 to gridY + (slot / gridColumns) * tileHeight + tileHeight / 2
    }

    /** Screen position of a palette swatch. Used by tests. */
    fun swatchCenter(dye: DyeColor): Pair<Int, Int> =
        paletteX + (dye.ordinal % 8) * swatchSize + swatchSize / 2 to paletteY + (dye.ordinal / 8) * swatchSize + swatchSize / 2

    /** Screen position of a row in the layer list ([NEW_LAYER] for the "new layer" row), or `null` if scrolled out of view. */
    fun layerRowCenter(selection: Int): Pair<Int, Int>? {
        val row = entryForSelection(selection) - layerScroll
        if (row < 0 || row >= listRows) return null
        return listX + listWidth / 2 to listY + row * ROW_HEIGHT + ROW_HEIGHT / 2
    }

    private fun layerRowAt(x: Int, y: Int): Int? {
        if (!inside(x, y, listX, listY, listWidth, listRows * ROW_HEIGHT)) return null
        return selectionForEntry((y - listY) / ROW_HEIGHT + layerScroll)
    }

    /** List entries are the "new layer" row followed by the layers from front to back. */
    private fun selectionForEntry(entry: Int): Int? = when {
        entry == 0 -> NEW_LAYER
        entry in 1..layers.size -> layers.size - entry
        else -> null
    }

    private fun entryForSelection(selection: Int): Int = if (selection == NEW_LAYER) 0 else layers.size - selection

    private fun swatchAt(x: Int, y: Int): DyeColor? {
        if (!inside(x, y, paletteX, paletteY, swatchSize * 8, swatchSize * 2)) return null
        return DyeColor.entries.getOrNull((y - paletteY) / swatchSize * 8 + (x - paletteX) / swatchSize)
    }

    private fun patternAt(x: Int, y: Int): Identifier? {
        if (!inside(x, y, gridX, gridY, gridColumns * tileWidth, gridRows * tileHeight)) return null
        val slot = (y - gridY) / tileHeight * gridColumns + (x - gridX) / tileWidth
        return patterns.getOrNull(slot + gridScroll * gridColumns)
    }

    // Actions

    private fun selectLayer(selection: Int) {
        selected = selection
        layers.getOrNull(selection)?.let { color = it.color }
        updateButtons()
    }

    private fun pickColor(dye: DyeColor) {
        color = dye
        if (selected in layers.indices) layers[selected] = layers[selected].copy(color = dye)
        updateButtons()
    }

    private fun pickPattern(pattern: Identifier) {
        if (selected in layers.indices) {
            layers[selected] = layers[selected].copy(pattern = pattern)
        } else if (layers.size >= ElytraCosmetic.MAX_PATTERN_LAYERS) {
            showStatus(tr("patterns.limit", ElytraCosmetic.MAX_PATTERN_LAYERS))
        } else {
            layers += PatternLayer(pattern, color)
            // Keep the new row visible so layers can be stacked quickly.
            layerScroll = 0
        }
        clampScroll()
        updateButtons()
    }

    private fun moveSelected(step: Int) {
        val target = selected + step
        if (selected !in layers.indices || target !in layers.indices) return
        layers[selected] = layers[target].also { layers[target] = layers[selected] }
        selected = target
        updateButtons()
    }

    private fun removeSelected() {
        if (selected !in layers.indices) return
        layers.removeAt(selected)
        if (selected >= layers.size) selected = NEW_LAYER
        clampScroll()
        updateButtons()
    }

    private fun clearLayers() {
        layers.clear()
        selected = NEW_LAYER
        clampScroll()
        updateButtons()
    }

    /** Copies the pattern layers of a banner or shield held in either hand. */
    private fun importFromHand() {
        val player = minecraft.player ?: return
        val source = InteractionHand.entries.asSequence()
            .map { player.getItemInHand(it).get(DataComponents.BANNER_PATTERNS) }
            .firstOrNull { it != null && it.layers().isNotEmpty() }
        if (source == null) return showStatus(tr("import.failed"))

        layers.clear()
        source.layers().take(ElytraCosmetic.MAX_PATTERN_LAYERS).mapTo(layers) { layer ->
            val id = layer.pattern().unwrapKey().map { it.identifier() }.orElseGet { layer.pattern().value().assetId() }
            PatternLayer(id, layer.color())
        }
        selected = NEW_LAYER
        clampScroll()
        updateButtons()
        showStatus(tr("import.success", layers.size))
    }

    // Helpers

    private fun updateButtons() {
        val hasSelection = selected in layers.indices
        upButton.active = hasSelection && selected < layers.size - 1
        downButton.active = hasSelection && selected > 0
        removeButton.active = hasSelection
        clearButton.active = layers.isNotEmpty()
    }

    private fun clampScroll() {
        layerScroll = layerScroll.coerceIn(0, (layers.size + 1 - listRows).coerceAtLeast(0))
        val totalRows = (patterns.size + gridColumns - 1) / gridColumns
        gridScroll = gridScroll.coerceIn(0, (totalRows - gridRows).coerceAtLeast(0))
    }

    private fun showStatus(text: Text) {
        status = text
        statusTicks = STATUS_TICKS
    }

    private fun inside(x: Int, y: Int, x0: Int, y0: Int, w: Int, h: Int): Boolean = x >= x0 && x < x0 + w && y >= y0 && y < y0 + h
}
