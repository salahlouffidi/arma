package com.example

import android.app.IosActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.ComposeUIViewController
import com.example.ads.AdManager
import com.example.ui.ArmadilloEscapeScreen
import com.example.ui.theme.ArmadilloEscapeTheme
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/** State the Swift side updates (consent, privacy options). */
object IosAppState {
    var privacyOptionsRequired by mutableStateOf(false)
    var canRequestAds by mutableStateOf(false)
    /** Set by Swift: opens the UMP privacy options form. */
    var openPrivacyOptions: () -> Unit = {}
}

/** iOS twin of MainActivity's setContent { ... }. Called from Swift. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val adManager = remember { AdManager(IosActivity) { IosAppState.canRequestAds } }
    ArmadilloEscapeTheme {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalContext provides IosActivity
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF140D08)) {
                ArmadilloEscapeScreen(
                    isInPip = false,
                    onEnterPip = {},
                    onSetGameplayActive = { active -> UIApplication.sharedApplication.idleTimerDisabled = active },
                    adManager = adManager,
                    privacyOptionsRequired = IosAppState.privacyOptionsRequired,
                    onOpenPrivacyOptions = { IosAppState.openPrivacyOptions() }
                )
            }
        }
    }
}
