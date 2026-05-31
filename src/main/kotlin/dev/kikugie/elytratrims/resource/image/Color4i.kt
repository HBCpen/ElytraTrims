package dev.kikugie.elytratrims.resource.image

@Suppress("NOTHING_TO_INLINE")
@JvmInline value class Color4i private constructor(val packed: Int) {
    constructor(value: Int, alpha: Int = 0xFF) : this(value or (alpha shl 24))
    constructor(red: Int, green: Int, blue: Int, alpha: Int = 0xFF) : this(pack(red, green, blue, alpha))

    val a: Int inline get() = packed shr 24 and 0xFF
    val r: Int inline get() = packed shr 16 and 0xFF
    val g: Int inline get() = packed shr 8 and 0xFF
    val b: Int inline get() = packed and 0xFF
    val rgb: Int inline get() = packed and 0xFFFFFF

    inline operator fun component1(): Int = r
    inline operator fun component2(): Int = g
    inline operator fun component3(): Int = b
    inline operator fun component4(): Int = a

    override fun toString(): String = packed.toHexString(HexFormat.UpperCase)

    companion object {
        val TRANSPARENT = Color4i(0)
        val WHITE = Color4i(-1)

        operator fun invoke(value: UInt): Color4i = Color4i(value.toInt())
    }
}

fun Color4i.blend(other: Color4i) = when (packed) {
    0 -> this
    -1 -> other
    else -> when (other.packed) {
        0 -> other
        -1 -> this
        else -> Color4i(blend(r, other.r), blend(g, other.g), blend(b, other.b), blend(a, other.a))
    }
}

private fun pack(red: Int, green: Int, blue: Int, alpha: Int): Int {
    val red = red and 0xFF
    val green = green and 0xFF
    val blue = blue and 0xFF
    val alpha = alpha and 0xFF
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun blend(a: Int, b: Int) = when (a) {
    0 -> 0
    0xFF -> b
    else -> when (b) {
        0 -> 0
        0xFF -> a
        else -> (a * b) / 0xFF
    }
}