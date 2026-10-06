package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundManager {
    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true

    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playCollectFruit() {
        if (!isSoundEnabled) return
        scope.launch {
            playToneSweep(startFreq = 587f, endFreq = 880f, durationMs = 90)
        }
    }

    fun playCollectGem() {
        if (!isSoundEnabled) return
        scope.launch {
            playToneSweep(startFreq = 784f, endFreq = 1318f, durationMs = 120)
        }
    }

    fun playHitHazard() {
        if (!isSoundEnabled) return
        scope.launch {
            playNoiseCrunch(durationMs = 150)
        }
    }

    fun playOverdriveStart() {
        if (!isSoundEnabled) return
        scope.launch {
            playToneSweep(startFreq = 440f, endFreq = 1174f, durationMs = 250)
        }
    }

    fun playPowerup() {
        if (!isSoundEnabled) return
        scope.launch {
            playArpeggio(listOf(523f, 659f, 784f, 1046f), noteDurationMs = 50)
        }
    }

    fun playGameOver() {
        if (!isSoundEnabled) return
        scope.launch {
            playToneSweep(startFreq = 440f, endFreq = 180f, durationMs = 300)
        }
    }

    fun playStageClear() {
        if (!isSoundEnabled) return
        scope.launch {
            playArpeggio(listOf(440f, 554f, 659f, 880f), noteDurationMs = 70)
        }
    }

    private fun playToneSweep(startFreq: Float, endFreq: Float, durationMs: Int) {
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                val envelope = 1.0 - progress
                val sample = (sin(2.0 * Math.PI * currentFreq * t) * envelope * Short.MAX_VALUE * 0.7).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        } catch (_: Exception) {}
    }

    private fun playArpeggio(notes: List<Float>, noteDurationMs: Int) {
        try {
            val totalDurationMs = notes.size * noteDurationMs
            val numSamples = (sampleRate * (totalDurationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val noteSamples = (sampleRate * (noteDurationMs / 1000.0)).toInt()

            for ((noteIdx, freq) in notes.withIndex()) {
                val offset = noteIdx * noteSamples
                for (i in 0 until noteSamples) {
                    if (offset + i >= numSamples) break
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / noteSamples
                    val envelope = 1.0 - progress * 0.5
                    val sample = (sin(2.0 * Math.PI * freq * t) * envelope * Short.MAX_VALUE * 0.6).toInt()
                    buffer[offset + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            playPcm(buffer)
        } catch (_: Exception) {}
    }

    private fun playNoiseCrunch(durationMs: Int) {
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            var last = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val envelope = (1.0 - progress) * (1.0 - progress)
                val whiteNoise = (Math.random() * 2.0 - 1.0)
                last = last * 0.7 + whiteNoise * 0.3
                val sample = (last * envelope * Short.MAX_VALUE * 0.8).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        } catch (_: Exception) {}
    }

    private fun playPcm(buffer: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        track.setNotificationMarkerPosition(buffer.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack?) {
                track.release()
            }
            override fun onPeriodicNotification(t: AudioTrack?) {}
        })
    }
}
