package dev.kikugie.elytratrims.cosmetic

import dev.kikugie.elytratrims.MINECRAFT
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.resources.model.EquipmentClientInfo
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import java.util.UUID

/**
 * Glue between entities, their render states and the elytra renderer.
 */
object ElytraCosmetics {
    /** Cosmetic attached to a player's render state during extraction; `null` renders the real item. */
    @JvmField val RENDER_KEY: RenderStateDataKey<ElytraCosmetic> = RenderStateDataKey.create { "elytratrims:cosmetic" }

    /** Cosmetic to show for the given entity, or `null` to render its real elytra. */
    @JvmStatic
    fun forEntity(entity: Entity): ElytraCosmetic? = when {
        isLocalPlayer(entity.uuid) -> CosmeticSettings.active
        // Hook for synchronizing other players' cosmetics later on.
        else -> null
    }

    /** Replaces the rendered wings stack if the render state carries a cosmetic. */
    @JvmStatic
    fun applyToWings(layerType: EquipmentClientInfo.LayerType, state: Any?, stack: ItemStack): ItemStack {
        if (layerType != EquipmentClientInfo.LayerType.WINGS || state !is EntityRenderState || stack.isEmpty) return stack
        val cosmetic = state.getData(RENDER_KEY) ?: return stack
        return CosmeticStacks.apply(cosmetic, stack, MINECRAFT.level?.registryAccess())
    }

    /**
     * Matches both the in-game player UUID and the account UUID,
     * since they differ on offline-mode servers and in replays.
     */
    private fun isLocalPlayer(uuid: UUID): Boolean =
        uuid == MINECRAFT.player?.uuid || uuid == MINECRAFT.user.profileId
}
