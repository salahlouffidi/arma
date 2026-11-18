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

    private val isMainThread: Boolean
        get() = Looper.myLooper() == Looper.getMainLooper()

    fun preload() {
        loadRewarded()
        loadInterstitial()
    }

    fun isRewardedLoaded(): Boolean = rewardedAd != null
    fun isInterstitialLoaded(): Boolean = interstitialAd != null
    fun isShowingAd(): Boolean = isShowingFullScreenAd

    fun showRewarded(
        activity: Activity,
        onRewardEarned: (RewardItem) -> Unit,
        onUnavailable: () -> Unit = {},
        onClosed: () -> Unit = {}
    ) {
        if (isShowingFullScreenAd) return // double tap / overlapping request
        val ad = rewardedAd
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
        val ad = interstitialAd
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
        if (!isMainThread || rewardedAd != null || isLoadingRewarded || !safeCanRequestAds()) return
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
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoadingRewarded = false
                        rewardedAd = null
                        Log.d(TAG, "Rewarded load failed: ${adError.code}")
                    }
                }
            )
        } catch (t: Throwable) {
            isLoadingRewarded = false
            rewardedAd = null
        }
    }

    private fun loadInterstitial() {
        if (!isMainThread || interstitialAd != null || isLoadingInterstitial || !safeCanRequestAds()) return
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
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoadingInterstitial = false
                        interstitialAd = null
                        Log.d(TAG, "Interstitial load failed: ${adError.code}")
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

        /** Debug builds never request production ads. */
        val USE_TEST_ADS: Boolean = BuildConfig.DEBUG

        val rewardedUnitId: String
            get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
        val interstitialUnitId: String
            get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

        /** Minimum gap between two interstitials (natural breaks only, no ad spam). */
        const val MIN_INTERSTITIAL_INTERVAL_MS = 60_000L
    }
}
