package dev.kikugie.elytratrims.cosmetic.gui

import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.cosmetic.CosmeticCatalog
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic.PatternLayer
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetics
import dev.kikugie.elytratrims.vanilla
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.CommonComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.math.PI

/**
 * Editor for the local player's elytra cosmetic with a live preview.
 *
 * Closing with Escape or "Done" saves the changes, "Cancel" discards them.
 */
class ElytraCosmeticScreen(private val parent: Screen?) : Screen(tr("title")) {
    private companion object {
        const val ROWS = 9
        const val WIDGET_HEIGHT = 20
        const val LABEL_WIDTH = 74
        const val STATUS_TICKS = 60
        const val TEXT_COLOR = -1
        const val HINT_COLOR = 0xFFA0A0A0.toInt()
        const val ERROR_COLOR = 0xFFFF5555.toInt()

        val DEFAULT_MATERIAL: Identifier = vanilla("gold")
        val HEX_PATTERN = Regex("#?([0-9a-fA-F]{6})")

        /** `null` stands for "undyed". */
        val COLOR_PRESETS: List<Int?> = listOf(null) + DyeColor.entries.map { it.textureDiffuseColor and 0xFFFFFF }
    }

    private val registries: RegistryAccess? get() = minecraft.level?.registryAccess()

    private var enabled: Boolean = CosmeticSettings.data.enabled
    private var draft: ElytraCosmetic = CosmeticSettings.data.cosmetic
    private var material: Identifier = draft.trim?.material ?: DEFAULT_MATERIAL
    private var selectedLayer: Int = 0

    private var previewYaw: Float = 30F
    private var draggingPreview: Boolean = false
    private var needsRebuild: Boolean = false
    private var status: Text? = null
    private var statusTicks: Int = 0

    private val trimPatterns: List<Identifier> by lazy { CosmeticCatalog.trimPatterns(minecraft.resourceManager) }
    private val trimMaterials: List<Identifier> by lazy { CosmeticCatalog.trimMaterials(registries) }
    private val bannerPatterns: List<Identifier> by lazy { CosmeticCatalog.bannerPatterns(registries) }

    // Layout, recomputed in init()
    private var previewRight = 0
    private var labelX = 0
    private var controlsX = 0
    private var controlsWidth = 0
    private var rowStep = 0
    private var top = 0
    private val labels = mutableListOf<Pair<Text, Int>>()
    private var swatchX = 0
    private var swatchY = 0
    private var layerCounterX = 0
    private var layerCounterY = 0

    private lateinit var colorPreset: Button
    private lateinit var colorBox: EditBox

    override fun init() {
        labels.clear()
        previewRight = (width * 0.38F).toInt().coerceIn(110, 260)
        labelX = previewRight + 8
        controlsX = labelX + LABEL_WIDTH
        controlsWidth = (width - controlsX - 10).coerceAtMost(230)
        top = 26
        rowStep = ((height - 34 - top) / ROWS).coerceIn(WIDGET_HEIGHT + 1, WIDGET_HEIGHT + 4)
        val half = (controlsWidth - 4) / 2
        val secondColumn = controlsX + controlsWidth - half

        // Row 0: master switch
        label(0, "enabled")
        addRenderableWidget(CycleButton.onOffBuilder(enabled).displayOnlyValue()
            .create(controlsX, rowY(0), controlsWidth, WIDGET_HEIGHT, tr("enabled")) { _, value -> enabled = value })

        // Row 1-2: trim
        label(1, "trim_pattern")
        val noTrim = Optional.empty<Identifier>()
        val patternValues = listOf(noTrim) + (trimPatterns + listOfNotNull(draft.trim?.pattern)).distinct().map { Optional.of(it) }
        addRenderableWidget(CycleButton.builder<Optional<Identifier>>({ id ->
            id.map { CosmeticCatalog.trimPatternName(it, registries) }.orElse(tr("none"))
        }, Optional.ofNullable(draft.trim?.pattern))
            .withValues(patternValues)
            .displayOnlyValue()
            .create(controlsX, rowY(1), controlsWidth, WIDGET_HEIGHT, tr("trim_pattern")) { _, value ->
                draft = draft.copy(trim = value.getOrNull()?.let { ElytraCosmetic.Trim(it, material) })
                needsRebuild = true
            })

        label(2, "trim_material")
        addRenderableWidget(CycleButton.builder<Identifier>({ CosmeticCatalog.trimMaterialName(it, registries) }, material)
            .withValues((trimMaterials + material).distinct())
            .displayOnlyValue()
            .create(controlsX, rowY(2), controlsWidth, WIDGET_HEIGHT, tr("trim_material")) { _, value ->
                material = value
                draft.trim?.let { draft = draft.copy(trim = it.copy(material = value)) }
            }).active = draft.trim != null

        // Row 3: dye color
        label(3, "color")
        colorPreset = addRenderableWidget(Button.builder(colorName()) { cycleColorPreset() }
            .bounds(controlsX, rowY(3), half, WIDGET_HEIGHT)
            .tooltip(Tooltip.create(tr("color.tooltip")))
            .build())
        colorBox = EditBox(font, secondColumn, rowY(3), half - WIDGET_HEIGHT - 4, WIDGET_HEIGHT, tr("color")).apply {
            setMaxLength(7)
            setHint(Text.literal("#RRGGBB"))
            setValue(draft.color?.let(::hex).orEmpty())
            setResponder(::onColorTyped)
        }
        addRenderableWidget(colorBox)
        swatchX = controlsX + controlsWidth - WIDGET_HEIGHT
        swatchY = rowY(3)

        // Row 4-5: effects
        label(4, "effects")
        toggle(controlsX, rowY(4), half, "rainbow", draft.rainbow) { draft = draft.copy(rainbow = it) }
        toggle(secondColumn, rowY(4), half, "glow", draft.glow) { draft = draft.copy(glow = it) }
        toggle(controlsX, rowY(5), half, "gateway", draft.gateway) { draft = draft.copy(gateway = it) }
        toggle(secondColumn, rowY(5), half, "animation", draft.animation) { draft = draft.copy(animation = it) }

        // Row 6-8: banner patterns
        label(6, "patterns")
        addRenderableWidget(CycleButton.booleanBuilder(tr("pattern_style.banner"), tr("pattern_style.shield"), draft.bannerStyle)
            .create(controlsX, rowY(6), half, WIDGET_HEIGHT, tr("pattern_style")) { _, value -> draft = draft.copy(bannerStyle = value) })
        addRenderableWidget(Button.builder(tr("import")) { importFromHand() }
            .bounds(secondColumn, rowY(6), half, WIDGET_HEIGHT)
            .tooltip(Tooltip.create(tr("import.tooltip")))
            .build())

        initLayerWidgets(half, secondColumn)

        // Footer
        val footerWidth = 74
        val footerX = labelX + (width - labelX - (footerWidth * 3 + 8)) / 2
        val footerY = height - 26
        addRenderableWidget(Button.builder(tr("reset")) { reset() }.bounds(footerX, footerY, footerWidth, WIDGET_HEIGHT).build())
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL) { minecraft.gui.setScreen(parent) }
            .bounds(footerX + footerWidth + 4, footerY, footerWidth, WIDGET_HEIGHT).build())
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }
            .bounds(footerX + (footerWidth + 4) * 2, footerY, footerWidth, WIDGET_HEIGHT).build())
    }

    private fun initLayerWidgets(half: Int, secondColumn: Int) {
        val layers = draft.patterns
        selectedLayer = selectedLayer.coerceIn(0, (layers.size - 1).coerceAtLeast(0))

        label(7, "layer")
        addRenderableWidget(Button.builder(Text.literal("<")) { selectLayer(-1) }
            .bounds(controlsX, rowY(7), WIDGET_HEIGHT, WIDGET_HEIGHT).build()).active = selectedLayer > 0
        addRenderableWidget(Button.builder(Text.literal(">")) { selectLayer(1) }
            .bounds(controlsX + half - WIDGET_HEIGHT, rowY(7), WIDGET_HEIGHT, WIDGET_HEIGHT).build()).active = selectedLayer < layers.size - 1
        layerCounterX = controlsX + half / 2
        layerCounterY = rowY(7) + (WIDGET_HEIGHT - font.lineHeight) / 2 + 1

        val quarter = (half - 4) / 2
        addRenderableWidget(Button.builder(tr("add_layer")) { addLayer() }
            .bounds(secondColumn, rowY(7), quarter, WIDGET_HEIGHT).build()).active =
            bannerPatterns.isNotEmpty() && layers.size < ElytraCosmetic.MAX_PATTERN_LAYERS
        addRenderableWidget(Button.builder(tr("remove_layer")) { removeLayer() }
            .bounds(secondColumn + half - quarter, rowY(7), quarter, WIDGET_HEIGHT).build()).active = layers.isNotEmpty()

        val layer = layers.getOrNull(selectedLayer) ?: return
        addRenderableWidget(CycleButton.builder<Identifier>({ CosmeticCatalog.bannerPatternName(it, layer.color, registries) }, layer.pattern)
            .withValues((bannerPatterns + layer.pattern).distinct())
            .displayOnlyValue()
            .create(controlsX, rowY(8), half, WIDGET_HEIGHT, tr("layer")) { _, value -> updateLayer { it.copy(pattern = value) } })
        addRenderableWidget(CycleButton.builder<DyeColor>(CosmeticCatalog::dyeName, layer.color)
            .withValues(DyeColor.entries)
            .displayOnlyValue()
            .create(secondColumn, rowY(8), half, WIDGET_HEIGHT, tr("color")) { _, value ->
                updateLayer { it.copy(color = value) }
                needsRebuild = true // The pattern name includes the color
            })
    }

    override fun tick() {
        if (statusTicks > 0 && --statusTicks == 0) status = null
        if (needsRebuild) {
            needsRebuild = false
            rebuildWidgets()
        }
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        CosmeticSettings.update(CosmeticSettings.Data(enabled, draft))
        minecraft.gui.setScreen(parent)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        graphics.centeredText(font, title, width / 2, 9, TEXT_COLOR)
        for ((text, y) in labels) graphics.text(font, text, labelX, y + (WIDGET_HEIGHT - font.lineHeight) / 2 + 1, TEXT_COLOR)

        val counter = if (draft.patterns.isEmpty()) tr("no_layers") else Text.literal("${selectedLayer + 1}/${draft.patterns.size}")
        graphics.centeredText(font, counter, layerCounterX, layerCounterY, TEXT_COLOR)

        graphics.fill(swatchX, swatchY, swatchX + WIDGET_HEIGHT, swatchY + WIDGET_HEIGHT, 0xFFA0A0A0.toInt())
        val color = draft.color
        if (color != null) graphics.fill(swatchX + 1, swatchY + 1, swatchX + WIDGET_HEIGHT - 1, swatchY + WIDGET_HEIGHT - 1, color or (0xFF shl 24))
        else graphics.centeredText(font, "-", swatchX + WIDGET_HEIGHT / 2, swatchY + 6, HINT_COLOR)

        status?.let { graphics.centeredText(font, it, labelX + (width - labelX) / 2, rowY(ROWS) + 2, TEXT_COLOR) }

        graphics.centeredText(font, tr("preview_hint"), previewRight / 2, height - 20, HINT_COLOR)
        extractPreview(graphics, partialTick)
    }

    private fun extractPreview(graphics: GuiGraphicsExtractor, partialTick: Float) {
        val player = minecraft.player ?: return
        val state = minecraft.entityRenderDispatcher.getRenderer(player).createRenderState(player, partialTick)
        if (state !is AvatarRenderState) return

        state.shadowPieces.clear()
        state.outlineColor = 0
        state.nameTag = null
        state.bodyRot = previewYaw
        state.yRot = 0F
        state.xRot = 0F
        state.boundingBoxWidth /= state.scale
        state.boundingBoxHeight /= state.scale
        state.scale = 1F
        // Show the cosmetic even when the player isn't wearing an elytra right now.
        if (!state.chestEquipment.has(DataComponents.GLIDER)) state.chestEquipment = ItemStack(Items.ELYTRA)
        state.setData(ElytraCosmetics.RENDER_KEY, draft.takeIf { enabled && !it.isEmpty })

        val x0 = 6
        val y0 = top
        val x1 = previewRight - 6
        val y1 = height - 28
        val size = ((y1 - y0) / 2.4F).coerceAtMost((x1 - x0) / 1.2F)
        val translation = Vector3f(0F, state.boundingBoxHeight / 2F + 0.0625F, 0F)
        graphics.entity(state, size, translation, Quaternionf().rotateZ(PI.toFloat()), null, x0, y0, x1, y1)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.x() < previewRight && event.y() > top) {
            draggingPreview = true
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (!draggingPreview) return super.mouseDragged(event, dx, dy)
        previewYaw = (previewYaw - dx.toFloat() * 2F) % 360F
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        draggingPreview = false
        return super.mouseReleased(event)
    }

    // Actions

    private fun cycleColorPreset() {
        val step = if (minecraft.hasShiftDown()) -1 else 1
        val next = COLOR_PRESETS[(COLOR_PRESETS.indexOf(draft.color) + step).mod(COLOR_PRESETS.size)]
        draft = draft.copy(color = next)
        colorBox.setValue(next?.let(::hex).orEmpty())
        colorPreset.message = colorName()
    }

    private fun onColorTyped(text: String) {
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

    private fun selectLayer(step: Int) {
        selectedLayer += step
        needsRebuild = true
    }

    private fun addLayer() {
        val pattern = bannerPatterns.firstOrNull() ?: return
        val layers = draft.patterns
        if (layers.size >= ElytraCosmetic.MAX_PATTERN_LAYERS) return
        val insertAt = if (layers.isEmpty()) 0 else selectedLayer + 1
        draft = draft.copy(patterns = layers.toMutableList().apply { add(insertAt, PatternLayer(pattern, DyeColor.WHITE)) })
        selectedLayer = insertAt
        needsRebuild = true
    }

    private fun removeLayer() {
        val layers = draft.patterns
        if (selectedLayer !in layers.indices) return
        draft = draft.copy(patterns = layers.toMutableList().apply { removeAt(selectedLayer) })
        selectedLayer = (selectedLayer - 1).coerceAtLeast(0)
        needsRebuild = true
    }

    private fun updateLayer(transform: (PatternLayer) -> PatternLayer) {
        val layers = draft.patterns
        if (selectedLayer !in layers.indices) return
        draft = draft.copy(patterns = layers.toMutableList().apply { this[selectedLayer] = transform(this[selectedLayer]) })
    }

    /** Copies the pattern layers of a banner or shield held in either hand. */
    private fun importFromHand() {
        val player = minecraft.player ?: return
        val source = InteractionHand.entries.asSequence()
            .map { player.getItemInHand(it).get(DataComponents.BANNER_PATTERNS) }
            .firstOrNull { it != null && it.layers().isNotEmpty() }
        if (source == null) return showStatus(tr("import.failed"))

        val layers = source.layers().take(ElytraCosmetic.MAX_PATTERN_LAYERS).map { layer ->
            val id = layer.pattern().unwrapKey().map { it.identifier() }.orElseGet { layer.pattern().value().assetId() }
            PatternLayer(id, layer.color())
        }
        draft = draft.copy(patterns = layers)
        selectedLayer = 0
        showStatus(tr("import.success", layers.size))
        needsRebuild = true
    }

    private fun reset() {
        draft = ElytraCosmetic.EMPTY
        material = DEFAULT_MATERIAL
        selectedLayer = 0
        needsRebuild = true
    }

    // Helpers

    private fun showStatus(text: Text) {
        status = text
        statusTicks = STATUS_TICKS
    }

    private fun rowY(row: Int): Int = top + row * rowStep

    private fun label(row: Int, key: String) {
        labels += tr(key) to rowY(row)
    }

    private fun toggle(x: Int, y: Int, width: Int, key: String, initial: Boolean, onChange: (Boolean) -> Unit) {
        addRenderableWidget(CycleButton.onOffBuilder(initial).create(x, y, width, WIDGET_HEIGHT, tr(key)) { _, value -> onChange(value) })
    }

    private fun hex(color: Int): String = "#%06X".format(color and 0xFFFFFF)
}

private fun tr(key: String, vararg args: Any): Text = Text.translatable("screen.elytratrims.cosmetic.$key", *args)
