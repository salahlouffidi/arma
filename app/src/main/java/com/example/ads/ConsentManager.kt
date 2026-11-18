package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google User Messaging Platform (UMP) wrapper for ARMASNAKE.
 *
 * Flow (per the official AdMob "privacy strategies" guide):
 *  1. requestConsentInfoUpdate() on every app launch.
 *  2. loadAndShowConsentFormIfRequired() — shows the privacy message published
 *     in AdMob > Privacy & messaging for this app, only when required.
 *  3. Ads are initialised / requested only once canRequestAds() is true.
 *  4. A "Privacy options" entry point is exposed when the user's region requires it.
 *
 * Every failure path calls back so the game never blocks on consent or ads.
 */
class ConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    val canRequestAds: Boolean
        get() = try { consentInformation.canRequestAds() } catch (t: Throwable) { false }

    val isPrivacyOptionsRequired: Boolean
        get() = try {
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (t: Throwable) {
            false
        }

    /**
     * @param onConsentResolved invoked on the main thread once the consent flow has finished
     *        (successfully or not). Check [canRequestAds] before requesting ads.
     */
    fun gatherConsent(activity: Activity, onConsentResolved: () -> Unit) {
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG && TEST_DEVICE_HASHED_ID.isNotEmpty()) {
                // Debug-only: force the EEA flow on a registered test device.
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .addTestDeviceHashedId(TEST_DEVICE_HASHED_ID)
                        .build()
                )
            }
        }.build()

        try {
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w(TAG, "Consent form error ${formError.errorCode}: ${formError.message}")
                        }
                        onConsentResolved()
                    }
                },
                { requestError ->
                    Log.w(TAG, "Consent info update failed ${requestError.errorCode}: ${requestError.message}")
                    onConsentResolved()
                }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "UMP unavailable", t)
            onConsentResolved()
        }
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) {
                    Log.w(TAG, "Privacy options form error ${formError.errorCode}: ${formError.message}")
                }
                onDismissed()
            }
        } catch (t: Throwable) {
            onDismissed()
        }
    }

    companion object {
        private const val TAG = "ArmaConsent"

        /**
         * Optional: paste the hashed device id printed by UMP in Logcat
         * ("Use new ConsentDebugSettings.Builder().addTestDeviceHashedId(...)") to
         * test the EEA consent form on a physical device in debug builds.
         */
        const val TEST_DEVICE_HASHED_ID = ""
    }
}
