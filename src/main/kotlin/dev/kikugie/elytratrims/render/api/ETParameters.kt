package dev.kikugie.elytratrims.render.api

import com.mojang.blaze3d.vertex.PoseStack
import dev.kikugie.elytratrims.resource.image.Color4i
import net.minecraft.client.model.Model
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

//? if >=26.3
private typealias TextureHandle = net.minecraft.client.resources.palette.PalettedTextureManager.Handle

@OptIn(ExperimentalStdlibApi::class)
class ETParameters {
    lateinit var model: Model<*>
    lateinit var state: Any
    lateinit var stack: ItemStack
    lateinit var matrices: PoseStack
    lateinit var texture: Identifier
    @get:JvmExposeBoxed
    @set:JvmExposeBoxed
    var color: Color4i = Color4i.WHITE
    @JvmField var lightCoords: Int = 0
    @JvmField var outlineColour: Int = 0

    //? if >=26.3
    @JvmField var trimTextureHandle: TextureHandle? = null
}
