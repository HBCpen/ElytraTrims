package dev.kikugie.elytratrims.resource.image

private fun fromColors(red: Int, green: Int, blue: Int, alpha: Int): Int {
    val r = red and 0xFF
    val g = green and 0xFF
    val b = blue and 0xFF
    val a = alpha and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun fromColors(red: Float, green: Float, blue: Float, alpha: Float): Int =
    fromColors((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(), (alpha * 255).toInt())

private fun blend(x: Int, y: Int): Int = when(x) {
    0 -> 0
    0xFF -> y
    else -> when(y) {
        0 -> 0
        0xFF -> x
        else -> (x * y) / 0xFF
    }
}

@JvmInline
value class Color4i(val value: Int) {
    companion object {
        @JvmField val WHITE = Color4i(0xFFFFFFFF.toInt()).value
    }
    constructor(value: Long) : this(value.toInt())
    constructor(value: Int, alpha: Int) : this(value or (alpha shl 24))
    constructor(red: Int, green: Int, blue: Int, alpha: Int = 0xFF)
            : this(fromColors(red, green, blue, alpha))

    val red get() = value and 0x00FF0000 ushr 16
    val green get() = value and 0x0000FF00 ushr 8
    val blue get() = value and 0x000000FF
    val alpha get() = value and 0xFF000000.toInt() ushr 24

    fun isEmpty() = value == 0
    fun isVisible() = alpha >= 24 // Approximately 10% transparency
    fun withRed(red: Int) = Color4i(value and 0xFF00FF00.toInt() or (red shl 16))
    fun withGreen(green: Int) = Color4i(value and 0xFFFF00FF.toInt() or (green shl 8))
    fun withBlue(blue: Int) = Color4i(value and 0xFFFFFF00.toInt() or blue)
    fun withAlpha(alpha: Int) = Color4i(value and 0x00FFFFFF or (alpha shl 24))

    operator fun component1(): Int = red
    operator fun component2(): Int = green
    operator fun component3(): Int = blue
    operator fun component4(): Int = alpha

    fun blend(other: Color4i) = when(value) {
        0 -> Color4i(0)
        WHITE -> other
        else -> when(other.value) {
            0 -> Color4i(0)
            WHITE -> this
            else -> Color4i(
                blend(red, other.red),
                blend(green, other.green),
                blend(blue, other.blue),
                blend(alpha, other.alpha)
            )
        }
    }

    override fun toString(): String = value.toHexString(HexFormat.UpperCase)
}
