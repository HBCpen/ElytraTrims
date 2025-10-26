package dev.kikugie.elytratrims.resource.pack

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.Dynamic
import com.mojang.serialization.JsonOps
import dev.kikugie.elytratrims.ResourcePack
import net.minecraft.server.packs.resources.Resource

fun List<ResourcePack>.hasElytraTrimsPack(): Boolean = any { "elytratrims" in it.packId() }
fun List<ResourcePack>.hasInjectedPack(): Boolean = any { it is ETRuntimePack }

fun <T> List<T>.wrap() = WrappedList(this)
fun <T> List<T>.isWrapped() = this is WrappedList

fun <T> Resource.readJsonWith(codec: Codec<T>): T = openAsReader()
    .use { Dynamic(JsonOps.INSTANCE, JsonParser.parseReader(it)) }
    .let { codec.parse(it).orThrow }

class WrappedList<T>(val list: List<T>) : List<T> by list