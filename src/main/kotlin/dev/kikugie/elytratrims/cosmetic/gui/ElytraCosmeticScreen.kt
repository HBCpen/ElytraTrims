package dev.kikugie.elytratrims.cosmetic.gui

import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.cosmetic.CosmeticCatalog
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.vanilla
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.RegistryAccess
import net.minecraft.locale.Language
import net.minecraft.network.chat.CommonComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.DyeColor
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * Editor for the local player's elytra cosmetic with a live preview.
 *
 * Closing with Escape or "Done" saves the changes, "Cancel" discards them.
 * Banner patterns are edited in [BannerPatternScreen].
 */
class ElytraCosmeticScreen(private val parent: Screen?) : Screen(tr("title")) {
    private companion object {
        const val ROWS = 9
        const val TEXT_COLOR = -1
        const val ERROR_COLOR = 0xFFFF5555.toInt()
        const val HINT_COLOR = 0xFFA0A0A0.toInt()

        val DEFAULT_MATERIAL: Identifier = vanilla("gold")
        val HEX_PATTERN = Regex("#?([0-9a-fA-F]{6})")

        /** `null` stands for "undyed". */
        val COLOR_PRESETS: List<Int?> = listOf(null) + DyeColor.entries.map { it.textureDiffuseColor and 0xFFFFFF }

        val LABEL_KEYS = listOf("enabled", "visibility", "trim_pattern", "trim_material", "color", "effects", "patterns", "designs")

        /** Where keyboard focus goes when the focused widget is gone or inactive after a rebuild. */
        val FOCUS_FALLBACKS: Map<String, List<String>> = mapOf(
            "trim_material" to listOf("trim_pattern"),
        )
    }

    private val registries: RegistryAccess? get() = minecraft.level?.registryAccess()

    private var enabled: Boolean = CosmeticSettings.data.enabled
    private var draft: ElytraCosmetic = CosmeticSettings.data.cosmetic
    private var material: Identifier = draft.trim?.material ?: DEFAULT_MATERIAL
    private val preview = ElytraPreview()

    private var needsRebuild: Boolean = false

    /** Raw contents of the hex color box, kept across rebuilds even while incomplete. */
    private var colorText: String = draft.color?.let(::hex).orEmpty()
    private var colorCursor: Int = colorText.length

    private val trimPatterns: List<Identifier> by lazy { CosmeticCatalog.trimPatterns(minecraft.resourceManager) }
    private val trimMaterials: List<Identifier> by lazy { CosmeticCatalog.trimMaterials(registries) }

    private lateinit var layout: EditorLayout
    private val labels = mutableListOf<Pair<Text, Int>>()
    private var swatchX = 0
    private var swatchY = 0

    /** Stable identities of the widgets, used to restore focus after rebuilding them. */
    private val widgetKeys = HashMap<GuiEventListener, String>()

    private lateinit var colorPreset: Button
    private lateinit var colorBox: EditBox

    override fun init() {
        labels.clear()
        widgetKeys.clear()
        layout = EditorLayout.forControls(width, height, ROWS, LABEL_KEYS.maxOf { font.width(tr(it)) })
        preview.setArea(layout.previewX0, layout.contentTop, layout.previewX1, layout.previewBottom)
        val l = layout
        val h = EditorLayout.WIDGET_HEIGHT

        // Row 0: master switch
        label(0, "enabled")
        add("enabled", CycleButton.onOffBuilder(enabled).displayOnlyValue()
            .create(l.controlsX, l.rowY(0), l.controlsWidth, h, tr("enabled")) { _, value -> enabled = value })

        // Row 1: when to draw the elytra
        label(1, "visibility")
        add("visibility", CycleButton.builder<ElytraCosmetic.Visibility>({ tr("visibility.${it.serializedName}") }, draft.visibility)
            .withValues(ElytraCosmetic.Visibility.entries)
            .withTooltip { Tooltip.create(tr("visibility.tooltip")) }
            .displayOnlyValue()
            .create(l.controlsX, l.rowY(1), l.controlsWidth, h, tr("visibility")) { _, value -> draft = draft.copy(visibility = value) })

        // Row 2-3: trim
        label(2, "trim_pattern")
        val patternValues = listOf(Optional.empty<Identifier>()) +
            (trimPatterns + listOfNotNull(draft.trim?.pattern)).distinct().map { Optional.of(it) }
        add("trim_pattern", CycleButton.builder<Optional<Identifier>>({ id ->
            id.map { CosmeticCatalog.trimPatternName(it, registries) }.orElse(tr("none"))
        }, Optional.ofNullable(draft.trim?.pattern))
            .withValues(patternValues)
            .displayOnlyValue()
            .create(l.controlsX, l.rowY(2), l.controlsWidth, h, tr("trim_pattern")) { _, value ->
                draft = draft.copy(trim = value.getOrNull()?.let { ElytraCosmetic.Trim(it, material) })
                needsRebuild = true
            })

        label(3, "trim_material")
        add("trim_material", CycleButton.builder<Identifier>({ CosmeticCatalog.trimMaterialName(it, registries) }, material)
            .withValues((trimMaterials + material).distinct())
            .displayOnlyValue()
            .create(l.controlsX, l.rowY(3), l.controlsWidth, h, tr("trim_material")) { _, value ->
                material = value
                draft.trim?.let { draft = draft.copy(trim = it.copy(material = value)) }
            }).active = draft.trim != null

        // Row 4: dye color
        label(4, "color")
        colorPreset = add("color_preset", Button.builder(colorName()) { cycleColorPreset() }
            .bounds(l.controlsX, l.rowY(4), l.half, h)
            .tooltip(Tooltip.create(tr("color.tooltip")))
            .build())
        colorBox = EditBox(font, l.secondColumn, l.rowY(4), l.half - h - 4, h, tr("color")).apply {
            setMaxLength(7)
            setHint(Text.literal("#RRGGBB"))
            setValue(colorText)
            setCursorPosition(colorCursor.coerceIn(0, colorText.length))
            setHighlightPos(cursorPosition)
            setResponder(::onColorTyped)
        }
        add("color_box", colorBox)
        onColorTyped(colorText)
        swatchX = l.controlsX + l.controlsWidth - h
        swatchY = l.rowY(4)

        // Row 5-6: effects
        label(5, "effects")
        toggle(l.controlsX, l.rowY(5), "rainbow", draft.rainbow) { draft = draft.copy(rainbow = it) }
        toggle(l.secondColumn, l.rowY(5), "glow", draft.glow) { draft = draft.copy(glow = it) }
        toggle(l.controlsX, l.rowY(6), "gateway", draft.gateway) { draft = draft.copy(gateway = it) }
        toggle(l.secondColumn, l.rowY(6), "animation", draft.animation) { draft = draft.copy(animation = it) }

        // Row 7-8: banner patterns and saved designs, on their own screens
        label(7, "patterns")
        add("edit_patterns", Button.builder(tr("edit_patterns", draft.patterns.size)) { openPatternEditor() }
            .bounds(l.controlsX, l.rowY(7), l.controlsWidth, h)
            .build())
        label(8, "designs")
        add("designs", Button.builder(tr("designs.open")) { openDesigns() }
            .bounds(l.controlsX, l.rowY(8), l.controlsWidth, h)
            .build())

        add("pose", preview.poseButton(l.previewX0, l.footerY, l.previewX1 - l.previewX0, h)
            .also { it.setTooltip(Tooltip.create(tr("preview_hint"))) })

        val footer = l.footerButtons(3)
        add("reset", Button.builder(tr("reset")) { reset() }.bounds(footer[0], l.footerY, l.footerWidth(3), h).build())
        add("cancel", Button.builder(CommonComponents.GUI_CANCEL) { minecraft.gui.setScreen(parent) }
            .bounds(footer[1], l.footerY, l.footerWidth(3), h).build())
        add("done", Button.builder(CommonComponents.GUI_DONE) { onClose() }
            .bounds(footer[2], l.footerY, l.footerWidth(3), h).build())
    }

    override fun tick() {
        if (needsRebuild) {
            needsRebuild = false
            rebuildWidgets()
        }
    }

    /**
     * Also used when resizing or returning from the pattern editor. The vanilla implementation moves keyboard focus to
     * the first widget, so a following key press would toggle the master switch; keep focus on the equivalent widget instead.
     */
    override fun rebuildWidgets() {
        val focusedKey = focused?.let(widgetKeys::get)
        if (::colorBox.isInitialized) colorCursor = colorBox.cursorPosition
        clearWidgets()
        clearFocus()
        init()

        if (focusedKey == null) return setInitialFocus()
        val target = (listOf(focusedKey) + FOCUS_FALLBACKS[focusedKey].orEmpty()).firstNotNullOfOrNull { key ->
            widgetKeys.entries.firstOrNull { it.value == key && (it.key as? AbstractWidget)?.active != false }?.key
        }
        // Without a suitable replacement, leave nothing focused rather than landing on an unrelated control.
        if (target != null) setFocused(target)
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        CosmeticSettings.update(CosmeticSettings.data.copy(enabled = enabled, cosmetic = draft))
        minecraft.gui.setScreen(parent)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        graphics.centeredText(font, title, width / 2, 8, TEXT_COLOR)
        for ((text, y) in labels) {
            val clipped = Language.getInstance().getVisualOrder(font.substrByWidth(text, layout.labelWidth - 4))
            graphics.text(font, clipped, layout.labelX, y + (EditorLayout.WIDGET_HEIGHT - font.lineHeight) / 2 + 1, TEXT_COLOR)
        }

        val size = EditorLayout.WIDGET_HEIGHT
        graphics.fill(swatchX, swatchY, swatchX + size, swatchY + size, 0xFFA0A0A0.toInt())
        val color = draft.color
        if (color != null) graphics.fill(swatchX + 1, swatchY + 1, swatchX + size - 1, swatchY + size - 1, color or (0xFF shl 24))
        else graphics.centeredText(font, "-", swatchX + size / 2, swatchY + 6, HINT_COLOR)

        layout.extractPreviewHint(graphics, font)
        preview.extract(graphics, draft.takeIf { enabled }, partialTick)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean =
        preview.mouseClicked(event) || super.mouseClicked(event, doubleClick)

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean =
        preview.mouseDragged(dx, dy) || super.mouseDragged(event, dx, dy)

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        preview.mouseReleased()
        return super.mouseReleased(event)
    }

    // Actions

    private fun openPatternEditor() {
        minecraft.gui.setScreen(BannerPatternScreen(this, preview, draft) { patterns, bannerStyle ->
            draft = draft.copy(patterns = patterns, bannerStyle = bannerStyle)
        })
    }

    private fun openDesigns() {
        minecraft.gui.setScreen(DesignsScreen(this, preview, draft) { loaded ->
            draft = loaded
            material = loaded.trim?.material ?: DEFAULT_MATERIAL
            colorText = loaded.color?.let(::hex).orEmpty()
            colorCursor = colorText.length
        })
    }

    private fun cycleColorPreset() {
        val step = if (minecraft.hasShiftDown()) -1 else 1
        val next = COLOR_PRESETS[(COLOR_PRESETS.indexOf(draft.color) + step).mod(COLOR_PRESETS.size)]
        draft = draft.copy(color = next)
        colorBox.setValue(next?.let(::hex).orEmpty())
        colorPreset.message = colorName()
    }

    private fun onColorTyped(text: String) {
        colorText = text
        val match = HEX_PATTERN.matchEntire(text.trim())
        val valid = text.isBlank() || match != null
        if (valid) draft = draft.copy(color = match?.groupValues?.get(1)?.toInt(16))
        colorBox.setTextColor(if (valid) TEXT_COLOR else ERROR_COLOR)
        colorPreset.message = colorName()
    }

    private fun colorName(): Text {
        val color = draft.color ?: return tr("none")
        val dye = DyeColor.entries.firstOrNull { (it.textureDiffuseColor and 0xFFFFFF) == color }
        return dye?.let(CosmeticCatalog::dyeName) ?: tr("color.custom")
    }

    private fun reset() {
        draft = ElytraCosmetic.EMPTY
        material = DEFAULT_MATERIAL
        colorText = ""
        colorCursor = 0
        needsRebuild = true
    }

    // Helpers

    private fun label(row: Int, key: String) {
        labels += tr(key) to layout.rowY(row)
    }

    private fun <T> add(key: String, widget: T): T where T : GuiEventListener, T : Renderable, T : NarratableEntry {
        widgetKeys[widget] = key
        return addRenderableWidget(widget)
    }

    private fun toggle(x: Int, y: Int, key: String, initial: Boolean, onChange: (Boolean) -> Unit) {
        add(key, CycleButton.onOffBuilder(initial).create(x, y, layout.half, EditorLayout.WIDGET_HEIGHT, tr(key)) { _, value -> onChange(value) })
    }

    private fun hex(color: Int): String = "#%06X".format(color and 0xFFFFFF)
}

internal fun tr(key: String, vararg args: Any): Text = Text.translatable("screen.elytratrims.cosmetic.$key", *args)
