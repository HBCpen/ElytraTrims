package dev.kikugie.elytratrims.resource.pack

import dev.kikugie.elytratrims.ResourcePack

fun List<ResourcePack>.hasElytraTrimsPack(): Boolean = any { "elytratrims" in it.packId() }
fun List<ResourcePack>.hasInjectedPack(): Boolean = any { it is ETRuntimePack }