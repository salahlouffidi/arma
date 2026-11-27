package java.util.concurrent.atomic

class AtomicInteger(initial: Int = 0) {
    private val v = kotlin.concurrent.AtomicInt(initial)
    fun get(): Int = v.value
    fun set(value: Int) { v.value = value }
    fun incrementAndGet(): Int = v.incrementAndGet()
    fun decrementAndGet(): Int = v.decrementAndGet()
    fun getAndIncrement(): Int = v.getAndIncrement()
    fun getAndDecrement(): Int = v.getAndDecrement()
    fun addAndGet(delta: Int): Int = v.addAndGet(delta)
    fun compareAndSet(expect: Int, update: Int): Boolean = v.compareAndSet(expect, update)
}

class AtomicBoolean(initial: Boolean = false) {
    private val v = kotlin.concurrent.AtomicInt(if (initial) 1 else 0)
    fun get(): Boolean = v.value != 0
    fun set(value: Boolean) { v.value = if (value) 1 else 0 }
    fun compareAndSet(expect: Boolean, update: Boolean): Boolean =
        v.compareAndSet(if (expect) 1 else 0, if (update) 1 else 0)
}
