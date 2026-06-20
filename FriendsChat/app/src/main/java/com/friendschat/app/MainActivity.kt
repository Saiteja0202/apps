package com.friendschat.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.friendschat.app.ads.RewardedAdManager
import com.friendschat.app.ui.AppRoot
import com.friendschat.app.ui.theme.EmberTheme
import com.friendschat.app.ui.theme.ThemeState

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Chat id from a tapped message notification, consumed once by the UI.
    private val pendingChatId = mutableStateOf<String?>(null)

    // Stack trace from a previous crash (if any), shown on this launch.
    private val lastCrash = mutableStateOf<String?>(null)

    override fun onResume() {
        super.onResume()
        // Shows a rewarded ad if 12+ hours have passed since the last one.
        RewardedAdManager.maybeShowAd(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("openChatId")?.let { pendingChatId.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeState.load(this)
        lastCrash.value = CrashGuard.consume(this)
        pendingChatId.value = intent?.getStringExtra("openChatId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val dark = ThemeState.mode != ThemeState.LIGHT   // light, or dark by default
            EmberTheme(darkTheme = dark) {
                val view = LocalView.current
                val barColor = MaterialTheme.colorScheme.surface.toArgb()
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = barColor
                    WindowCompat.getInsetsController(window, view)
                        .isAppearanceLightStatusBars = !dark
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        openChatId = pendingChatId.value,
                        onChatConsumed = { pendingChatId.value = null }
                    )
                }
                lastCrash.value?.let { report ->
                    CrashReportDialog(report) { lastCrash.value = null }
                }
            }
        }
    }
}

/** Shows the previous crash's stack trace so it can be screenshotted/reported. */
@Composable
private fun CrashReportDialog(report: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } },
        title = { Text("App crashed last time") },
        text = {
            Text(
                report,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())
            )
        }
    )
}
