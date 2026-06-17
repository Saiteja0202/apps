package com.friendschat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.friendschat.app.ads.RewardedAdManager
import com.friendschat.app.messaging.MessageNotifier
import com.friendschat.app.messaging.MessageSyncWorker
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FriendsChatApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Initialise the Google Mobile Ads SDK, then preload a rewarded ad.
        MobileAds.initialize(this) { RewardedAdManager.preload(this) }
        // Presence: mark the signed-in user online/offline as the app moves
        // to the foreground/background, recording last-seen each time.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                setPresence(true)
                startHeartbeat()
            }
            override fun onStop(owner: LifecycleOwner) {
                stopHeartbeat()
                setPresence(false)
            }
        })
        // App-level message notifications: watch chats whenever someone is signed
        // in (and stop on sign-out). Runs as long as the process is alive.
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                MessageNotifier.start(this)          // live notifications while running
                MessageSyncWorker.schedule(this)     // periodic check while closed
            } else {
                MessageNotifier.stop()
                MessageSyncWorker.cancel(this)
            }
        }
    }

    private fun setPresence(online: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).set(
            mapOf("online" to online, "lastSeen" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
    }

    /** While foreground, refresh lastSeen every 50s so others see an accurate
     *  online status (and a killed app's stale online flag expires on its own). */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = appScope.launch {
            while (isActive) {
                delay(50_000)
                setPresence(true)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
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
