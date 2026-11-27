package com.example.game

import kotlin.time.TimeSource

// JVM APIs used by the shared game code, provided for iOS in the same package.

internal object Math {
    const val PI: Double = kotlin.math.PI
    fun random(): Double = kotlin.random.Random.nextDouble()
    fun toRadians(deg: Double): Double = deg / 180.0 * PI
    fun toDegrees(rad: Double): Double = rad * 180.0 / PI
}

internal object System {
    private val start = TimeSource.Monotonic.markNow()
    fun nanoTime(): Long = start.elapsedNow().inWholeNanoseconds
    fun currentTimeMillis(): Long = (platform.Foundation.NSDate().timeIntervalSince1970 * 1000.0).toLong()
}

/** Same signature as android.graphics.Canvas.drawText, drawn with Skia. */
internal fun org.jetbrains.skia.Canvas.drawText(text: String, x: Float, y: Float, paint: android.graphics.Paint) {
    val bold = paint.typeface?.bold == true
    val face = org.jetbrains.skia.FontMgr.default.matchFamilyStyle(
        null,
        if (bold) org.jetbrains.skia.FontStyle.BOLD else org.jetbrains.skia.FontStyle.NORMAL
    )
    val font = org.jetbrains.skia.Font(face, paint.textSize)
    val width = font.measureTextWidth(text)
    val startX = when (paint.textAlign) {
        android.graphics.Paint.Align.CENTER -> x - width / 2f
        android.graphics.Paint.Align.RIGHT -> x - width
        android.graphics.Paint.Align.LEFT -> x
    }
    val skPaint = org.jetbrains.skia.Paint().apply {
        color = paint.color
        isAntiAlias = paint.isAntiAlias || true
        mode = if (paint.style == android.graphics.Paint.Style.STROKE) org.jetbrains.skia.PaintMode.STROKE else org.jetbrains.skia.PaintMode.FILL
        strokeWidth = paint.strokeWidth
    }
    drawString(text, startX, y, font, skPaint)
}
