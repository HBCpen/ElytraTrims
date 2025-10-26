package dev.kikugie.elytratrims.resource

import dev.kikugie.elytratrims.elytratrims
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item

object ETTags {
    @JvmField val ELYTRA_DECORATABLE: TagKey<Item> = create("decoratable")

    private fun create(name: String): TagKey<Item> =
        TagKey.create(Registries.ITEM, elytratrims(name))
}