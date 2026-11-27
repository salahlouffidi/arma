package com.example.ads

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/** Implemented in Swift (Google Mobile Ads iOS SDK) and registered with [IosAds.bridge]. */
interface AdBridge {
    fun isRewardedReady(): Boolean
    fun isInterstitialReady(): Boolean
    fun isLoading(rewarded: Boolean): Boolean
    fun preload()
    /** Always calls [onDone] exactly once, immediately if no ad can be shown. */
    fun showInterstitial(onDone: () -> Unit)
    /** Calls [onReward] if the user earned the reward, then [onClosed]; or [onUnavailable]. */
    fun showRewarded(onReward: () -> Unit, onUnavailable: () -> Unit, onClosed: () -> Unit)
}

object IosAds {
    var bridge: AdBridge? = null
}

class RewardItem(val amount: Int, val type: String)

/**
 * iOS version of the Android AdManager, with the same public API so the shared
 * game screens call it unchanged. Ads are shown by the Swift [AdBridge].
 */
class AdManager(
    context: Context,
    private val canRequestAds: () -> Boolean = { true }
) {
    private val scope = MainScope()
    private val clock = TimeSource.Monotonic.markNow()
    private fun now() = clock.elapsedNow().inWholeMilliseconds
    private var showing = false
    private var lastInterstitialShownAt = 0L

    var lastError: String? = null
        private set

    private val bridge: AdBridge? get() = IosAds.bridge?.takeIf { safeCanRequestAds() }

    fun preload() { bridge?.preload() }
    fun isRewardedLoaded(): Boolean = bridge?.isRewardedReady() == true
    fun isInterstitialLoaded(): Boolean = bridge?.isInterstitialReady() == true
    fun isShowingAd(): Boolean = showing
    fun openAdInspector(activity: Activity) {}

    fun showRewardedWhenReady(
        activity: Activity,
        timeoutMs: Long = 6_000L,
        onLoading: (Boolean) -> Unit = {},
        onRewardEarned: (RewardItem) -> Unit,
        onUnavailable: () -> Unit = {},
        onClosed: () -> Unit = {}
    ) {
        if (showing) return
        if (isRewardedLoaded()) { showRewarded(activity, onRewardEarned, onUnavailable, onClosed); return }
        val b = bridge
        if (b == null) { onUnavailable(); return }
        b.preload()
        onLoading(true)
        val deadline = now() + timeoutMs
        scope.launch {
            while (true) {
                when {
                    isRewardedLoaded() -> { onLoading(false); showRewarded(activity, onRewardEarned, onUnavailable, onClosed); return@launch }
                    now() > deadline -> { onLoading(false); lastError = "No video available"; onUnavailable(); return@launch }
                }
                delay(250)
            }
        }
    }

    /**
     * Interstitial at a natural break (next round, retry). Never waits: if no ad is
     * ready right now the game continues immediately and an ad is loaded for next time,
     * so the finished round never keeps running behind a pending ad request.
     */
    fun showInterstitialWhenReady(activity: Activity, timeoutMs: Long = 2_500L, onFinished: () -> Unit = {}) {
        if (showing) return
        val t = now()
        if (lastInterstitialShownAt != 0L && t - lastInterstitialShownAt < MIN_INTERSTITIAL_INTERVAL_MS) { onFinished(); return }
        if (isInterstitialLoaded()) { showInterstitial(activity, onFinished); return }
        bridge?.preload()
        onFinished()
    }

    fun showRewarded(
        activity: Activity,
        onRewardEarned: (RewardItem) -> Unit,
        onUnavailable: () -> Unit = {},
        onClosed: () -> Unit = {}
    ) {
        val b = bridge
        if (showing || b == null || !b.isRewardedReady()) { onUnavailable(); return }
        showing = true
        var granted = false
        b.showRewarded(
            onReward = { if (!granted) { granted = true; onRewardEarned(RewardItem(1, "Reward")) } },
            onUnavailable = { showing = false; onUnavailable() },
            onClosed = { showing = false; onClosed() }
        )
    }

    fun showInterstitial(activity: Activity, onFinished: () -> Unit = {}) {
        val b = bridge
        if (showing || b == null || !b.isInterstitialReady()) { onFinished(); return }
        showing = true
        lastInterstitialShownAt = now()
        var done = false
        b.showInterstitial { if (!done) { done = true; showing = false; onFinished() } }
    }

    private fun safeCanRequestAds(): Boolean = try { canRequestAds() } catch (t: Throwable) { false }

    companion object {
        const val MIN_INTERSTITIAL_INTERVAL_MS = 30_000L
    }
}
