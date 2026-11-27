package java.util

/** Exact port of java.util.Random's LCG, so seeded scenery matches Android. */
open class Random(seed: Long = kotlin.random.Random.nextLong()) {
    private var seed: Long = (seed xor MULTIPLIER) and MASK

    fun setSeed(seed: Long) { this.seed = (seed xor MULTIPLIER) and MASK }

    protected fun next(bits: Int): Int {
        seed = (seed * MULTIPLIER + ADDEND) and MASK
        return (seed ushr (48 - bits)).toInt()
    }

    fun nextInt(): Int = next(32)

    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        var r = next(31)
        val m = bound - 1
        if (bound and m == 0) return ((bound.toLong() * r.toLong()) shr 31).toInt()
        var u = r
        while (true) {
            r = u % bound
            if (u - r + m >= 0) break
            u = next(31)
        }
        return r
    }

    fun nextLong(): Long = (next(32).toLong() shl 32) + next(32)
    fun nextBoolean(): Boolean = next(1) != 0
    fun nextFloat(): Float = next(24) / (1 shl 24).toFloat()
    fun nextDouble(): Double = ((next(26).toLong() shl 27) + next(27)) * DOUBLE_UNIT

    private companion object {
        const val MULTIPLIER = 0x5DEECE66DL
        const val ADDEND = 0xBL
        const val MASK = (1L shl 48) - 1
        const val DOUBLE_UNIT = 1.0 / (1L shl 53)
    }
}
