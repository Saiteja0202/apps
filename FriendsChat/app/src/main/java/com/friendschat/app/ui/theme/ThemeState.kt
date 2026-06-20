package com.friendschat.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide theme preference, persisted in SharedPreferences and observable by
 * Compose. Modes: "light", "dark". (The old "system" mode is gone — it behaved
 * the same as dark — and any saved "system" value is migrated to dark.)
 */
object ThemeState {
    const val LIGHT = "light"
    const val DARK = "dark"

    var mode by mutableStateOf(DARK)
        private set

    private const val PREFS = "friendschat_prefs"
    private const val KEY = "theme_mode"

    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, DARK)
        mode = if (saved == LIGHT) LIGHT else DARK   // anything else (incl. legacy "system") → dark
    }

    fun set(context: Context, newMode: String) {
        mode = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, newMode).apply()
    }
}
