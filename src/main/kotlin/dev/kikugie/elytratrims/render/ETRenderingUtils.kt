package dev.kikugie.elytratrims.render

import dev.kikugie.elytratrims.MINECRAFT
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.has
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.resources.model.EquipmentClientInfo
import net.minecraft.world.item.ItemStack
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.Identifier

val TextureAtlasSprite?.isMissing
    get() = this == null || contents().name() == MissingTextureAtlasSprite.getLocation()

val Identifier?.isMissing
    get() = this == MissingTextureAtlasSprite.getLocation()

fun Identifier.getSprite(identifier: Identifier, reporter: (Identifier) -> Unit): TextureAtlasSprite =
    MINECRAFT.atlasManager.getAtlasOrThrow(this).getSprite(identifier).also { if (it.isMissing) reporter(identifier) }

val ETRenderParameters.glowingLightCoords: Int
    get() = if (stack has ETItemFlag.GLOW) 0xFF00FF else lightCoords

/** Block light the elytra itself gets at least when it glows, so it stays faintly lit; its decorations are fully lit. */
const val GLOWING_WINGS_MIN_LIGHT: Int = 10

/** [lightCoords] for drawing [stack] in the [layerType] layer, raised to [GLOWING_WINGS_MIN_LIGHT] for glowing wings. */
fun glowingWingsLight(layerType: EquipmentClientInfo.LayerType, stack: ItemStack, lightCoords: Int): Int {
    if (layerType != EquipmentClientInfo.LayerType.WINGS || !(stack has ETItemFlag.GLOW)) return lightCoords
    val block = (lightCoords and 0xFFFF) shr 4
    return if (block >= GLOWING_WINGS_MIN_LIGHT) lightCoords else (lightCoords and 0xFFFF.inv()) or (GLOWING_WINGS_MIN_LIGHT shl 4)
}
