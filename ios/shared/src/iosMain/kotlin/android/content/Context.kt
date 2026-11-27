package android.content

import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

/** iOS stand-in for the small part of android.content.Context the game uses. */
open class Context {
    open val applicationContext: Context get() = this
    val packageName: String get() = NSBundle.mainBundle.bundleIdentifier ?: "com.aistudio.armadilloescape.xwqrpz"

    fun getSharedPreferences(name: String, mode: Int): SharedPreferences = IosSharedPreferences(name)

    open fun getSystemService(name: String): Any? = when (name) {
        VIBRATOR_SERVICE -> android.os.Vibrator.default
        VIBRATOR_MANAGER_SERVICE -> android.os.VibratorManager.default
        else -> null
    }

    companion object {
        const val MODE_PRIVATE = 0
        const val VIBRATOR_SERVICE = "vibrator"
        const val VIBRATOR_MANAGER_SERVICE = "vibrator_manager"
    }
}

open class ContextWrapper(val baseContext: Context) : Context()

interface SharedPreferences {
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun getInt(key: String, defValue: Int): Int
    fun getLong(key: String, defValue: Long): Long
    fun getFloat(key: String, defValue: Float): Float
    fun getString(key: String, defValue: String?): String?
    fun contains(key: String): Boolean
    fun edit(): Editor

    interface Editor {
        fun putBoolean(key: String, value: Boolean): Editor
        fun putInt(key: String, value: Int): Editor
        fun putLong(key: String, value: Long): Editor
        fun putFloat(key: String, value: Float): Editor
        fun putString(key: String, value: String?): Editor
        fun remove(key: String): Editor
        fun clear(): Editor
        fun apply()
        fun commit(): Boolean
    }
}

/** NSUserDefaults-backed preferences, one key namespace per preferences file name. */
private class IosSharedPreferences(private val name: String) : SharedPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults
    private fun k(key: String) = "$name.$key"

    override fun getBoolean(key: String, defValue: Boolean) =
        if (contains(key)) defaults.boolForKey(k(key)) else defValue
    override fun getInt(key: String, defValue: Int) =
        if (contains(key)) defaults.integerForKey(k(key)).toInt() else defValue
    override fun getLong(key: String, defValue: Long) =
        if (contains(key)) defaults.integerForKey(k(key)) else defValue
    override fun getFloat(key: String, defValue: Float) =
        if (contains(key)) defaults.floatForKey(k(key)) else defValue
    override fun getString(key: String, defValue: String?) =
        defaults.stringForKey(k(key)) ?: defValue
    override fun contains(key: String) = defaults.objectForKey(k(key)) != null

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val ops = mutableListOf<() -> Unit>()
        private var clearFirst = false
        override fun putBoolean(key: String, value: Boolean) = also { ops += { defaults.setBool(value, k(key)) } }
        override fun putInt(key: String, value: Int) = also { ops += { defaults.setInteger(value.toLong(), k(key)) } }
        override fun putLong(key: String, value: Long) = also { ops += { defaults.setInteger(value, k(key)) } }
        override fun putFloat(key: String, value: Float) = also { ops += { defaults.setFloat(value, k(key)) } }
        override fun putString(key: String, value: String?) = also {
            ops += { if (value == null) defaults.removeObjectForKey(k(key)) else defaults.setObject(value, k(key)) }
        }
        override fun remove(key: String) = also { ops += { defaults.removeObjectForKey(k(key)) } }
        override fun clear() = also { clearFirst = true }
        override fun apply() { commit() }
        override fun commit(): Boolean {
            if (clearFirst) {
                defaults.dictionaryRepresentation().keys
                    .mapNotNull { it as? String }
                    .filter { it.startsWith("$name.") }
                    .forEach { defaults.removeObjectForKey(it) }
            }
            ops.forEach { it() }
            return true
        }
    }
}
