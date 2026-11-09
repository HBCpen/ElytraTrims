package dev.kikugie.elytratrims.compat

import dev.kikugie.elytratrims.cosmetic.ElytraCosmetics
import dev.kikugie.elytratrims.elytratrims
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes

/**
 * Makes players with glowing wings a faint light source when LambDynamicLights is installed.
 * Only loaded through LambDynamicLights' entrypoint ([LambDynamicLightsEntrypoint]), so the mod works without it.
 */
object GlowingWingsLight : EntityLuminance {
    /** Light level of glowing wings; a torch is 14. */
    const val LUMINANCE = 7

    private val TYPE: EntityLuminance.Type by lazy { EntityLuminance.Type.registerSimple(elytratrims("glowing_wings"), this) }

    fun init(context: DynamicLightsContext) {
        TYPE
        context.entityLightSourceManager().onRegisterEvent().register { it.register(EntityTypes.PLAYER, this) }
    }

    override fun type(): EntityLuminance.Type = TYPE

    override fun getLuminance(items: ItemLightSourceManager, entity: Entity): Int =
        if (ElytraCosmetics.hasGlowingWings(entity)) LUMINANCE else 0
}
