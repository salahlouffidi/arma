package java.util.concurrent

/**
 * Copy-on-write list: iteration works on a snapshot, so the game can add/remove
 * snakes, fruit and obstacles while drawing, exactly like on Android.
 */
class CopyOnWriteArrayList<E>() : AbstractMutableList<E>(), RandomAccess {
    private var items: List<E> = emptyList()

    override val size: Int get() = items.size
    override fun get(index: Int): E = items[index]
    override fun add(index: Int, element: E) { items = items.toMutableList().apply { add(index, element) } }
    override fun removeAt(index: Int): E { val l = items.toMutableList(); val e = l.removeAt(index); items = l; return e }
    override fun set(index: Int, element: E): E { val l = items.toMutableList(); val e = l.set(index, element); items = l; return e }
    override fun clear() { items = emptyList() }
    override fun addAll(elements: Collection<E>): Boolean { if (elements.isEmpty()) return false; items = items + elements; return true }
    override fun remove(element: E): Boolean {
        val i = items.indexOf(element)
        if (i < 0) return false
        items = items.toMutableList().apply { removeAt(i) }
        return true
    }
    override fun removeAll(elements: Collection<E>): Boolean {
        val n = items.size; items = items.filterNot { it in elements }; return items.size != n
    }
    override fun retainAll(elements: Collection<E>): Boolean {
        val n = items.size; items = items.filter { it in elements }; return items.size != n
    }
    override fun contains(element: E): Boolean = items.contains(element)
    override fun indexOf(element: E): Int = items.indexOf(element)
    override fun lastIndexOf(element: E): Int = items.lastIndexOf(element)
    override fun iterator(): MutableIterator<E> = Snapshot(items)
    override fun listIterator(): MutableListIterator<E> = SnapshotList(items, 0)
    override fun listIterator(index: Int): MutableListIterator<E> = SnapshotList(items, index)

    private class Snapshot<E>(list: List<E>) : MutableIterator<E> {
        private val it = list.iterator()
        override fun hasNext() = it.hasNext()
        override fun next() = it.next()
        override fun remove() = throw UnsupportedOperationException()
    }

    private class SnapshotList<E>(list: List<E>, index: Int) : MutableListIterator<E> {
        private val it = list.listIterator(index)
        override fun hasNext() = it.hasNext()
        override fun hasPrevious() = it.hasPrevious()
        override fun next() = it.next()
        override fun previous() = it.previous()
        override fun nextIndex() = it.nextIndex()
        override fun previousIndex() = it.previousIndex()
        override fun add(element: E) = throw UnsupportedOperationException()
        override fun remove() = throw UnsupportedOperationException()
        override fun set(element: E) = throw UnsupportedOperationException()
    }
}

/** Sound-effect cache. Accessed from audio coroutines; guarded by a tiny spin lock. */
class ConcurrentHashMap<K, V> : AbstractMutableMap<K, V>() {
    private val map = HashMap<K, V>()
    private val lock = kotlin.concurrent.AtomicInt(0)
    private inline fun <R> locked(block: () -> R): R {
        while (!lock.compareAndSet(0, 1)) { }
        try { return block() } finally { lock.value = 0 }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = locked { HashMap(map) }.entries
    override fun put(key: K, value: V): V? = locked { map.put(key, value) }
    override fun get(key: K): V? = locked { map[key] }
    override fun containsKey(key: K): Boolean = locked { map.containsKey(key) }
    override fun remove(key: K): V? = locked { map.remove(key) }
    override fun clear() = locked { map.clear() }
    override val size: Int get() = locked { map.size }
}

class ConcurrentLinkedQueue<E> : AbstractMutableCollection<E>() {
    private val list = CopyOnWriteArrayList<E>()
    override val size: Int get() = list.size
    override fun add(element: E): Boolean = list.add(element)
    override fun iterator(): MutableIterator<E> = list.iterator()
    override fun remove(element: E): Boolean = list.remove(element)
    override fun clear() = list.clear()
    fun poll(): E? = if (list.isEmpty()) null else list.removeAt(0)
    fun peek(): E? = list.firstOrNull()
    fun offer(e: E): Boolean = add(e)
}
