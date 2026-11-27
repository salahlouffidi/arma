package android.graphics

/** Minimal android.graphics.Paint/Typeface/Color for the board's text pops (drawn with Skia on iOS). */
class Paint(flags: Int = 0) {
    enum class Style { FILL, STROKE, FILL_AND_STROKE }
    enum class Align { LEFT, CENTER, RIGHT }

    var isAntiAlias: Boolean = flags and ANTI_ALIAS_FLAG != 0
    var textAlign: Align = Align.LEFT
    var typeface: Typeface? = null
    var textSize: Float = 12f
    var style: Style = Style.FILL
    var strokeWidth: Float = 0f
    var color: Int = Color.BLACK
    var alpha: Int
        get() = (color ushr 24) and 0xFF
        set(v) { color = (color and 0x00FFFFFF) or ((v and 0xFF) shl 24) }

    fun setShadowLayer(radius: Float, dx: Float, dy: Float, shadowColor: Int) {}

    companion object { const val ANTI_ALIAS_FLAG = 1 }
}

class Typeface private constructor(val bold: Boolean) {
    companion object {
        const val NORMAL = 0
        const val BOLD = 1
        val DEFAULT = Typeface(false)
        val DEFAULT_BOLD = Typeface(true)
        fun create(family: Typeface?, style: Int): Typeface = Typeface(style and BOLD != 0 || family?.bold == true)
    }
}

object Color {
    const val BLACK = -0x1000000
    const val WHITE = -0x1
    const val TRANSPARENT = 0
    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    fun rgb(r: Int, g: Int, b: Int): Int = argb(255, r, g, b)
}
