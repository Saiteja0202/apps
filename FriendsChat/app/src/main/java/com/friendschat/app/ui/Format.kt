package com.friendschat.app.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** WhatsApp-style relative time label. */
fun formatTime(date: Date?): String {
    if (date == null) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
    }
}

fun formatClock(date: Date?): String {
    if (date == null) return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

/** "last seen" style label: today at 14:05 / yesterday at 14:05 / 03 Jun. */
fun formatLastSeen(date: Date?): String {
    if (date == null) return "a while ago"
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }
    val clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "today at $clock"
        sameYear && dayDiff == 1 -> "yesterday at $clock"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
    }
}

fun humanSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.size - 1) { v /= 1024; i++ }
    return if (i == 0) "$bytes B" else String.format(Locale.US, "%.1f %s", v, units[i])
}
