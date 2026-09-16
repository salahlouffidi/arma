package com.example.game

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Independent 3-Channel Audio Engine for ARMASNAKE — LOUFFI EDITION.
 *
 * Provides:
 * 1. Independent Background Music Channel:
 *    - Plays the official attached ARMASNAKE music (arma_snake.mp3) via looped MediaPlayer.
 *    - Has PCM synthesizer fallback if resource is unavailable.
 *    - Plays continuously without stopping, restarting, or stuttering when SFX occur.
 * 2. Independent Stage Transition Music Channel:
 *    - Dedicated stageTransitionMusic player (stage_transition.mp3).
 *    - Plays whenever a stage is completed or transitioned, completely decoupled from intro and SFX.
 * 3. Independent Sound Effects (SFX) Channel:
 *    - Zero-latency multi-voice synthesized PCM sounds for all in-game gameplay events.
 * 4. Control Mode Persistence:
 *    - PHONE_MOTION vs TOUCH_JOYSTICK
 */
class SoundManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("armasnake_audio_prefs", Context.MODE_PRIVATE)

    private val audioScope = CoroutineScope(Dispatchers.Default + Job())
    private val sampleRate = 22050

    var isSoundEnabled: Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) {
            field = value
            prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
            updateMusicVolume()
            updateTransitionVolume()
        }

    var musicVolume: Float = prefs.getFloat(KEY_MUSIC_VOL, 0.75f)
        set(value) {
            field = value.coerceIn(0f, 1f)
            prefs.edit().putFloat(KEY_MUSIC_VOL, field).apply()
            updateMusicVolume()
        }

    var sfxVolume: Float = prefs.getFloat(KEY_SFX_VOL, 0.90f)
        set(value) {
            field = value.coerceIn(0f, 1f)
            prefs.edit().putFloat(KEY_SFX_VOL, field).apply()
        }

    // Music Player State (Arma-Snake.mp3)
    private var mediaPlayerMusic: MediaPlayer? = null
    private var musicTrack: AudioTrack? = null
    private var musicJob: Job? = null
    private var isMusicPlaying = false

    // Stage Transition Music Player
    private var transitionPlayer: MediaPlayer? = null

    init {
        // Pre-warm audio state
    }

    // -------------------------------------------------------------
    // CHANNEL A: LOUFFI / ARMASNAKE INTRO & BACKGROUND MUSIC
    // -------------------------------------------------------------
    fun startMusic() {
        // Background music removed per user requirement: no looping ARMASNAKE / LOUFFI music.
        // SFX and stage completion / transition audio remain active.
    }

    fun stopMusic() {
        isMusicPlaying = false
        try {
            mediaPlayerMusic?.stop()
            mediaPlayerMusic?.release()
        } catch (e: Exception) {}
        mediaPlayerMusic = null

        musicJob?.cancel()
        musicJob = null
        try {
            musicTrack?.stop()
            musicTrack?.release()
        } catch (e: Exception) {}
        musicTrack = null
    }

    private fun updateMusicVolume() {
        val effectiveVol = if (isSoundEnabled) musicVolume else 0f
        try {
            mediaPlayerMusic?.setVolume(effectiveVol, effectiveVol)
        } catch (e: Exception) {}
        try {
            musicTrack?.setVolume(effectiveVol)
        } catch (e: Exception) {}
    }

    // -------------------------------------------------------------
    // CHANNEL B: INDEPENDENT STAGE TRANSITION MUSIC
    // -------------------------------------------------------------
    fun playStageTransitionMusic() {
        if (!isSoundEnabled) return
        try {
            val resId = context.resources.getIdentifier("stage_transition", "raw", context.packageName)
            if (resId != 0) {
                transitionPlayer?.release()
                transitionPlayer = MediaPlayer.create(context, resId).apply {
                    val vol = if (isSoundEnabled) musicVolume else 0f
                    setVolume(vol, vol)
                    setOnCompletionListener {
                        try {
                            it.release()
                        } catch (e: Exception) {}
                        transitionPlayer = null
                    }
                    start()
                }
                return
            }
        } catch (e: Exception) {
            // Fall back to PCM
        }

        // Synth fallback for stage transition if file missing
        playStageCompleted()
    }

    private fun updateTransitionVolume() {
        val vol = if (isSoundEnabled) musicVolume else 0f
        try {
            transitionPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {}
    }

    // -------------------------------------------------------------
    // INDEPENDENT SOUND EFFECTS (SFX)
    // -------------------------------------------------------------

    private fun playPcmSfx(samples: ShortArray) {
        if (!isSoundEnabled || sfxVolume <= 0.01f) return
        audioScope.launch {
            try {
                val sfxTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                sfxTrack.setVolume(sfxVolume)
                sfxTrack.write(samples, 0, samples.size)
                sfxTrack.play()

                val durationMs = (samples.size.toDouble() / sampleRate * 1000).toLong() + 50
                delay(durationMs)
                sfxTrack.stop()
                sfxTrack.release()
            } catch (e: Exception) {
                // Safe
            }
        }
    }

    // 1. 🍎 fruit eaten: bright, crisp upward chime
    fun playFruitEaten() {
        val length = (sampleRate * 0.14).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        for (i in 0 until length) {
            val progress = i.toDouble() / length
            val freq = 650.0 + progress * 550.0 // 650Hz -> 1200Hz
            phase += (2.0 * PI * freq) / sampleRate
            val env = (1.0 - progress) * (1.0 - progress)
            val v = (sin(phase) + 0.3 * sin(2.0 * phase)) * env
            samples[i] = (v * 28000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 2. ⚡ special fruit activated: sparkling ascending arpeggio
    fun playSpecialFruitActivated() {
        val length = (sampleRate * 0.35).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
        val noteLen = length / notes.size
        for (i in 0 until length) {
            val noteIdx = (i / noteLen).coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]
            val subT = (i % noteLen).toDouble() / noteLen
            phase += (2.0 * PI * freq) / sampleRate
            val env = exp(-subT * 4.0)
            val sparkle = sin(phase * 3.0) * 0.2
            val v = (sin(phase) + sparkle) * env
            samples[i] = (v * 30000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 3. 🦔 armadillo transformation / power activation: powerful ascending hyper-spin whoosh
    fun playPowerActivated() {
        val length = (sampleRate * 0.45).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq = 320.0 + p * p * 980.0
            phase += (2.0 * PI * freq) / sampleRate
            val tremolo = 0.5 + 0.5 * sin(2.0 * PI * 28.0 * p)
            val env = if (p < 0.1) p / 0.1 else (1.0 - p)
            val v = sin(phase) * tremolo * env
            samples[i] = (v * 31000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 4. 🐍 snake collision: low rumble & dramatic thud
    fun playSnakeCollision() {
        val length = (sampleRate * 0.30).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq = 160.0 * (1.0 - p * 0.7)
            phase += (2.0 * PI * freq) / sampleRate
            val noise = (Math.random() * 2.0 - 1.0) * 0.4
            val env = exp(-p * 7.0)
            val v = (sin(phase) * 0.7 + noise) * env
            samples[i] = (v * 32000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 4b. 💀 game over defeat: dramatic descending minor cadence
    fun playGameOver() {
        val length = (sampleRate * 0.70).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        val notes = doubleArrayOf(392.0, 369.99, 349.23, 311.13) // G4 -> F#4 -> F4 -> Eb4
        val noteLen = length / notes.size
        for (i in 0 until length) {
            val noteIdx = (i / noteLen).coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]
            val subT = (i % noteLen).toDouble() / noteLen
            phase += (2.0 * PI * freq) / sampleRate
            val env = exp(-subT * 3.5)
            val v = sin(phase) * env
            samples[i] = (v * 28000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 5. 💥 protection activation / shield deflect: energetic metallic clang / laser deflection
    fun playProtectionHit() {
        val length = (sampleRate * 0.28).toInt()
        val samples = ShortArray(length)
        var phase1 = 0.0
        var phase2 = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq1 = 1200.0 * exp(-p * 3.0)
            val freq2 = 1840.0 * exp(-p * 3.0)
            phase1 += (2.0 * PI * freq1) / sampleRate
            phase2 += (2.0 * PI * freq2) / sampleRate
            val env = exp(-p * 9.0)
            val v = (sin(phase1) + 0.6 * sin(phase2)) * env
            samples[i] = (v * 31000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 6. 🦔 new armadillo spawned: gentle pop & re-roll swirl
    fun playNewArmadilloSpawned() {
        val length = (sampleRate * 0.22).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq = 280.0 + p * 420.0
            phase += (2.0 * PI * freq) / sampleRate
            val env = sin(p * PI)
            val v = sin(phase) * env
            samples[i] = (v * 26000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 7. ⭐ point gained: high clear coin chime
    fun playPointGained() {
        val length = (sampleRate * 0.18).toInt()
        val samples = ShortArray(length)
        var phase1 = 0.0
        var phase2 = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq1 = if (p < 0.5) 987.77 else 1318.51
            val freq2 = freq1 * 2.0
            phase1 += (2.0 * PI * freq1) / sampleRate
            phase2 += (2.0 * PI * freq2) / sampleRate
            val subP = (p % 0.5) / 0.5
            val env = exp(-subP * 6.0)
            val v = (sin(phase1) + 0.25 * sin(phase2)) * env
            samples[i] = (v * 29000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 8. 💔 point lost: descending buzzy tone
    fun playPointLost() {
        val length = (sampleRate * 0.25).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq = 360.0 - p * 190.0
            phase += (2.0 * PI * freq) / sampleRate
            val square = if (sin(phase) > 0) 0.7 else -0.7
            val env = (1.0 - p)
            val v = square * env
            samples[i] = (v * 24000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 9. 🎉 stage completed: victorious fanfare chord
    fun playStageCompleted() {
        val length = (sampleRate * 0.60).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        val chord = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val chordLen = length / chord.size
        for (i in 0 until length) {
            val chordIdx = (i / chordLen).coerceIn(0, chord.size - 1)
            val freq = chord[chordIdx]
            val subT = (i % chordLen).toDouble() / chordLen
            phase += (2.0 * PI * freq) / sampleRate
            val env = exp(-subT * 3.5)
            val v = (sin(phase) + 0.3 * sin(2.0 * phase)) * env
            samples[i] = (v * 30000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 10. 🏆 final stage completed: grand majestic multi-note champion fanfare
    fun playFinalStageCompleted() {
        val length = (sampleRate * 1.1).toInt()
        val samples = ShortArray(length)
        val melody = doubleArrayOf(523.25, 659.25, 783.99, 659.25, 1046.50, 1318.51)
        val noteLen = length / melody.size
        var phase = 0.0
        for (i in 0 until length) {
            val idx = (i / noteLen).coerceIn(0, melody.size - 1)
            val freq = melody[idx]
            val subP = (i % noteLen).toDouble() / noteLen
            phase += (2.0 * PI * freq) / sampleRate
            val env = exp(-subP * 3.0)
            val v = (sin(phase) + 0.4 * sin(2.0 * phase)) * env
            samples[i] = (v * 32000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 11. 🖱️ menu button: tactile crisp click
    fun playMenuClick() {
        val length = (sampleRate * 0.04).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq = 800.0 - p * 300.0
            phase += (2.0 * PI * freq) / sampleRate
            val env = (1.0 - p)
            val v = sin(phase) * env
            samples[i] = (v * 24000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 12. 🎮 stage selection: tech electronic swoosh / confirm
    fun playStageSelected() {
        val length = (sampleRate * 0.16).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq = 440.0 + p * 440.0
            phase += (2.0 * PI * freq) / sampleRate
            val env = sin(p * PI)
            val v = (sin(phase) + 0.25 * sin(2.0 * phase)) * env
            samples[i] = (v * 27000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 13. 🪵 wall bump: short tactile wooden pop/thud
    fun playWallBump() {
        val length = (sampleRate * 0.08).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq = 220.0 * (1.0 - p * 0.45)
            phase += (2.0 * PI * freq) / sampleRate
            val env = (1.0 - p) * (1.0 - p)
            val v = sin(phase) * env
            samples[i] = (v * 26000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    // 14. 🌋 lava hit: sizzling sizzle/crackle burst
    fun playLavaHit() {
        val length = (sampleRate * 0.25).toInt()
        val samples = ShortArray(length)
        var phase = 0.0
        for (i in 0 until length) {
            val p = i.toDouble() / length
            val freq = 480.0 * (1.0 - p * 0.5)
            phase += (2.0 * PI * freq) / sampleRate
            val noise = (Math.random() * 2.0 - 1.0) * 0.6
            val env = exp(-p * 6.0)
            val v = (sin(phase) * 0.4 + noise) * env
            samples[i] = (v * 29000).toInt().toShort()
        }
        playPcmSfx(samples)
    }

    companion object {
        private const val KEY_SOUND_ENABLED = "sound_master_enabled"
        private const val KEY_MUSIC_VOL = "music_volume"
        private const val KEY_SFX_VOL = "sfx_volume"
    }
}
