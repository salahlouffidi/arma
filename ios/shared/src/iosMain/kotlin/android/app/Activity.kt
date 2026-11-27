package android.app

import android.content.Context
import android.content.ContextWrapper

/** The single iOS "activity": the game's root view controller. */
open class Activity : ContextWrapper(Context()) {
    val isFinishing: Boolean get() = false
    val isDestroyed: Boolean get() = false
}

object IosActivity : Activity()
