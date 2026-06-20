package com.friendschat.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VerifyEmailScreen(
    onVerified: () -> Unit,
    vm: VerifyEmailViewModel = viewModel()
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(84.dp)) {
            Icon(
                Icons.Rounded.MarkEmailRead,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(20.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Verify your email", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "We sent a verification link to\n${vm.email}\n\nOpen it to confirm this is a real, working address, then come back and continue.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        vm.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { vm.recheck(onVerified) },
            enabled = !vm.checking,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (vm.checking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
            else Text("I've verified — continue", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { vm.resend() }, enabled = !vm.resending) {
            Text(if (vm.resending) "Sending…" else "Resend verification email")
        }
        TextButton(onClick = { vm.logout() }) {
            Text("Use a different account", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
