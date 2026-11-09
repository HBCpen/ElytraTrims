@file:Suppress("UnstableApiUsage")

package dev.kikugie.elytratrims.test

import com.mojang.blaze3d.platform.InputConstants
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings
import dev.kikugie.elytratrims.cosmetic.CosmeticStacks
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic.PatternLayer
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetics
import dev.kikugie.elytratrims.cosmetic.gui.BannerPatternScreen
import dev.kikugie.elytratrims.cosmetic.gui.DesignsScreen
import dev.kikugie.elytratrims.cosmetic.gui.ElytraCosmeticScreen
import dev.kikugie.elytratrims.cosmetic.gui.ElytraPreview
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.vanilla
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.client.Minecraft
import net.minecraft.client.CameraType
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import com.google.gson.JsonParser
import dev.kikugie.elytratrims.render.GLOWING_WINGS_MIN_LIGHT
import dev.kikugie.elytratrims.render.glowingWingsLight
import net.minecraft.client.resources.model.EquipmentClientInfo
import com.mojang.serialization.JsonOps
import java.util.Optional

/**
 * Tests for the client-side cosmetics: rendering on the local player and the editor screens,
 * taking screenshots for manual inspection along the way.
 */
object ElytraCosmeticGameTest : FabricClientGameTest {
    private val COSMETIC = ElytraCosmetic(
        // A pattern that only exists client-side, to check that it renders without server support.
        trim = ElytraCosmetic.Trim(elytratrims("stars"), vanilla("gold")),
        color = 0x3355FF,
        patterns = listOf(PatternLayer(vanilla("creeper"), DyeColor.LIME)),
        glow = true,
    )

    override fun runTest(context: ClientGameTestContext): Unit = context.runInWorld {
        server.runCommand("item replace entity @a armor.chest with minecraft:elytra")
        context.waitTicks(5)

        context.runOnClient<Throwable> { client ->
            val registries = client.level!!.registryAccess()
            val stack = CosmeticStacks.apply(COSMETIC, ItemStack(Items.ELYTRA), registries)
            check(stack.get(DataComponents.TRIM)?.pattern()?.value()?.assetId() == elytratrims("stars")) { "Trim was not applied" }
            check(stack.get(DataComponents.DYED_COLOR)?.rgb() == 0x3355FF) { "Color was not applied" }
            check(stack.get(DataComponents.BANNER_PATTERNS)?.layers()?.size == 1) { "Patterns were not applied" }
            check(stack has ETItemFlag.GLOW) { "Glow flag was not applied" }

            val worn = client.player!!.getItemBySlot(EquipmentSlot.CHEST)
            check(worn.get(DataComponents.TRIM) == null) { "The real item must stay untouched" }

            testRemovedEffects(registries)
            testGlowingWingsLight(stack)

            CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC))
            client.options.cameraType = CameraType.THIRD_PERSON_BACK
            client.player!!.yRot = 0F
            client.player!!.xRot = 20F
        }
        context.waitTicks(20)
        context.takeScreenshot("cosmetic_world")

        context.runOnClient<Throwable> { CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC.copy(rainbow = true))) }
        context.waitTicks(5)
        context.takeScreenshot("cosmetic_rainbow")
        context.runOnClient<Throwable> { CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC)) }

        testVirtualWings(context)

        val (width, height) = context.computeOnClient<Pair<Int, Int>, Throwable> { it.window.width to it.window.height }
        val guiScale = context.computeOnClient<Int, Throwable> { it.options.guiScale().get() }
        try {
            // A common setup: 1920x1080 at GUI scale 2.
            context.setWindow(1920, 1080, 2)
            testEditor(context)
            testPatternEditor(context)
            testDesigns(context)

            // Smallest GUI size supported by Minecraft (320x240 at GUI scale 2).
            context.setScreen { ElytraCosmeticScreen(null) }
            context.setWindow(640, 480, 2)
            context.takeScreenshot("cosmetic_editor_small")
            context.click { it.widget<Button>(translated("edit_patterns", 1)) }
            context.takeScreenshot("cosmetic_patterns_small")
            context.setScreen { ElytraCosmeticScreen(null) }
            context.click { it.widget<Button>(translated("designs.open")) }
            context.takeScreenshot("cosmetic_designs_small")
        } finally {
            context.setScreen { null }
            context.input.resizeWindow(width, height)
            context.runOnClient<Throwable> { it.options.guiScale().set(guiScale); it.resizeGui() }
        }

        testGlowLight(context)
        context.runOnClient<Throwable> { CosmeticSettings.update(CosmeticSettings.Data()) }
    }

    /**
     * Glowing wings are a light source for LambDynamicLights (installed in the dev client): in a dark room, the floor
     * around the player gets brighter when the cosmetic glows.
     */
    private fun TestSingleplayerContext.testGlowLight(context: ClientGameTestContext) {
        context.runOnClient<Throwable> { client ->
            val player = client.player!!
            CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC))
            check(ElytraCosmetics.hasGlowingWings(player)) { "Glowing wings were not detected" }
            CosmeticSettings.update(CosmeticSettings.Data(false, COSMETIC))
            check(!ElytraCosmetics.hasGlowingWings(player)) { "A disabled cosmetic must not glow" }
            CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC.copy(glow = false)))
            check(!ElytraCosmetics.hasGlowingWings(player)) { "Wings without glow were taken as glowing" }
            client.options.cameraType = CameraType.THIRD_PERSON_BACK
            player.yRot = 0F
            player.xRot = 20F
        }
        server.runCommand("item replace entity @a armor.chest with minecraft:diamond_chestplate")
        context.waitTicks(2)
        context.runOnClient<Throwable> { client ->
            CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC))
            check(!ElytraCosmetics.hasGlowingWings(client.player!!)) { "Wings that aren't drawn must not glow" }
            CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC.copy(visibility = ElytraCosmetic.Visibility.OVER_ARMOR)))
            check(ElytraCosmetics.hasGlowingWings(client.player!!)) { "Wings drawn over a chestplate must glow" }
        }
        server.runCommand("item replace entity @a armor.chest with minecraft:elytra")
        server.runCommand("effect give @a minecraft:invisibility 5 0 true")
        context.waitTicks(2)
        context.runOnClient<Throwable> { client ->
            CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC))
            check(client.player!!.isInvisible && ElytraCosmetics.hasGlowingWings(client.player!!)) { "Wings stay visible, and glowing, on invisible players" }
        }
        server.runCommand("effect clear @a minecraft:invisibility")
        // Without it, glowing wings only glow themselves; the rest of the tests check that the mod works that way too.
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("lambdynlights")) return

        server.runCommand("time set midnight")
        server.runCommand("fill ~-4 ~-1 ~-4 ~4 ~5 ~4 minecraft:stone hollow")
        try {
            context.runOnClient<Throwable> { CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC.copy(glow = false))) }
            context.waitTicks(40)
            val dark = floorBrightness(context.takeScreenshot("cosmetic_glow_off"))
            context.runOnClient<Throwable> { CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC)) }
            context.waitTicks(40)
            val lit = floorBrightness(context.takeScreenshot("cosmetic_glow_light"))
            check(lit - dark > 4) { "Glowing wings didn't light up the floor: $dark without glow, $lit with it" }
        } finally {
            server.runCommand("fill ~-4 ~ ~-4 ~4 ~5 ~4 minecraft:air")
            server.runCommand("fill ~-4 ~-1 ~-4 ~4 ~-1 ~4 minecraft:grass_block")
            server.runCommand("time set day")
        }
    }

    /** Mean brightness (0-255) of the floor to the lower left of the player, seen from behind. */
    private fun floorBrightness(screenshot: java.nio.file.Path): Double {
        val image = javax.imageio.ImageIO.read(screenshot.toFile())
        var sum = 0.0
        var count = 0
        for (y in (image.height * 0.65).toInt() until (image.height * 0.85).toInt())
            for (x in (image.width * 0.15).toInt() until (image.width * 0.35).toInt()) {
                val rgb = image.getRGB(x, y)
                sum += ((rgb shr 16 and 0xFF) + (rgb shr 8 and 0xFF) + (rgb and 0xFF)) / 3.0
                count++
            }
        return sum / count
    }

    /**
     * The cosmic and animation effects are no longer cosmetics: configs, designs and synced records that still have
     * them load without them, and the cosmetic replaces them on a real elytra decorated with them.
     */
    private fun testRemovedEffects(registries: net.minecraft.core.RegistryAccess) {
        val legacy = JsonParser.parseString("""{"color":3364351,"glow":true,"gateway":true,"animation":true}""")
        val loaded = ElytraCosmetic.CODEC.parse(JsonOps.INSTANCE, legacy).result().orElse(null)
        check(loaded == ElytraCosmetic(color = 0x3355FF, glow = true)) { "A cosmetic with removed effects didn't load: $loaded" }

        val decorated = ItemStack(Items.ELYTRA).also { ETItemFlag.GATEWAY[it] = true; ETItemFlag.BAD_APPLE[it] = true }
        val stack = CosmeticStacks.apply(COSMETIC, decorated, registries)
        check(!(stack has ETItemFlag.GATEWAY) && !(stack has ETItemFlag.BAD_APPLE)) { "The cosmetic kept effects of the real item" }
        check(decorated has ETItemFlag.GATEWAY) { "The real item must stay untouched" }
    }

    /** Glowing wings get at least a faint block light; other layers, other stacks and brighter light are left alone. */
    private fun testGlowingWingsLight(glowing: ItemStack) {
        val wings = EquipmentClientInfo.LayerType.WINGS
        val dark = 0x00F0_0000 // Sky light 15, block light 0.
        val expected = 0x00F0_0000 or (GLOWING_WINGS_MIN_LIGHT shl 4)
        check(glowingWingsLight(wings, glowing, dark) == expected) { "Glowing wings weren't lit: ${glowingWingsLight(wings, glowing, dark)}" }
        check(glowingWingsLight(wings, glowing, 0xF000F0) == 0xF000F0) { "Brighter light must be kept" }
        check(glowingWingsLight(wings, ItemStack(Items.ELYTRA), dark) == dark) { "Wings without glow were lit" }
        check(glowingWingsLight(EquipmentClientInfo.LayerType.HUMANOID, glowing, dark) == dark) { "Other layers were lit" }
    }

    private fun testEditor(context: ClientGameTestContext) {
        context.setScreen { ElytraCosmeticScreen(null) }
        context.waitTicks(5)
        context.takeScreenshot("cosmetic_editor")
        for (pose in listOf(ElytraPreview.PreviewPose.SNEAKING, ElytraPreview.PreviewPose.GLIDING)) {
            context.click { screen -> screen.children().first { (it as? CycleButton<*>)?.value is ElytraPreview.PreviewPose } as AbstractWidget }
            context.takeScreenshot("cosmetic_editor_${pose.key}")
        }

        // Cycling a value rebuilds the widgets; keyboard focus must stay on the same control.
        context.runOnClient<Throwable> { it.gui.screen()!!.clearFocus() }
        // Fourth widget: master switch, sync, elytra visibility, trim pattern.
        repeat(4) { context.input.pressKey(InputConstants.KEY_TAB) }
        context.input.pressKey(InputConstants.KEY_SPACE)
        context.waitTicks(2)
        context.runOnClient<Throwable> { client ->
            val focused = client.gui.screen()?.focused
            check((focused as? CycleButton<*>)?.value is Optional<*>) { "Focus moved away from the trim button: $focused" }
        }

        // An incomplete hex color must survive a rebuild, here triggered by resizing.
        context.runOnClient<Throwable> { client ->
            val screen = client.gui.screen()!!
            val box = screen.widget<EditBox>()
            screen.focused = box
            box.setValue("")
        }
        context.input.typeChars("#12")
        context.setWindow(1600, 900, 2)
        context.runOnClient<Throwable> { client ->
            val screen = client.gui.screen()!!
            val box = screen.widget<EditBox>()
            check(box.value == "#12") { "Unfinished color input was lost: '${box.value}'" }
            check(screen.focused === box) { "Focus left the color box" }
            box.setValue("#3355FF")
        }
        context.setWindow(1920, 1080, 2)
    }

    /** With "over chestplate", the elytra is drawn on top of a worn chestplate, which stays visible. */
    private fun TestSingleplayerContext.testVirtualWings(context: ClientGameTestContext) {
        server.runCommand("item replace entity @a armor.chest with minecraft:diamond_chestplate")
        context.runOnClient<Throwable> { CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC.copy(visibility = ElytraCosmetic.Visibility.OVER_ARMOR))) }
        context.waitTicks(10)
        context.runOnClient<Throwable> { client -> check(virtualWings(client)) { "Virtual wings are missing over the chestplate" } }
        context.takeScreenshot("cosmetic_over_chestplate")

        context.runOnClient<Throwable> { client ->
            CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC))
            check(!virtualWings(client)) { "Virtual wings must only be drawn when enabled" }
        }
        server.runCommand("item replace entity @a armor.chest with minecraft:elytra")
        context.waitTicks(5)
    }

    private fun virtualWings(client: Minecraft): Boolean {
        val player = client.player!!
        val state = client.entityRenderDispatcher.getRenderer(player).createRenderState(player, 1F)
        return state.getData(ElytraCosmetics.VIRTUAL_WINGS_KEY) == true
    }

    private fun testPatternEditor(context: ClientGameTestContext) {
        context.click { it.widget<Button>(translated("edit_patterns", 1)) }
        context.runOnClient<Throwable> { check(it.gui.screen() is BannerPatternScreen) { "The pattern editor did not open" } }
        context.takeScreenshot("cosmetic_patterns")
        fun layers() = context.computeOnClient<List<PatternLayer>, Throwable> { (it.gui.screen() as BannerPatternScreen).currentLayers }

        // "+ New Layer" adds a layer right away and selects it; the palette and grid then edit it.
        context.clickAt { (it as BannerPatternScreen).newLayerRowCenter()!! }
        check(layers().size == 2) { "New layer was not added: ${layers()}" }
        context.clickAt { (it as BannerPatternScreen).swatchCenter(DyeColor.RED) }
        context.clickAt { (it as BannerPatternScreen).patternTileCenter(vanilla("stripe_bottom"))!! }
        check(layers() == listOf(COSMETIC.patterns[0], PatternLayer(vanilla("stripe_bottom"), DyeColor.RED))) { "Unexpected layers: ${layers()}" }

        // Selecting a layer makes the palette edit it.
        context.clickAt { (it as BannerPatternScreen).layerRowCenter(0)!! }
        context.clickAt { (it as BannerPatternScreen).swatchCenter(DyeColor.BLUE) }
        check(layers()[0] == PatternLayer(vanilla("creeper"), DyeColor.BLUE)) { "Selected layer was not recolored: ${layers()}" }
        context.takeScreenshot("cosmetic_patterns_edited")

        // Each row has buttons to move and remove its layer.
        context.clickAt { (it as BannerPatternScreen).layerRowCenter(0, "up")!! }
        check(layers().map { it.pattern } == listOf(vanilla("stripe_bottom"), vanilla("creeper"))) { "Layer was not moved up: ${layers()}" }
        context.clickAt { (it as BannerPatternScreen).layerRowCenter(0, "remove")!! }
        check(layers() == listOf(PatternLayer(vanilla("creeper"), DyeColor.BLUE))) { "Layer was not removed: ${layers()}" }

        // Confirming returns to the editor, whose Done button saves everything.
        context.click { it.widget<Button>(Component.translatable("gui.done")) }
        context.click { it.widget<Button>(Component.translatable("gui.done")) }
        context.runOnClient<Throwable> { client ->
            check(client.gui.screen() == null) { "The editor did not close" }
            val saved = CosmeticSettings.data.cosmetic.patterns
            check(saved == listOf(PatternLayer(vanilla("creeper"), DyeColor.BLUE))) { "Unexpected saved patterns: $saved" }
        }
    }

    private fun testDesigns(context: ClientGameTestContext) {
        val edited = context.computeOnClient<ElytraCosmetic, Throwable> { CosmeticSettings.data.cosmetic }
        context.setScreen { ElytraCosmeticScreen(null) }
        context.click { it.widget<Button>(translated("designs.open")) }
        fun names() = context.computeOnClient<List<String>, Throwable> { (it.gui.screen() as DesignsScreen).designNames }

        context.runOnClient<Throwable> { client ->
            val screen = client.gui.screen()!!
            screen.focused = screen.widget<EditBox>()
        }
        context.input.typeChars("Test")
        context.click { it.widget<Button>(translated("designs.save")) }
        check(names() == listOf("Test")) { "Design was not saved: ${names()}" }
        check(context.computeOnClient<Int, Throwable> { CosmeticSettings.data.designs.size } == 1) { "Design was not persisted" }

        // Copy and paste through the clipboard; the pasted copy gets a unique name.
        context.click { it.widget<Button>(translated("designs.copy")) }
        context.click { it.widget<Button>(translated("designs.paste")) }
        check(names() == listOf("Test", "Test (2)")) { "Design was not pasted: ${names()}" }
        context.takeScreenshot("cosmetic_designs")

        // Deleting needs a second click.
        context.clickAt { (it as DesignsScreen).rowCenter(1, deleteButton = true)!! }
        check(names().size == 2) { "Deleted without confirmation" }
        context.clickAt { (it as DesignsScreen).rowCenter(1, deleteButton = true)!! }
        check(names() == listOf("Test")) { "Design was not deleted: ${names()}" }

        // Loading a design replaces the one being edited.
        context.click { it.widget<Button>(Component.translatable("gui.back")) }
        context.click { it.widget<Button>(translated("reset")) }
        context.click { it.widget<Button>(translated("designs.open")) }
        context.clickAt { (it as DesignsScreen).rowCenter(0)!! }
        context.click { it.widget<Button>(translated("designs.load")) }
        context.click { it.widget<Button>(Component.translatable("gui.done")) }
        context.runOnClient<Throwable> {
            check(CosmeticSettings.data.cosmetic == edited) { "Loaded design differs: ${CosmeticSettings.data.cosmetic}" }
        }
    }
}

private fun translated(key: String, vararg args: Any): Component = Component.translatable("screen.elytratrims.cosmetic.$key", *args)

private inline fun <reified T : AbstractWidget> Screen.widget(message: Component? = null): T =
    children().first { it is T && (message == null || it.message == message) } as T

private fun ClientGameTestContext.setWindow(width: Int, height: Int, guiScale: Int) {
    input.resizeWindow(width, height)
    runOnClient<Throwable> { it.options.guiScale().set(guiScale); it.resizeGui() }
    waitTicks(5)
}

/** Clicks at a position in GUI coordinates of the current screen. */
private fun ClientGameTestContext.clickAt(position: (Screen) -> Pair<Int, Int>) {
    val (x, y) = computeOnClient<Pair<Double, Double>, Throwable> { client ->
        val (x, y) = position(client.gui.screen()!!)
        val scale = client.window.guiScale.toDouble()
        (x + 0.5) * scale to (y + 0.5) * scale
    }
    input.setCursorPos(x, y)
    input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT)
    waitTicks(2)
}

private fun ClientGameTestContext.click(widget: (Screen) -> AbstractWidget) =
    clickAt { screen -> widget(screen).let { it.x + it.width / 2 to it.y + it.height / 2 } }
