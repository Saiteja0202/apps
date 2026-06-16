package com.friendschat.app.util

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import java.io.File

/** Simple AAC/m4a voice recorder writing to the app cache. */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    fun start(): Boolean = runCatching {
        val f = File(context.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        recorder = r; file = f; startedAt = SystemClock.elapsedRealtime()
        true
    }.getOrDefault(false)

    /** Returns (file uri, durationMs) or null if too short / failed. */
    fun stop(): Pair<Uri, Long>? {
        val r = recorder ?: return null
        val dur = SystemClock.elapsedRealtime() - startedAt
        runCatching { r.stop() }
        runCatching { r.release() }
        recorder = null
        val f = file ?: return null
        if (dur < 800 || !f.exists() || f.length() == 0L) { f.delete(); return null }
        return Uri.fromFile(f) to dur
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        file?.delete()
    }
}
