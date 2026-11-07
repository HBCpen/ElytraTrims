@file:Suppress("UnstableApiUsage")

package dev.kikugie.elytratrims.test

import com.mojang.blaze3d.platform.InputConstants
import dev.kikugie.elytratrims.cosmetic.CosmeticSettings
import dev.kikugie.elytratrims.cosmetic.CosmeticStacks
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic.PatternLayer
import dev.kikugie.elytratrims.cosmetic.gui.BannerPatternScreen
import dev.kikugie.elytratrims.cosmetic.gui.ElytraCosmeticScreen
import dev.kikugie.elytratrims.cosmetic.gui.ElytraPreview
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.vanilla
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
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

        val (width, height) = context.computeOnClient<Pair<Int, Int>, Throwable> { it.window.width to it.window.height }
        val guiScale = context.computeOnClient<Int, Throwable> { it.options.guiScale().get() }
        try {
            // A common setup: 1920x1080 at GUI scale 2.
            context.setWindow(1920, 1080, 2)
            testEditor(context)
            testPatternEditor(context)

            // Smallest GUI size supported by Minecraft (320x240 at GUI scale 2).
            context.setScreen { ElytraCosmeticScreen(null) }
            context.setWindow(640, 480, 2)
            context.takeScreenshot("cosmetic_editor_small")
            context.click { it.widget<Button>(translated("edit_patterns", 1)) }
            context.takeScreenshot("cosmetic_patterns_small")
        } finally {
            context.setScreen { null }
            context.input.resizeWindow(width, height)
            context.runOnClient<Throwable> { it.options.guiScale().set(guiScale); it.resizeGui() }
        }

        context.runOnClient<Throwable> { CosmeticSettings.update(CosmeticSettings.Data()) }
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
        context.input.pressKey(InputConstants.KEY_TAB)
        context.input.pressKey(InputConstants.KEY_TAB)
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

    private fun testPatternEditor(context: ClientGameTestContext) {
        context.click { it.widget<Button>(translated("edit_patterns", 1)) }
        context.runOnClient<Throwable> { check(it.gui.screen() is BannerPatternScreen) { "The pattern editor did not open" } }
        context.takeScreenshot("cosmetic_patterns")

        // Pick a color, then a pattern: a new layer is stacked on top.
        context.clickAt { (it as BannerPatternScreen).swatchCenter(DyeColor.RED) }
        context.clickAt { (it as BannerPatternScreen).patternTileCenter(vanilla("stripe_bottom"))!! }
        context.runOnClient<Throwable> { client ->
            val layers = (client.gui.screen() as BannerPatternScreen).currentLayers
            check(layers == listOf(COSMETIC.patterns[0], PatternLayer(vanilla("stripe_bottom"), DyeColor.RED))) { "Unexpected layers: $layers" }
        }

        // Selecting a layer makes the palette edit it.
        context.clickAt { (it as BannerPatternScreen).layerRowCenter(0)!! }
        context.clickAt { (it as BannerPatternScreen).swatchCenter(DyeColor.BLUE) }
        context.runOnClient<Throwable> { client ->
            val layers = (client.gui.screen() as BannerPatternScreen).currentLayers
            check(layers[0] == PatternLayer(vanilla("creeper"), DyeColor.BLUE)) { "Selected layer was not recolored: $layers" }
        }
        context.takeScreenshot("cosmetic_patterns_edited")

        // Confirming returns to the editor, whose Done button saves everything.
        context.click { it.widget<Button>(Component.translatable("gui.done")) }
        context.click { it.widget<Button>(Component.translatable("gui.done")) }
        context.runOnClient<Throwable> { client ->
            check(client.gui.screen() == null) { "The editor did not close" }
            val saved = CosmeticSettings.data.cosmetic.patterns
            check(saved == listOf(PatternLayer(vanilla("creeper"), DyeColor.BLUE), PatternLayer(vanilla("stripe_bottom"), DyeColor.RED))) {
                "Unexpected saved patterns: $saved"
            }
            CosmeticSettings.update(CosmeticSettings.Data(true, COSMETIC))
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
