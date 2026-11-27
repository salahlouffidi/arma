package android.os

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/** Android vibration effects, played on the iPhone Taptic Engine. */
class VibrationEffect private constructor(internal val pulses: List<Pair<Long, Int>>) {
    // pulses: (start offset ms, amplitude 1..255)
    companion object {
        const val DEFAULT_AMPLITUDE = -1
        const val EFFECT_CLICK = 0
        const val EFFECT_DOUBLE_CLICK = 1
        const val EFFECT_TICK = 2
        const val EFFECT_HEAVY_CLICK = 5

        fun createPredefined(effectId: Int): VibrationEffect = when (effectId) {
            EFFECT_TICK -> VibrationEffect(listOf(0L to 70))
            EFFECT_HEAVY_CLICK -> VibrationEffect(listOf(0L to 255))
            EFFECT_DOUBLE_CLICK -> VibrationEffect(listOf(0L to 160, 120L to 160))
            else -> VibrationEffect(listOf(0L to 150))
        }

        fun createOneShot(milliseconds: Long, amplitude: Int): VibrationEffect =
            VibrationEffect(listOf(0L to if (amplitude <= 0) (60 + milliseconds * 2).toInt().coerceIn(60, 255) else amplitude))

        fun createWaveform(timings: LongArray, amplitudes: IntArray, repeat: Int): VibrationEffect {
            val out = mutableListOf<Pair<Long, Int>>()
            var at = 0L
            timings.forEachIndexed { i, d ->
                val amp = amplitudes.getOrElse(i) { 0 }
                if (amp > 0 && d > 0) out += at to amp
                at += d
            }
            return VibrationEffect(out)
        }

        // Android pattern: [wait, buzz, wait, buzz, ...]
        fun createWaveform(timings: LongArray, repeat: Int): VibrationEffect =
            createWaveform(timings, IntArray(timings.size) { if (it % 2 == 1) 180 else 0 }, repeat)
    }
}

class CombinedVibration private constructor(internal val effect: VibrationEffect) {
    companion object {
        fun createParallel(effect: VibrationEffect) = CombinedVibration(effect)
    }
}

open class Vibrator {
    fun hasVibrator(): Boolean = true
    fun hasAmplitudeControl(): Boolean = true
    fun cancel() {}

    fun vibrate(effect: VibrationEffect) = Taptic.play(effect.pulses)
    fun vibrate(milliseconds: Long) = vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
    fun vibrate(pattern: LongArray, repeat: Int) = vibrate(VibrationEffect.createWaveform(pattern, repeat))

    companion object { val shared = Vibrator() }
}

open class VibratorManager {
    val defaultVibrator: Vibrator get() = Vibrator.shared
    fun vibrate(vibration: CombinedVibration) = Vibrator.shared.vibrate(vibration.effect)

    companion object { val shared = VibratorManager() }
}

private object Taptic {
    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val medium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavy = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)

    fun play(pulses: List<Pair<Long, Int>>) {
        pulses.take(16).forEach { (at, amp) ->
            if (at <= 0L) onMain { impact(amp) }
            else dispatch_after(dispatch_time(DISPATCH_TIME_NOW, at * 1_000_000L), dispatch_get_main_queue()) { impact(amp) }
        }
    }

    private fun onMain(block: () -> Unit) = dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0), dispatch_get_main_queue(), block)

    private fun impact(amp: Int) {
        val gen = when {
            amp < 100 -> light
            amp < 200 -> medium
            else -> heavy
        }
        gen.impactOccurredWithIntensity((amp.coerceIn(1, 255) / 255.0).coerceAtLeast(0.35))
    }
}
