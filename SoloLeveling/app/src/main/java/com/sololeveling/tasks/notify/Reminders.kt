package com.sololeveling.tasks.notify

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sololeveling.tasks.MainActivity
import com.sololeveling.tasks.R
import com.sololeveling.tasks.data.QuestType
import com.sololeveling.tasks.data.Store
import com.sololeveling.tasks.data.today
import java.util.Calendar

object Reminders {
    private const val REQ = 7001

    fun schedule(context: Context, minuteOfDay: Int) {
        if (minuteOfDay < 0) { cancel(context); return }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(context))
    }

    private fun pi(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQ, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = Store(context).load()
        val t = today()
        val pending = data.quests.count { it.type != QuestType.TODO && it.activeOn(t) && !it.isDoneOn(t) }
        val title = "Today's tasks"
        val body = if (pending > 0) "You have $pending task(s) left today. Tap to finish them."
                   else "All tasks done today — nice work! Keep your streak going."

        val openIntent = Intent(context, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val content = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, "quests")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setContentIntent(content)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.NotificationManagerCompat.from(context).notify(101, n)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val data = Store(context).load()
            Reminders.schedule(context, data.dailyReminderMinutes)
        }
    }
}
