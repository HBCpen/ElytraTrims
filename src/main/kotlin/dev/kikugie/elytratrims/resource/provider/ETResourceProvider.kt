package dev.kikugie.elytratrims.resource.provider

import dev.kikugie.elytratrims.resource.pack.InputSupplier
import dev.kikugie.elytratrims.resource.pack.PackIdentifier
import net.minecraft.server.packs.resources.ResourceManager

fun interface ETResourceProvider {
    fun collect(lookup: ResourceManager, to: MutableMap<PackIdentifier, InputSupplier>)
}
