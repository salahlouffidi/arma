package com.example.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticHelper(context: Context) {
    var isHapticsEnabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun vibrateShort() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25)
        }
    }

    fun vibrateHit() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(70)
        }
    }

    fun vibrateOverdrive() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 40, 40, 60)
            val amps = intArrayOf(0, 200, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(120)
        }
    }
}
