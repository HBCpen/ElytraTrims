package dev.kikugie.elytratrims.cosmetic.gui

import com.mojang.blaze3d.platform.InputConstants
import dev.kikugie.elytratrims.elytratrims
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping

object CosmeticKeys {
    private val CATEGORY: KeyMapping.Category = KeyMapping.Category.register(elytratrims("cosmetics"))

    private val OPEN_EDITOR: KeyMapping = KeyMapping(
        "key.elytratrims.open_cosmetics", InputConstants.KEY_K, CATEGORY
    )

    fun init() {
        KeyMappingHelper.registerKeyMapping(OPEN_EDITOR)
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (OPEN_EDITOR.consumeClick())
                if (client.player != null && client.gui.screen() == null) client.gui.setScreen(ElytraCosmeticScreen(null))
        }
    }
}
