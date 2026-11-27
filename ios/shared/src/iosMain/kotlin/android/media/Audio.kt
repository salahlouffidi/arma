package android.media

import android.content.Context
import com.example.R
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.NSObject
import platform.AVFAudio.AVAudioPlayerDelegateProtocol

class AudioAttributes private constructor() {
    class Builder {
        fun setUsage(usage: Int) = this
        fun setContentType(type: Int) = this
        fun build() = AudioAttributes()
    }
    companion object {
        const val USAGE_MEDIA = 1
        const val USAGE_GAME = 14
        const val CONTENT_TYPE_MUSIC = 2
        const val CONTENT_TYPE_SONIFICATION = 4
    }
}

class AudioFormat private constructor(internal val sampleRate: Int) {
    class Builder {
        private var rate = 22050
        fun setSampleRate(rate: Int) = also { this.rate = rate }
        fun setChannelMask(mask: Int) = this
        fun setEncoding(encoding: Int) = this
        fun build() = AudioFormat(rate)
    }
    companion object {
        const val CHANNEL_OUT_MONO = 4
        const val ENCODING_PCM_16BIT = 2
    }
}

/**
 * Static-mode AudioTrack: the PCM written to it is wrapped in a WAV header and played
 * with AVAudioPlayer, so the game's synthesized sound effects play unchanged on iOS.
 */
class AudioTrack private constructor(private val format: AudioFormat) {
    private var pcm = ShortArray(0)
    private var volume = 1f
    private var player: AVAudioPlayer? = null

    class Builder {
        private var format = AudioFormat.Builder().build()
        fun setAudioAttributes(attributes: AudioAttributes) = this
        fun setAudioFormat(format: AudioFormat) = also { this.format = format }
        fun setBufferSizeInBytes(size: Int) = this
        fun setTransferMode(mode: Int) = this
        fun build() = AudioTrack(format)
    }

    fun write(data: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
        pcm = data.copyOfRange(offsetInShorts, offsetInShorts + sizeInShorts)
        return sizeInShorts
    }

    fun setVolume(gain: Float): Int {
        volume = gain.coerceIn(0f, 1f)
        player?.volume = volume
        return 0
    }

    fun play() {
        val p = AVAudioPlayer(data = wav(pcm, format.sampleRate), error = null)
        p.volume = volume
        p.prepareToPlay()
        p.play()
        player = p
    }

    fun stop() { player?.stop() }
    fun release() { player?.stop(); player = null }

    companion object {
        const val MODE_STATIC = 0
        const val MODE_STREAM = 1

        internal fun wav(samples: ShortArray, rate: Int): NSData {
            val dataLen = samples.size * 2
            val b = ByteArray(44 + dataLen)
            fun str(at: Int, s: String) = s.forEachIndexed { i, c -> b[at + i] = c.code.toByte() }
            fun int32(at: Int, v: Int) { for (i in 0..3) b[at + i] = (v shr (8 * i)).toByte() }
            fun int16(at: Int, v: Int) { b[at] = v.toByte(); b[at + 1] = (v shr 8).toByte() }
            str(0, "RIFF"); int32(4, 36 + dataLen); str(8, "WAVE")
            str(12, "fmt "); int32(16, 16); int16(20, 1); int16(22, 1)
            int32(24, rate); int32(28, rate * 2); int16(32, 2); int16(34, 16)
            str(36, "data"); int32(40, dataLen)
            samples.forEachIndexed { i, s -> int16(44 + i * 2, s.toInt()) }
            return b.usePinned { NSData.create(bytes = it.addressOf(0), length = b.size.toULong()) }
        }
    }
}

/** MediaPlayer over AVAudioPlayer for the bundled mp3 files. */
class MediaPlayer private constructor(private val player: AVAudioPlayer) {
    private var onComplete: ((MediaPlayer) -> Unit)? = null
    private val delegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            onComplete?.invoke(this@MediaPlayer)
        }
    }

    init { player.delegate = delegate }

    var isLooping: Boolean
        get() = player.numberOfLoops < 0
        set(value) { player.numberOfLoops = if (value) -1L else 0L }

    val isPlaying: Boolean get() = player.playing

    fun setVolume(left: Float, right: Float) { player.volume = maxOf(left, right).coerceIn(0f, 1f) }
    fun setOnCompletionListener(listener: (MediaPlayer) -> Unit) { onComplete = listener }
    fun start() { player.play() }
    fun pause() { player.pause() }
    fun stop() { player.stop() }
    fun seekTo(ms: Int) { player.currentTime = ms / 1000.0 }
    fun release() { player.stop(); player.delegate = null; live.remove(this) }

    companion object {
        // AVAudioPlayer is not retained by the system while it plays.
        private val live = mutableSetOf<MediaPlayer>()

        fun create(context: Context, resId: Int): MediaPlayer {
            val (name, ext) = R.rawFile(resId) ?: error("Unknown raw resource $resId")
            val url = NSBundle.mainBundle.URLForResource(name, ext) ?: error("$name.$ext not bundled")
            val p = AVAudioPlayer(contentsOfURL = url, error = null)
            p.prepareToPlay()
            return MediaPlayer(p).also { live += it }
        }
    }
}
