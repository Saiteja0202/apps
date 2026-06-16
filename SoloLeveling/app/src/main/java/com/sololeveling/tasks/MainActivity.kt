package com.sololeveling.tasks

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sololeveling.tasks.ui.AppRoot
import com.sololeveling.tasks.ui.MainViewModel
import com.sololeveling.tasks.ui.theme.SoloLevelingTheme

class MainActivity : ComponentActivity() {
    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val vm: MainViewModel = viewModel()
            val dark = vm.data.themeDark
            SoloLevelingTheme(darkTheme = dark) {
                val view = LocalView.current
                val barColor = MaterialTheme.colorScheme.background.toArgb()
                SideEffect {
                    val w = (view.context as Activity).window
                    w.statusBarColor = barColor
                    WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(vm)
                }
            }
        }
    }
}
