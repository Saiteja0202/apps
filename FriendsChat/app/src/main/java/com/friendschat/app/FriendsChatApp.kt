package com.friendschat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.friendschat.app.ads.RewardedAdManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FriendsChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Initialise the Google Mobile Ads SDK, then preload a rewarded ad.
        MobileAds.initialize(this) { RewardedAdManager.preload(this) }
        // Presence: mark the signed-in user online/offline as the app moves
        // to the foreground/background, recording last-seen each time.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = setPresence(true)
            override fun onStop(owner: LifecycleOwner) = setPresence(false)
        })
    }

    private fun setPresence(online: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).set(
            mapOf("online" to online, "lastSeen" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "New chat messages" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
