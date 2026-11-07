@file:Suppress("UnstableApiUsage")

package dev.kikugie.elytratrims.test

import dev.kikugie.elytratrims.cosmetic.CosmeticSettings
import dev.kikugie.elytratrims.cosmetic.CosmeticStacks
import dev.kikugie.elytratrims.cosmetic.ElytraCosmetic
import dev.kikugie.elytratrims.cosmetic.gui.ElytraCosmeticScreen
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.has
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.vanilla
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.CameraType
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.Optional

/**
 * Smoke test for the client-side cosmetics: renders the local player with a cosmetic applied
 * and opens the editor, taking screenshots of both for manual inspection.
 */
object ElytraCosmeticGameTest : FabricClientGameTest {
    private val COSMETIC = ElytraCosmetic(
        // A pattern that only exists client-side, to check that it renders without server support.
        trim = ElytraCosmetic.Trim(elytratrims("stars"), vanilla("gold")),
        color = 0x3355FF,
        patterns = listOf(ElytraCosmetic.PatternLayer(vanilla("creeper"), DyeColor.LIME)),
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

        context.setScreen { ElytraCosmeticScreen(null) }
        context.waitTicks(10)
        context.takeScreenshot("cosmetic_editor")

        // Cycling a value rebuilds the widgets; keyboard focus must stay on the same control.
        context.input.pressKey(InputConstants.KEY_TAB)
        context.input.pressKey(InputConstants.KEY_TAB)
        context.input.pressKey(InputConstants.KEY_SPACE)
        context.waitTicks(2)
        context.runOnClient<Throwable> { client ->
            val focused = client.gui.screen()?.focused
            check((focused as? CycleButton<*>)?.value is Optional<*>) { "Focus moved away from the trim button: $focused" }
        }

        // Smallest GUI size supported by Minecraft (320x240 at GUI scale 2).
        val (width, height) = context.computeOnClient<Pair<Int, Int>, Throwable> { it.window.width to it.window.height }
        val guiScale = context.computeOnClient<Int, Throwable> { it.options.guiScale().get() }
        context.input.resizeWindow(640, 480)
        context.runOnClient<Throwable> { it.options.guiScale().set(2); it.resizeGui() }
        context.waitTicks(5)
        context.takeScreenshot("cosmetic_editor_small")
        context.setScreen { null }
        context.input.resizeWindow(width, height)
        context.runOnClient<Throwable> { it.options.guiScale().set(guiScale); it.resizeGui() }

        context.runOnClient<Throwable> { CosmeticSettings.update(CosmeticSettings.Data()) }
    }
}
