package com.example

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ads.AdManager
import com.example.ui.ArmadilloEscapeScreen
import com.example.ui.theme.ArmadilloEscapeTheme
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    private var isInPipMode by mutableStateOf(false)
    private var isGameplayActive by mutableStateOf(false)
    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Strictly lock the visual game screen to fixed landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()

        adManager = AdManager(applicationContext)
        MobileAds.initialize(this) {
            adManager.preload()
        }

        setContent {
            ArmadilloEscapeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF140D08)
                ) {
                    ArmadilloEscapeScreen(
                        isInPip = isInPipMode,
                        onEnterPip = { enterPip() },
                        onSetGameplayActive = { updatePipParams(it) },
                        adManager = adManager
                    )
                }
            }
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Ensure orientation remains locked to landscape across system configuration changes
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}
