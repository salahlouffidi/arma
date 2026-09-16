package com.example

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdMobIntegrationTest {

    @Test
    fun testAdManagerInitializationAndNonBlockingFallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val adManager = AdManager(context)

        // Initially no ads are loaded
        assertFalse(adManager.isInterstitialLoaded())
        assertFalse(adManager.isRewardedLoaded())

        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        // When showInterstitial is called without a loaded ad, onFinished must execute promptly without blocking
        var finishedCalled = false
        adManager.showInterstitial(activity, onFinished = {
            finishedCalled = true
        })
        assertTrue("onFinished callback must be invoked when interstitial is unavailable", finishedCalled)

        // When showRewarded is called without a loaded ad, onUnavailable must execute promptly without blocking
        var unavailableCalled = false
        var rewardEarned = false
        adManager.showRewarded(
            activity = activity,
            onRewardEarned = { rewardEarned = true },
            onUnavailable = { unavailableCalled = true }
        )
        assertTrue("onUnavailable callback must be invoked when rewarded ad is unavailable", unavailableCalled)
        assertFalse("Reward must not be earned when ad was unavailable", rewardEarned)
    }
}
