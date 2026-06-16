package com.sololeveling.tasks

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SoloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("quests", "Daily Quests", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "The System's daily quest reminders" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}
