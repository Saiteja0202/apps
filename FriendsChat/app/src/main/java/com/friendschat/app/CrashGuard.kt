package com.friendschat.app

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Captures any uncaught exception to a file so it can be shown on the next launch
 * (so a crash can be diagnosed without adb/logcat). It does NOT swallow the crash —
 * it records the trace, then lets the default handler terminate the app as usual.
 */
object CrashGuard {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val appCtx = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            runCatching {
                val sw = StringWriter()
                ex.printStackTrace(PrintWriter(sw))
                File(appCtx.filesDir, FILE).writeText("Thread: ${thread.name}\n\n$sw")
            }
            previous?.uncaughtException(thread, ex)
        }
    }

    /** Returns the last crash trace (if any) and clears it. */
    fun consume(context: Context): String? {
        val f = File(context.filesDir, FILE)
        if (!f.exists()) return null
        val text = runCatching { f.readText() }.getOrNull()
        runCatching { f.delete() }
        return text?.takeIf { it.isNotBlank() }
    }
}
