package com.example

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.example.ads.AdManager
import com.example.ads.ConsentManager
import com.example.ui.ArmadilloEscapeScreen
import com.example.ui.theme.ArmadilloEscapeTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    private var isInPipMode by mutableStateOf(false)
    private var isGameplayActive by mutableStateOf(false)
    private var privacyOptionsRequired by mutableStateOf(false)
    private lateinit var adManager: AdManager
    private lateinit var consentManager: ConsentManager
    private val mobileAdsInitStarted = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Orientation preference (landscape) is declared in the manifest; on large screens
        // Android 16+ may ignore it, and the UI adapts to any size / orientation.
        enableEdgeToEdge()

        consentManager = ConsentManager(this)
        adManager = AdManager(applicationContext) { consentManager.canRequestAds }

        // UMP: refresh consent on every launch, then initialise ads only if allowed.
        consentManager.gatherConsent(this) {
            privacyOptionsRequired = consentManager.isPrivacyOptionsRequired
            if (consentManager.canRequestAds) initializeMobileAdsOnce()
        }
        // Consent obtained in a previous session: start loading in parallel.
        if (consentManager.canRequestAds) initializeMobileAdsOnce()

        setContent {
            ArmadilloEscapeTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF140D08)
                    ) {
                        ArmadilloEscapeScreen(
                            isInPip = isInPipMode,
                            onEnterPip = { enterPip() },
                            onSetGameplayActive = { updatePipParams(it) },
                            adManager = adManager,
                            privacyOptionsRequired = privacyOptionsRequired,
                            onOpenPrivacyOptions = { openPrivacyOptions() }
                        )
                    }
                }
            }
        }
    }

    private fun initializeMobileAdsOnce() {
        if (!mobileAdsInitStarted.compareAndSet(false, true)) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(applicationContext) {}
            } catch (t: Throwable) {
                // Devices without Google Play services: game continues without ads.
            }
            withContext(Dispatchers.Main) { adManager.preload() }
        }
    }

    private fun openPrivacyOptions() {
        consentManager.showPrivacyOptionsForm(this) {
            privacyOptionsRequired = consentManager.isPrivacyOptionsRequired
            if (consentManager.canRequestAds) initializeMobileAdsOnce()
        }
    }

    fun enterPip() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(true)
                }
                enterPictureInPictureMode(builder.build())
            } else {
                @Suppress("DEPRECATION")
                enterPictureInPictureMode()
            }
        } catch (e: Exception) {
            // Safely handled
        }
    }

    private fun updatePipParams(active: Boolean) {
        isGameplayActive = active
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setAutoEnterEnabled(active)
                    .build()
                setPictureInPictureParams(params)
            } catch (e: Exception) {
                // Safely handled
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isGameplayActive && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            enterPip()
        }
    }

    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}
