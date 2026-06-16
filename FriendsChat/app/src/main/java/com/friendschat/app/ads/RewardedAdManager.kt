package com.friendschat.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.friendschat.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Loads a single rewarded ad and shows it at most once every 12 hours.
 *
 * Call [preload] once the Mobile Ads SDK is initialised, and [maybeShowAd]
 * whenever there's an opportunity to display (e.g. on app foreground). The
 * 12-hour gate is enforced via SharedPreferences so it survives restarts.
 */
object RewardedAdManager {
    private const val TAG = "RewardedAdManager"

    // Debug builds use Google's official test rewarded unit (always fills, safe
    // to click). Release builds use the real GenZ AdMob unit.
    private val AD_UNIT_ID =
        if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917"
        else "ca-app-pub-2109171031508747/3099239965"

    private const val PREFS = "ads_prefs"
    private const val KEY_LAST_SHOWN = "last_rewarded_shown_at"

    // Show every minute in debug so it's easy to verify; every 12 hours in release.
    private val INTERVAL_MS =
        if (BuildConfig.DEBUG) 60L * 1000L else 12L * 60L * 60L * 1000L

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    /** Fetch a rewarded ad in the background if one isn't already ready/loading. */
    fun preload(context: Context) {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows the rewarded ad if at least 12 hours have passed since the last one.
     * Otherwise it just makes sure an ad is preloaded for the next opportunity.
     */
    fun maybeShowAd(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastShown = prefs.getLong(KEY_LAST_SHOWN, 0L)
        val now = System.currentTimeMillis()

        if (now - lastShown < INTERVAL_MS) {
            return // Not yet time for the next ad.
        }

        val ad = rewardedAd
        if (ad == null) {
            // Nothing ready yet — load one so it's available next time.
            preload(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // Record the time as soon as it shows so the 12-hour gate holds
                // even if the user dismisses before earning the reward.
                prefs.edit().putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply()
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload(activity) // Get the next one ready.
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                rewardedAd = null
                preload(activity)
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
    }
}
