package com.friendschat.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.friendschat.app.ui.components.Avatar
import com.friendschat.app.ui.components.LivePill
import com.friendschat.app.ui.components.ProfileCard
import com.friendschat.app.ui.theme.ThemeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onEditProfile: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val me by vm.me.collectAsState()
    val context = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }
    var deletePwd by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            me?.let { ProfileCard(user = it) }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Rounded.Edit, null)
                Spacer(Modifier.width(8.dp))
                Text("Edit profile", fontWeight = FontWeight.Bold)
            }

            // ---- Live Now (real-time availability) ----
            Spacer(Modifier.height(28.dp))
            Text(
                "AVAILABILITY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            val live = me?.isFreeNow == true
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (live) LivePill("You're live")
                        else Text("Go Live", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (live) "People in your city see you at the top, ready to chat — for the next hour."
                            else "Tell people in your city you're free to chat right now. You'll jump to the top of their feed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    if (live) OutlinedButton(onClick = { vm.goLive(false) }) { Text("Stop") }
                    else Button(onClick = { vm.goLive(true) }) { Text("Go Live", fontWeight = FontWeight.Bold) }
                }
            }

            // ---- Connections sub-tabs ----
            val likedYou by vm.likedYou.collectAsState()
            val youLiked by vm.youLiked.collectAsState()
            val blocked by vm.blocked.collectAsState()
            var tab by remember { mutableStateOf(0) }

            Spacer(Modifier.height(28.dp))
            Text(
                "CONNECTIONS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Liked you (${likedYou.size})") })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Interested (${youLiked.size})") })
                FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("Blocked (${blocked.size})") })
            }
            Spacer(Modifier.height(12.dp))
            when (tab) {
                0 -> if (likedYou.isEmpty()) ConnEmpty("No one has liked you yet.")
                    else likedYou.forEach { (_, u) -> ConnectionRow(u, null) {} }
                1 -> if (youLiked.isEmpty()) ConnEmpty("You haven't liked anyone yet.")
                    else youLiked.forEach { u -> ConnectionRow(u, null) {} }
                else -> if (blocked.isEmpty()) ConnEmpty("No blocked users.")
                    else blocked.forEach { u -> ConnectionRow(u, "Unblock") { vm.unblock(u.uid) } }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeOption("Light", Icons.Rounded.LightMode, ThemeState.mode == ThemeState.LIGHT, Modifier.weight(1f)) {
                    ThemeState.set(context, ThemeState.LIGHT)
                }
                ThemeOption("Dark", Icons.Rounded.DarkMode, ThemeState.mode == ThemeState.DARK, Modifier.weight(1f)) {
                    ThemeState.set(context, ThemeState.DARK)
                }
            }

            Spacer(Modifier.height(28.dp))
            OutlinedButton(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Log out", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { vm.clearError(); deletePwd = ""; showDelete = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Delete account", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDelete) {
        val dismiss = { if (!vm.deleting) { showDelete = false; deletePwd = ""; vm.clearError() } }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Delete account?") },
            text = {
                Column {
                    Text(
                        "This permanently deletes your account, profile, matches and chats. " +
                            "This cannot be undone."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deletePwd,
                        onValueChange = { deletePwd = it },
                        label = { Text("Confirm your password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    vm.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.deleteAccount(deletePwd) }, enabled = !vm.deleting) {
                    if (vm.deleting) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = dismiss, enabled = !vm.deleting) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ConnectionRow(user: com.friendschat.app.data.ChatUser, action: String?, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = user.name, photoUrl = user.photoUrl, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                buildString {
                    append(user.name.substringBefore(' '))
                    if (user.age in 18..120) append(", ${user.age}")
                },
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            if (user.location.isNotBlank()) {
                Text(user.location, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (action != null) {
            OutlinedButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun ConnEmpty(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
