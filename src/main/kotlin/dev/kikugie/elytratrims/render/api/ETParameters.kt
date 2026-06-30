package dev.kikugie.elytratrims.render.api

import com.mojang.blaze3d.vertex.PoseStack
import dev.kikugie.elytratrims.resource.image.Color4i
import net.minecraft.client.model.Model
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

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
    @JvmField var crumblingOverlay: CrumblingOverlay? = null
}
