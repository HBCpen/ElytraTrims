package dev.kikugie.elytratrims.resource.provider

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.Strictness
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.JsonOps
import dev.kikugie.elytratrims.resource.image.EditableImage
import dev.kikugie.elytratrims.resource.pack.InputSupplier
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import java.io.InputStream

private val GSON: Gson = GsonBuilder().setStrictness(Strictness.LENIENT).create()

@JvmInline
value class StringSupplier(val string: String) : InputSupplier {
    override fun get(): InputStream = string.byteInputStream(Charsets.UTF_8)
}

@JvmInline
value class BytesSupplier(val bytes: ByteArray) : InputSupplier {
    override fun get(): InputStream = bytes.inputStream()
}

internal inline fun <T> jsonSupplier(codec: Codec<T>, crossinline provider: () -> T): InputSupplier =
    lazySupplier { encode(codec, provider()) }

internal fun lazySupplier(provider: () -> String): InputSupplier = object : InputSupplier {
    private val value by lazy(provider)
    override fun get(): InputStream = value.byteInputStream(Charsets.UTF_8)
}

internal fun JsonElement.asString(): String = GSON.toJson(this)

private fun <T> encode(codec: Codec<T>, value: T): String = when (val it = codec.encodeStart(JsonOps.INSTANCE, value)) {
    is DataResult.Success<*> -> it.value.toString()
    is DataResult.Error<*> -> throw IllegalStateException("Couldn't serialize $value:\n${it.messageSupplier()}")
}
