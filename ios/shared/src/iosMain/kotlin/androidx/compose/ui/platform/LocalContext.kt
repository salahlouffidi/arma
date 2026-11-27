package androidx.compose.ui.platform

import android.app.IosActivity
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/** Same name as Android's LocalContext so the shared screens compile unchanged. */
val LocalContext = staticCompositionLocalOf<Context> { IosActivity }
