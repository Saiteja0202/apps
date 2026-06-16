package com.friendschat.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide theme preference, persisted in SharedPreferences and observable by
 * Compose. Modes: "system" (follow device), "light", "dark".
 */
object ThemeState {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    var mode by mutableStateOf(SYSTEM)
        private set

    private const val PREFS = "friendschat_prefs"
    private const val KEY = "theme_mode"

    fun load(context: Context) {
        mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, SYSTEM) ?: SYSTEM
    }

    fun set(context: Context, newMode: String) {
        mode = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, newMode).apply()
    }
}
