package com.example.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.OnUserEarnedRewardListener

/**
 * Centralized Google Mobile Ads integration for ARMASNAKE.
 *
 * Test ads stay enabled while the v1.2 build is being developed and tested.
 * Replace USE_TEST_ADS with false only for a release that is intentionally
 * configured to serve the real ARMASNAKE AdMob units.
 */
class AdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    fun preload() {
        loadRewarded()
        loadInterstitial()
    }

    fun isRewardedLoaded(): Boolean = rewardedAd != null
    fun isInterstitialLoaded(): Boolean = interstitialAd != null

    fun showRewarded(
        activity: Activity,
        onRewardEarned: (RewardItem) -> Unit,
        onUnavailable: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded()
            onUnavailable()
            return
        }

        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                loadRewarded()
                onUnavailable()
            }
        }

        ad.show(activity, object : OnUserEarnedRewardListener {
            override fun onUserEarnedReward(rewardItem: RewardItem) {
                onRewardEarned(rewardItem)
            }
        })
    }

    fun showInterstitial(
        activity: Activity,
        onFinished: () -> Unit = {}
    ) {
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitial()
            onFinished()
            return
        }

        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadInterstitial()
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                loadInterstitial()
                onFinished()
            }
        }
        ad.show(activity)
    }

    private fun loadRewarded() {
        try {
            RewardedAd.load(
                context,
                if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        rewardedAd = null
                    }
                }
            )
        } catch (t: Throwable) {
            rewardedAd = null
        }
    }

    private fun loadInterstitial() {
        try {
            InterstitialAd.load(
                context,
                if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        interstitialAd = null
                    }
                }
            )
        } catch (t: Throwable) {
            interstitialAd = null
        }
    }

    companion object {
        // ARMASNAKE production units created in AdMob.
        private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-3521560929268417/5578306837"
        private const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3521560929268417/6699816813"

        // Official Google test units. Safe for development and closed testing.
        private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        private const val USE_TEST_ADS = true
    }
}
