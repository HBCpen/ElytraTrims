package dev.kikugie.elytratrims.cosmetic

import dev.kikugie.elytratrims.MINECRAFT
import dev.kikugie.elytratrims.cosmetic.sync.CosmeticSync
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.client.resources.model.EquipmentClientInfo
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.UUID

/**
 * Glue between entities, their render states and the elytra renderer.
 */
object ElytraCosmetics {
    /** Cosmetic attached to a player's render state during extraction; `null` renders the real item. */
    @JvmField val RENDER_KEY: RenderStateDataKey<ElytraCosmetic> = RenderStateDataKey.create { "elytratrims:cosmetic" }

    /** Set when an elytra should be drawn although none is worn, see [ElytraCosmetic.Visibility]. */
    @JvmField val VIRTUAL_WINGS_KEY: RenderStateDataKey<Boolean> = RenderStateDataKey.create { "elytratrims:virtual_wings" }

    private val VIRTUAL_ELYTRA: ItemStack by lazy { ItemStack(Items.ELYTRA) }

    /** Attaches the entity's cosmetic to its render state; called after the state is extracted. */
    @JvmStatic
    fun attach(entity: Entity, state: HumanoidRenderState) {
        val cosmetic = forEntity(entity)
        state.setData(RENDER_KEY, cosmetic)
        val wearsWings = state.chestEquipment.has(DataComponents.GLIDER)
        state.setData(VIRTUAL_WINGS_KEY, !wearsWings && cosmetic != null && cosmetic.visibility.showsWithout(state.chestEquipment.isEmpty))
    }

    /**
     * The stack the wings layer draws: the worn one, or a plain elytra when the state asks for virtual wings.
     * The cosmetic is applied to it later, when the layer is rendered.
     */
    @JvmStatic
    fun wingsStack(state: EntityRenderState, chest: ItemStack): ItemStack =
        if (state.getData(VIRTUAL_WINGS_KEY) == true && !chest.has(DataComponents.GLIDER)) VIRTUAL_ELYTRA else chest

    /** Whether virtual wings are drawn, which hides the cape like a worn elytra does. */
    @JvmStatic
    fun hasVirtualWings(state: EntityRenderState): Boolean = state.getData(VIRTUAL_WINGS_KEY) == true

    /** Cosmetic to show for the given entity, or `null` to render its real elytra. */
    @JvmStatic
    fun forEntity(entity: Entity): ElytraCosmetic? = when {
        isLocalPlayer(entity.uuid) -> CosmeticSettings.active
        else -> CosmeticSync.cosmeticFor(entity.uuid)
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
