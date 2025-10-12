package dev.kikugie.elytratrims.resource.pack

import dev.kikugie.elytratrims.ResourcePack

fun List<ResourcePack>.hasElytraTrimsPack(): Boolean = any { "elytratrims" in it.packId() }
fun List<ResourcePack>.hasInjectedPack(): Boolean = any { it is ETRuntimePack }

fun <T> List<T>.wrap() = WrappedList(this)
fun <T> List<T>.isWrapped() = this is WrappedList

class WrappedList<T>(val list: List<T>) : List<T> by list