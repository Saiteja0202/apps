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
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

/**
 * Loads rewarded ads and shows up to [maxPerDay] of them per calendar day, only
 * while a user is signed in. A small [minGapMs] between ads stops two from
 * appearing back-to-back (e.g. right after one is dismissed). The daily count
 * is persisted in SharedPreferences so it survives restarts.
 *
 * Call [preload] once the Mobile Ads SDK is initialised, and [maybeShowAd]
 * whenever there's an opportunity to display (e.g. on app foreground).
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
    private const val KEY_DAY = "rewarded_day_stamp"
    private const val KEY_COUNT = "rewarded_day_count"

    // At most 3 rewarded ads per day in release. Debug is effectively unlimited
    // so the integration is easy to verify.
    private val maxPerDay = if (BuildConfig.DEBUG) Int.MAX_VALUE else 3

    // Minimum spacing between two ads so they don't stack. Short in debug for
    // quick testing; a couple of hours in release to spread them across the day.
    private val minGapMs = if (BuildConfig.DEBUG) 30_000L else 2L * 60L * 60L * 1000L

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
     * Shows a rewarded ad when: a user is signed in, fewer than [maxPerDay] ads
     * have shown today, and at least [minGapMs] has passed since the last one.
     * Otherwise it just keeps an ad preloaded for the next opportunity.
     */
    fun maybeShowAd(activity: Activity) {
        // Only show ads to signed-in users.
        if (FirebaseAuth.getInstance().currentUser == null) return

        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val today = todayStamp()

        // Reset the counter when the calendar day changes.
        var count = if (prefs.getInt(KEY_DAY, 0) == today) prefs.getInt(KEY_COUNT, 0) else 0
        if (count >= maxPerDay) return

        if (now - prefs.getLong(KEY_LAST_SHOWN, 0L) < minGapMs) return

        val ad = rewardedAd
        if (ad == null) {
            // Nothing ready yet — load one so it's available next time.
            preload(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // Count + timestamp the moment it shows, so the daily cap and gap
                // hold even if the user dismisses before earning the reward.
                prefs.edit()
                    .putInt(KEY_DAY, today)
                    .putInt(KEY_COUNT, count + 1)
                    .putLong(KEY_LAST_SHOWN, System.currentTimeMillis())
                    .apply()
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

    /** A stable integer that changes once per calendar day (year * 1000 + day-of-year). */
    private fun todayStamp(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }
}
