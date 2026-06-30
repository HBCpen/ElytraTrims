@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.elytratrims

import dev.kikugie.elytratrims.resource.image.Color4i
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.world.item.DyeColor
import java.util.Optional
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

val MINECRAFT: Minecraft get() = Minecraft.getInstance()

@Suppress("NOTHING_TO_INLINE")
inline infix fun <T> Any.then(o: T): T = o
inline fun runIf(bool: Boolean, action: () -> Unit) = bool.also { if (it) action() }

fun <T : Any?> T.asOptional() = Optional.ofNullable(this)

fun vanilla(path: String): Identifier = Identifier.withDefaultNamespace(path)
fun elytratrims(path: String): Identifier = Identifier.fromNamespaceAndPath("elytratrims", path)

fun String.text(): MutableText = Text.literal(this)
fun String.translatable(): MutableText = Text.translatable(this)
fun String.translatable(vararg args: Any): MutableText = Text.translatable(this, *args)

interface Memoizer<K, V> : (K) -> V {
    fun clear()
}

fun <K, V> memoize(provider: (K) -> V): Memoizer<K, V> = object : Memoizer<K, V> {
    private val cache: MutableMap<K, V> = mutableMapOf()

    override fun invoke(key: K): V = cache.computeIfAbsent(key, provider)
    override fun clear() = cache.clear()
}

fun <T> memoized(supplier: () -> T): MemoizedProperty<T> = MemoizedProperty(supplier)

class MemoizedProperty<T>(private val supplier: () -> T) : ReadOnlyProperty<Any?, T> {
    private var value: T? = null
    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        value ?: supplier().also { value = it }

    fun clear() {
        value = null
    }
}