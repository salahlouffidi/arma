package com.example.game

import kotlin.time.TimeSource
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

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
    fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
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

/**
 * java.util.LinkedHashMap with access order and removeEldestEntry (LRU cache),
 * which Kotlin/Native's final LinkedHashMap does not offer.
 */
internal open class LinkedHashMap<K, V>(
    initialCapacity: Int = 16,
    loadFactor: Float = 0.75f,
    private val accessOrder: Boolean = false
) : AbstractMutableMap<K, V>() {
    private val map = kotlin.collections.LinkedHashMap<K, V>(initialCapacity)

    protected open fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = false

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = map.entries
    override val size: Int get() = map.size
    override fun containsKey(key: K): Boolean = map.containsKey(key)

    override fun get(key: K): V? {
        if (!map.containsKey(key)) return null
        @Suppress("UNCHECKED_CAST")
        val v = map[key] as V
        if (accessOrder) { map.remove(key); map[key] = v }
        return v
    }

    override fun put(key: K, value: V): V? {
        val old = map.remove(key)
        map[key] = value
        val eldest = map.entries.firstOrNull()
        if (eldest != null && removeEldestEntry(eldest)) map.remove(eldest.key)
        return old
    }

    override fun remove(key: K): V? = map.remove(key)
    override fun clear() = map.clear()
}
