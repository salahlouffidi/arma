package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Centralized Google Mobile Ads integration for ARMASNAKE.
 *
 * - Ads are only requested when [canRequestAds] (UMP consent) allows it.
 * - Debug builds always use Google's official test units; release builds use the
 *   ARMASNAKE production units below.
 * - Only one full-screen ad can be on screen at a time, a reward is granted at most
 *   once per rewarded ad, and repeated taps while an ad is opening are ignored.
 * - Interstitials respect a minimum interval so they only appear at natural breaks.
 * - Every failure path calls back immediately so gameplay is never blocked.
 */
class AdManager(
    context: Context,
    private val canRequestAds: () -> Boolean = { true }
) {
    private val appContext = context.applicationContext

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingRewarded = false
    private var isLoadingInterstitial = false
    private var isShowingFullScreenAd = false
    private var lastInterstitialShownAt = 0L
    private var rewardedLoadedAt = 0L
    private var interstitialLoadedAt = 0L
    private var rewardedRetry = 0
    private var interstitialRetry = 0
    private val handler = android.os.Handler(Looper.getMainLooper())

    /** Last load/show error, for logs and the in-game status line. */
    var lastError: String? = null
        private set

    private val isMainThread: Boolean
        get() = Looper.myLooper() == Looper.getMainLooper()

    fun preload() {
        loadRewarded()
        loadInterstitial()
    }

    fun isRewardedLoaded(): Boolean = rewardedAd != null && !isStale(rewardedLoadedAt)
    fun isInterstitialLoaded(): Boolean = interstitialAd != null && !isStale(interstitialLoadedAt)

    // Google: a loaded full-screen ad should be shown within ~1 hour, then refreshed.
    private fun isStale(loadedAt: Long) = loadedAt != 0L && SystemClock.elapsedRealtime() - loadedAt > 55 * 60_000L

    /**
     * Shows the rewarded ad, waiting up to [timeoutMs] for it to finish loading if needed
     * (instead of failing instantly when the player taps right after a game over).
     */
    fun showRewardedWhenReady(
        activity: Activity,
        timeoutMs: Long = 6_000L,
        onLoading: (Boolean) -> Unit = {},
        onRewardEarned: (RewardItem) -> Unit,
        onUnavailable: () -> Unit = {},
        onClosed: () -> Unit = {}
    ) {
        if (isShowingFullScreenAd) return
        if (isRewardedLoaded()) {
            showRewarded(activity, onRewardEarned, onUnavailable, onClosed)
            return
        }
        if (isStale(rewardedLoadedAt)) { rewardedAd = null; rewardedLoadedAt = 0L }
        loadRewarded()
        onLoading(true)
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        handler.post(object : Runnable {
            override fun run() {
                when {
                    activity.isFinishing || activity.isDestroyed -> onLoading(false)
                    isRewardedLoaded() -> { onLoading(false); showRewarded(activity, onRewardEarned, onUnavailable, onClosed) }
                    SystemClock.elapsedRealtime() > deadline -> { onLoading(false); onUnavailable() }
                    else -> { if (!isLoadingRewarded) loadRewarded(); handler.postDelayed(this, 250L) }
                }
            }
        })
    }

    /** Interstitial at a natural break: waits briefly for an in-flight load, never blocks long. */
    fun showInterstitialWhenReady(activity: Activity, timeoutMs: Long = 2_500L, onFinished: () -> Unit = {}) {
        if (isShowingFullScreenAd) return
        val now = SystemClock.elapsedRealtime()
        val tooSoon = lastInterstitialShownAt != 0L && now - lastInterstitialShownAt < MIN_INTERSTITIAL_INTERVAL_MS
        if (tooSoon) { onFinished(); return }
        if (isInterstitialLoaded()) { showInterstitial(activity, onFinished); return }
        if (isStale(interstitialLoadedAt)) { interstitialAd = null; interstitialLoadedAt = 0L }
        loadInterstitial()
        if (!isLoadingInterstitial) { onFinished(); return } // consent not given / SDK unavailable
        val deadline = now + timeoutMs
        handler.post(object : Runnable {
            override fun run() {
                when {
                    activity.isFinishing || activity.isDestroyed -> onFinished()
                    isInterstitialLoaded() -> showInterstitial(activity, onFinished)
                    SystemClock.elapsedRealtime() > deadline -> onFinished()
                    else -> handler.postDelayed(this, 200L)
                }
            }
        })
    }

    /** Debug builds: opens Google's Ad Inspector to see why a request failed / did not fill. */
    fun openAdInspector(activity: Activity) {
        try {
            com.google.android.gms.ads.MobileAds.openAdInspector(activity) { error ->
                if (error != null) Log.w(TAG, "Ad inspector: ${error.message}")
            }
        } catch (t: Throwable) {}
    }

    private fun scheduleRetry(rewarded: Boolean) {
        val attempt = if (rewarded) ++rewardedRetry else ++interstitialRetry
        val delay = (15_000L shl (attempt - 1).coerceAtMost(3)).coerceAtMost(120_000L)
        handler.postDelayed({ if (rewarded) loadRewarded() else loadInterstitial() }, delay)
    }
    fun isShowingAd(): Boolean = isShowingFullScreenAd

    fun showRewarded(
        activity: Activity,
        onRewardEarned: (RewardItem) -> Unit,
        onUnavailable: () -> Unit = {},
        onClosed: () -> Unit = {}
    ) {
        if (isShowingFullScreenAd) return // double tap / overlapping request
        val ad = rewardedAd?.takeIf { isRewardedLoaded() }
        if (ad == null || activity.isFinishing) {
            loadRewarded()
            onUnavailable()
            return
        }

        rewardedAd = null
        isShowingFullScreenAd = true
        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingFullScreenAd = false
                loadRewarded()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingFullScreenAd = false
                lastError = "show ${adError.code}: ${adError.message}"
                Log.w(TAG, "Rewarded failed to show: $lastError")
                loadRewarded()
                onUnavailable()
            }
        }

        try {
            ad.show(activity) { rewardItem ->
                if (!rewardGranted) {
                    rewardGranted = true
                    onRewardEarned(rewardItem)
                }
            }
        } catch (t: Throwable) {
            isShowingFullScreenAd = false
            onUnavailable()
        }
    }

    fun showInterstitial(
        activity: Activity,
        onFinished: () -> Unit = {}
    ) {
        if (isShowingFullScreenAd) return
        val ad = interstitialAd?.takeIf { isInterstitialLoaded() }
        val now = SystemClock.elapsedRealtime()
        val tooSoon = lastInterstitialShownAt != 0L && now - lastInterstitialShownAt < MIN_INTERSTITIAL_INTERVAL_MS
        if (ad == null || tooSoon || activity.isFinishing) {
            if (ad == null) loadInterstitial()
            onFinished()
            return
        }

        interstitialAd = null
        isShowingFullScreenAd = true
        var finished = false
        val finishOnce = {
            if (!finished) {
                finished = true
                isShowingFullScreenAd = false
                onFinished()
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                loadInterstitial()
                finishOnce()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                lastError = "show ${adError.code}: ${adError.message}"
                Log.w(TAG, "Interstitial failed to show: $lastError")
                loadInterstitial()
                finishOnce()
            }
        }
        try {
            ad.show(activity)
        } catch (t: Throwable) {
            finishOnce()
        }
    }

    private fun loadRewarded() {
        if (!isMainThread || isRewardedLoaded() || isLoadingRewarded || !safeCanRequestAds()) return
        isLoadingRewarded = true
        try {
            RewardedAd.load(
                appContext,
                rewardedUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        isLoadingRewarded = false
                        rewardedAd = ad
                        rewardedLoadedAt = SystemClock.elapsedRealtime()
                        rewardedRetry = 0
                        Log.i(TAG, "Rewarded loaded (${rewardedUnitId})")
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoadingRewarded = false
                        rewardedAd = null
                        lastError = "load ${adError.code}: ${adError.message}"
                        Log.w(TAG, "Rewarded load failed: $lastError")
                        scheduleRetry(rewarded = true)
                    }
                }
            )
        } catch (t: Throwable) {
            isLoadingRewarded = false
            rewardedAd = null
        }
    }

    private fun loadInterstitial() {
        if (!isMainThread || isInterstitialLoaded() || isLoadingInterstitial || !safeCanRequestAds()) return
        isLoadingInterstitial = true
        try {
            InterstitialAd.load(
                appContext,
                interstitialUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        isLoadingInterstitial = false
                        interstitialAd = ad
                        interstitialLoadedAt = SystemClock.elapsedRealtime()
                        interstitialRetry = 0
                        Log.i(TAG, "Interstitial loaded (${interstitialUnitId})")
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoadingInterstitial = false
                        interstitialAd = null
                        lastError = "load ${adError.code}: ${adError.message}"
                        Log.w(TAG, "Interstitial load failed: $lastError")
                        scheduleRetry(rewarded = false)
                    }
                }
            )
        } catch (t: Throwable) {
            isLoadingInterstitial = false
            interstitialAd = null
        }
    }

    private fun safeCanRequestAds(): Boolean = try { canRequestAds() } catch (t: Throwable) { false }

    companion object {
        private const val TAG = "ArmaAds"

        // ARMASNAKE production units created in AdMob (do not change).
        const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-3521560929268417/5578306837"
        const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3521560929268417/6699816813"

        // Official Google test units — used automatically by debug builds.
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        /**
         * Production ARMASNAKE units in every build (also builds made by Google AI Studio,
         * which are debug-signed). Test units only when built with -PuseTestAds=true.
         * Never click your own live ads: register your phone as a test device in AdMob
         * (Settings > Test devices) to see live ads safely with a "Test" badge.
         */
        val USE_TEST_ADS: Boolean = BuildConfig.USE_TEST_ADS

        val rewardedUnitId: String
            get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
        val interstitialUnitId: String
            get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

        /** Minimum gap between two interstitials (natural breaks only, no ad spam). */
        const val MIN_INTERSTITIAL_INTERVAL_MS = 30_000L
    }
}
