package dev.kikugie.elytratrims.cosmetic.gui

import com.mojang.blaze3d.platform.InputConstants
import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.cosmetic.CosmeticCatalog
import dev.kikugie.elytratrims.cosmetic.CosmeticStacks
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic.PatternLayer
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
import dev.kikugie.elytratrims.vanilla
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
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.DyeColor
import kotlin.math.roundToInt

/**
 * Loom-like editor for the banner pattern layers of a cosmetic.
 *
 * "+ New Layer" adds a layer on top and selects it; the palette and the pattern grid then edit the selected layer.
 * The list shows the front-most layer first, like layers in an image editor, and each row has buttons to move
 * or remove its layer.
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
        /** Selection index when no layer is selected. */
        const val NO_LAYER = -1

        private const val GAP = 6
        private const val STATUS_TICKS = 60
        /** Pattern of new layers: a plain fill, which is then usually replaced. */
        private val DEFAULT_PATTERN: Identifier = vanilla("base")
    }

    private enum class RowAction(val glyph: String, val tooltip: String) {
        UP("▲", "patterns.move_up"),
        DOWN("▼", "patterns.move_down"),
        REMOVE("✕", "remove_layer"),
    }

    private val registries: RegistryAccess? get() = minecraft.level?.registryAccess()
    private val patterns: List<Identifier> by lazy { CosmeticCatalog.bannerPatterns(registries) }

    private val layers: MutableList<PatternLayer> = base.patterns.toMutableList()
    private var bannerStyle: Boolean = base.bannerStyle
    /** Index into [layers], or [NO_LAYER]. */
    var selected: Int = if (layers.isEmpty()) NO_LAYER else layers.lastIndex
        private set
    private var color: DyeColor = layers.getOrNull(selected)?.color ?: DyeColor.WHITE

    private var layerScroll = 0
    private var gridScroll = 0
    private var status: Text? = null
    private var statusTicks = 0

    private lateinit var layout: EditorLayout
    private var hintLines: List<FormattedCharSequence> = emptyList()

    // Panel geometry, computed in init()
    private var blockX = 0
    private var blockWidth = 0
    private var panelTop = 0
    private var panelBottom = 0
    private var listX = 0
    private var listWidth = 0
    private var listRows = 1
    private var gridX = 0
    private var gridY = 0
    private var gridWidth = 0
    private var gridColumns = 1
    private var gridRows = 1
    private var tileWidth = 16
    private var tileHeight = 24
    private var iconWidth = 10
    private var swatchWidth = 10
    private var swatchHeight = 10

    private lateinit var clearButton: Button

    val currentLayers: List<PatternLayer> get() = layers.toList()

    override fun init() {
        layout = EditorLayout.compute(width, height, 0, 440)
        preview.setArea(layout.previewX0, layout.contentTop, layout.previewX1, layout.previewBottom)
        val l = layout
        val h = EditorLayout.WIDGET_HEIGHT

        // Horizontal layout: the layer list, then the palette and pattern grid. Their widths are chosen so that
        // the palette and the grid share the same edges, and the block is centered in the controls area.
        listWidth = (l.controlsWidth * 0.42F).toInt().coerceIn(110, 200)
        val gridSpace = l.controlsWidth - listWidth - GAP
        var best = Triple(10, 1, 0) // icon width, columns, grid width
        for (icon in 16 downTo 10 step 2) {
            val tile = icon + 6
            val columns = (gridSpace / tile downTo 1).firstOrNull { (it * tile) % 8 == 0 } ?: continue
            if (columns * tile > best.third) best = Triple(icon, columns, columns * tile)
        }
        iconWidth = best.first
        tileWidth = iconWidth + 6
        tileHeight = iconWidth * 2 + 4
        gridColumns = best.second
        gridWidth = best.third.coerceAtLeast(tileWidth)
        swatchWidth = gridWidth / 8
        swatchHeight = swatchWidth.coerceAtMost(16)
        blockWidth = listWidth + GAP + gridWidth
        blockX = l.controlsX + (l.controlsWidth - blockWidth) / 2
        listX = blockX
        gridX = blockX + listWidth + GAP

        // Vertical layout: hint, then both panels with equal top and bottom edges, then a row of buttons.
        hintLines = font.split(tr("patterns.hint"), blockWidth).take(3)
        panelTop = l.contentTop + hintLines.size * font.lineHeight + 4
        val actionY = l.contentBottom - h
        gridY = panelTop + swatchHeight * 2 + 4
        gridRows = ((actionY - 4 - gridY) / tileHeight).coerceAtLeast(1)
        panelBottom = gridY + gridRows * tileHeight
        listRows = ((panelBottom - panelTop) / ROW_HEIGHT).coerceAtLeast(1)
        // Right below the panels, so the spare height ends up above the footer rather than between them.
        val buttonsY = panelBottom + 4

        val clearWidth = (listWidth - 4) / 3
        addRenderableWidget(Button.builder(tr("import")) { importFromHand() }
            .bounds(listX, buttonsY, listWidth - clearWidth - 4, h).tooltip(Tooltip.create(tr("import.tooltip"))).build())
        clearButton = addRenderableWidget(Button.builder(tr("patterns.clear")) { clearLayers() }
            .bounds(listX + listWidth - clearWidth, buttonsY, clearWidth, h).build())
        addRenderableWidget(CycleButton.booleanBuilder(tr("pattern_style.banner"), tr("pattern_style.shield"), bannerStyle)
            .create(gridX, buttonsY, gridWidth, h, tr("pattern_style")) { _, value -> bannerStyle = value })

        // Preview and footer, the latter aligned with the columns above
        addRenderableWidget(preview.poseButton(l.previewX0, l.footerY, l.previewX1 - l.previewX0, h)
            .also { it.setTooltip(Tooltip.create(tr("preview_hint"))) })
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL) { minecraft.gui.setScreen(parent) }
            .bounds(listX, l.footerY, listWidth, h).build())
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }
            .bounds(gridX, l.footerY, gridWidth, h).build())

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
        hintLines.forEachIndexed { i, line -> graphics.text(font, line, blockX, layout.contentTop + i * font.lineHeight, HINT_COLOR) }

        layout.extractPreviewHint(graphics, font)
        extractLayerList(graphics, mouseX, mouseY)
        extractPalette(graphics, mouseX, mouseY)
        extractPatternGrid(graphics, mouseX, mouseY)
        preview.extract(graphics, base.copy(patterns = layers.toList(), bannerStyle = bannerStyle), partialTick)
    }

    private fun extractLayerList(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.fill(listX, panelTop, listX + listWidth, panelBottom, PANEL_COLOR)
        for (row in 0 until listRows) {
            val entry = row + layerScroll
            val y = panelTop + row * ROW_HEIGHT
            if (entry == 0) {
                if (inside(mouseX, mouseY, listX, y, listWidth, ROW_HEIGHT)) graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, HOVER_COLOR)
                val canAdd = layers.size < ElytraCosmetic.MAX_PATTERN_LAYERS
                graphics.text(font, tr("patterns.new_layer"), listX + 4, y + (ROW_HEIGHT - font.lineHeight) / 2 + 1, if (canAdd) TEXT_COLOR else HINT_COLOR)
                continue
            }
            val index = layerForEntry(entry) ?: break
            val layer = layers[index]
            val rowHovered = inside(mouseX, mouseY, listX, y, listWidth, ROW_HEIGHT)
            if (index == selected) outline(graphics, listX, y, listWidth, ROW_HEIGHT, SELECTED_COLOR)
            else if (rowHovered) graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, HOVER_COLOR)

            extractPatternIcon(graphics, layer.pattern, layer.color, listX + 3, y + 2, 6, 12)
            val name = Text.literal("${index + 1}. ").append(CosmeticCatalog.bannerPatternName(layer.pattern, layer.color, registries))
            val textX = listX + 13
            val textEnd = cellX(RowAction.UP) - 2
            graphics.text(font, Language.getInstance().getVisualOrder(font.substrByWidth(name, textEnd - textX)), textX, y + (ROW_HEIGHT - font.lineHeight) / 2 + 1, TEXT_COLOR)

            for (action in RowAction.entries) {
                val x = cellX(action)
                val hovered = inside(mouseX, mouseY, x, y, CELL_WIDTH, ROW_HEIGHT)
                val active = canApply(action, index)
                PanelDrawing.cell(graphics, font, x, y, action.glyph, active, hovered, if (action == RowAction.REMOVE) DANGER_COLOR else TEXT_COLOR)
                if (hovered && active) graphics.setTooltipForNextFrame(tr(action.tooltip), mouseX, mouseY)
            }
        }
        PanelDrawing.scrollbar(graphics, listX + listWidth - 2, panelTop, panelBottom - panelTop, layerScroll, layers.size + 1, listRows)
    }

    private fun extractPalette(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        for ((i, dye) in DyeColor.entries.withIndex()) {
            val x = gridX + (i % 8) * swatchWidth
            val y = panelTop + (i / 8) * swatchHeight
            graphics.fill(x + 1, y + 1, x + swatchWidth - 1, y + swatchHeight - 1, dye.textureDiffuseColor)
            if (dye == color) outline(graphics, x, y, swatchWidth, swatchHeight, SELECTED_COLOR)
            else if (inside(mouseX, mouseY, x, y, swatchWidth, swatchHeight)) {
                outline(graphics, x, y, swatchWidth, swatchHeight, HINT_COLOR)
                graphics.setTooltipForNextFrame(CosmeticCatalog.dyeName(dye), mouseX, mouseY)
            }
        }
    }

    private fun extractPatternGrid(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val current = layers.getOrNull(selected)?.pattern
        graphics.fill(gridX, gridY, gridX + gridWidth, panelBottom, PANEL_COLOR)
        for (slot in 0 until gridColumns * gridRows) {
            val pattern = patterns.getOrNull(slot + gridScroll * gridColumns) ?: break
            val x = gridX + (slot % gridColumns) * tileWidth
            val y = gridY + (slot / gridColumns) * tileHeight
            val hovered = inside(mouseX, mouseY, x, y, tileWidth, tileHeight)
            if (pattern == current) outline(graphics, x, y, tileWidth, tileHeight, SELECTED_COLOR)
            else if (hovered) graphics.fill(x, y, x + tileWidth, y + tileHeight, HOVER_COLOR)
            extractPatternIcon(graphics, pattern, color, x + 3, y + 2, iconWidth, iconWidth * 2)
            if (hovered) graphics.setTooltipForNextFrame(CosmeticCatalog.bannerPatternName(pattern, color, registries), mouseX, mouseY)
        }
        val totalRows = (patterns.size + gridColumns - 1) / gridColumns
        PanelDrawing.scrollbar(graphics, gridX + gridWidth - 2, gridY, panelBottom - gridY, gridScroll, totalRows, gridRows)
    }

    /** Draws a banner with a single pattern, on a gray that contrasts with the pattern color. */
    private fun extractPatternIcon(graphics: GuiGraphicsExtractor, pattern: Identifier, dye: DyeColor, x: Int, y: Int, w: Int, h: Int) {
        val rgb = dye.textureDiffuseColor
        val luminance = (0.299 * (rgb shr 16 and 0xFF) + 0.587 * (rgb shr 8 and 0xFF) + 0.114 * (rgb and 0xFF)) / 255
        graphics.fill(x, y, x + w, y + h, if (luminance > 0.55) 0xFF3C3C3C.toInt() else 0xFFB4B4B4.toInt())
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
            (20 * scale).roundToInt(), (40 * scale).roundToInt(), textureWidth, textureHeight, rgb
        )
    }

    // Input

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && clickPanels(event.x().toInt(), event.y().toInt())) return true
        return preview.mouseClicked(event) || super.mouseClicked(event, doubleClick)
    }

    private fun clickPanels(x: Int, y: Int): Boolean {
        if (inside(x, y, listX, panelTop, listWidth, listRows * ROW_HEIGHT)) {
            val entry = (y - panelTop) / ROW_HEIGHT + layerScroll
            if (entry == 0) return true.also { addLayer() }
            val index = layerForEntry(entry) ?: return true
            val action = RowAction.entries.firstOrNull { x >= cellX(it) && x < cellX(it) + CELL_WIDTH }
            if (action != null) apply(action, index) else selectLayer(index)
            return true
        }
        swatchAt(x, y)?.let { pickColor(it); return true }
        patternAt(x, y)?.let { pickPattern(it); return true }
        return false
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
            inside(x.toInt(), y.toInt(), listX, panelTop, listWidth, panelBottom - panelTop) -> layerScroll += step
            inside(x.toInt(), y.toInt(), gridX, gridY, gridWidth, panelBottom - gridY) -> gridScroll += step
            else -> return super.mouseScrolled(x, y, scrollX, scrollY)
        }
        clampScroll()
        return true
    }

    // Positions for tests

    /** Screen position of a pattern tile, or `null` if it is scrolled out of view. */
    fun patternTileCenter(pattern: Identifier): Pair<Int, Int>? {
        val slot = patterns.indexOf(pattern) - gridScroll * gridColumns
        if (slot < 0 || slot >= gridColumns * gridRows) return null
        return gridX + (slot % gridColumns) * tileWidth + tileWidth / 2 to gridY + (slot / gridColumns) * tileHeight + tileHeight / 2
    }

    fun swatchCenter(dye: DyeColor): Pair<Int, Int> =
        gridX + (dye.ordinal % 8) * swatchWidth + swatchWidth / 2 to panelTop + (dye.ordinal / 8) * swatchHeight + swatchHeight / 2

    /** Screen position of the "+ New Layer" row, or `null` if it is scrolled out of view. */
    fun newLayerRowCenter(): Pair<Int, Int>? = rowCenter(0, listX + listWidth / 2)

    /** Screen position of a layer's row, or of one of its buttons ("up", "down" or "remove"). */
    fun layerRowCenter(index: Int, button: String? = null): Pair<Int, Int>? {
        val x = RowAction.entries.firstOrNull { it.name.equals(button, ignoreCase = true) }?.let { cellX(it) + CELL_WIDTH / 2 } ?: (listX + 30)
        return rowCenter(layers.size - index, x)
    }

    private fun rowCenter(entry: Int, x: Int): Pair<Int, Int>? {
        val row = entry - layerScroll
        if (row < 0 || row >= listRows) return null
        return x to panelTop + row * ROW_HEIGHT + ROW_HEIGHT / 2
    }

    // Geometry

    /** List entries are the "+ New Layer" row followed by the layers from front to back. */
    private fun layerForEntry(entry: Int): Int? = if (entry in 1..layers.size) layers.size - entry else null

    private fun cellX(action: RowAction): Int = listX + listWidth - 3 - CELL_WIDTH * (RowAction.entries.size - action.ordinal)

    private fun swatchAt(x: Int, y: Int): DyeColor? {
        if (!inside(x, y, gridX, panelTop, swatchWidth * 8, swatchHeight * 2)) return null
        return DyeColor.entries.getOrNull((y - panelTop) / swatchHeight * 8 + (x - gridX) / swatchWidth)
    }

    private fun patternAt(x: Int, y: Int): Identifier? {
        if (!inside(x, y, gridX, gridY, gridColumns * tileWidth, gridRows * tileHeight)) return null
        val slot = (y - gridY) / tileHeight * gridColumns + (x - gridX) / tileWidth
        return patterns.getOrNull(slot + gridScroll * gridColumns)
    }

    // Actions

    private fun addLayer(pattern: Identifier = DEFAULT_PATTERN.takeIf { it in patterns } ?: patterns.firstOrNull() ?: DEFAULT_PATTERN) {
        if (layers.size >= ElytraCosmetic.MAX_PATTERN_LAYERS)
            return showStatus(tr("patterns.limit", ElytraCosmetic.MAX_PATTERN_LAYERS))
        layers += PatternLayer(pattern, color)
        selected = layers.lastIndex
        // New layers appear right below the "+ New Layer" row.
        layerScroll = 0
        clampScroll()
        updateButtons()
    }

    private fun selectLayer(index: Int) {
        selected = index
        color = layers[index].color
    }

    private fun pickColor(dye: DyeColor) {
        color = dye
        if (selected in layers.indices) layers[selected] = layers[selected].copy(color = dye)
    }

    private fun pickPattern(pattern: Identifier) {
        if (selected in layers.indices) layers[selected] = layers[selected].copy(pattern = pattern)
        // Without a selected layer, picking a pattern is the quickest way to start.
        else addLayer(pattern)
    }

    private fun canApply(action: RowAction, index: Int): Boolean = when (action) {
        RowAction.UP -> index < layers.lastIndex
        RowAction.DOWN -> index > 0
        RowAction.REMOVE -> true
    }

    private fun apply(action: RowAction, index: Int) {
        if (!canApply(action, index)) return
        when (action) {
            RowAction.UP, RowAction.DOWN -> {
                val target = if (action == RowAction.UP) index + 1 else index - 1
                layers[index] = layers[target].also { layers[target] = layers[index] }
                // The selection follows the layer it was on.
                selected = when (selected) {
                    index -> target
                    target -> index
                    else -> selected
                }
            }
            RowAction.REMOVE -> {
                layers.removeAt(index)
                selected = when {
                    selected == index -> (index - 1).coerceAtLeast(0).takeIf { layers.isNotEmpty() } ?: NO_LAYER
                    selected > index -> selected - 1
                    else -> selected
                }
            }
        }
        clampScroll()
        updateButtons()
    }

    private fun clearLayers() {
        layers.clear()
        selected = NO_LAYER
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
        selected = layers.lastIndex
        clampScroll()
        updateButtons()
        showStatus(tr("import.success", layers.size))
    }

    // Helpers

    private fun updateButtons() {
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
}
