package com.friendschat.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.friendschat.app.data.AuthRepository
import com.friendschat.app.ui.auth.ForgotPasswordScreen
import com.friendschat.app.ui.auth.LoginScreen
import com.friendschat.app.ui.auth.RegisterScreen
import com.friendschat.app.ui.auth.VerifyEmailScreen
import com.friendschat.app.ui.chat.ChatScreen
import com.friendschat.app.ui.discover.DiscoverScreen
import com.friendschat.app.ui.likes.LikesScreen
import com.friendschat.app.ui.matches.MatchesScreen
import com.friendschat.app.ui.matches.MatchesViewModel
import com.friendschat.app.ui.onboarding.OnboardingFlow
import com.friendschat.app.ui.profile.EditProfileScreen
import com.friendschat.app.ui.profile.ProfileScreen

/** Top-level router: auth flow → onboarding gate → main tabbed app. */
@Composable
fun AppRoot(openChatId: String? = null, onChatConsumed: () -> Unit = {}) {
    val authRepo = remember { AuthRepository() }
    var loggedIn by remember { mutableStateOf(authRepo.isLoggedIn()) }
    var verified by remember { mutableStateOf(authRepo.isEmailVerified()) }

    DisposableEffect(Unit) {
        val listener = authRepo.addAuthStateListener { loggedIn = it }
        onDispose { authRepo.removeAuthStateListener(listener) }
    }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            // Refresh server state so a just-clicked verification link is reflected.
            runCatching { authRepo.reloadUser() }
            verified = authRepo.isEmailVerified()
            runCatching { authRepo.ensureProfileName() }
        } else {
            verified = false
        }
    }

    when {
        !loggedIn -> AuthFlow()
        !verified -> VerifyEmailScreen(onVerified = { verified = true })
        else -> MainGate(openChatId = openChatId, onChatConsumed = onChatConsumed)
    }
}

@Composable
private fun AuthFlow() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onGoToRegister = { nav.navigate("register") },
                onForgotPassword = { nav.navigate("forgot") }
            )
        }
        composable("register") { RegisterScreen(onBack = { nav.popBackStack() }) }
        composable("forgot") { ForgotPasswordScreen(onBack = { nav.popBackStack() }) }
    }
}

/** Decides between onboarding and the main app based on the loaded profile. */
@Composable
private fun MainGate(
    openChatId: String? = null,
    onChatConsumed: () -> Unit = {},
    vm: GateViewModel = viewModel()
) {
    val me by vm.me.collectAsState()
    when {
        me == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        !me!!.onboarded -> OnboardingFlow(onDone = {})
        else -> MainTabs(openChatId = openChatId, onChatConsumed = onChatConsumed)
    }
}

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Discover : Tab("discover", "Discover", Icons.Filled.Whatshot)
    data object Likes : Tab("likes", "Likes", Icons.Filled.Favorite)
    data object Matches : Tab("matches", "Matches", Icons.AutoMirrored.Filled.Chat)
    data object Profile : Tab("profile", "Profile", Icons.Filled.Person)
}

private val TABS = listOf(Tab.Discover, Tab.Likes, Tab.Matches, Tab.Profile)

@Composable
private fun MainTabs(openChatId: String? = null, onChatConsumed: () -> Unit = {}) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in TABS.map { it.route }

    // A tapped message notification asks us to open a specific chat.
    LaunchedEffect(openChatId) {
        if (!openChatId.isNullOrBlank()) {
            nav.navigate("chat/$openChatId") { launchSingleTop = true }
            onChatConsumed()
        }
    }

    // Unread-chat count for the Matches tab badge.
    val matchesVm: MatchesViewModel = viewModel()
    val matchRows by matchesVm.matches.collectAsState()
    val unreadCount = matchRows.count { it.unread }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (tab is Tab.Matches && unreadCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            NavHost(navController = nav, startDestination = Tab.Discover.route) {
                composable(Tab.Discover.route) {
                    DiscoverScreen(onOpenChat = { nav.navigate("chat/$it") })
                }
                composable(Tab.Likes.route) {
                    LikesScreen(onOpenChat = { nav.navigate("chat/$it") })
                }
                composable(Tab.Matches.route) {
                    MatchesScreen(onOpenChat = { nav.navigate("chat/$it") })
                }
                composable(Tab.Profile.route) {
                    ProfileScreen(onEditProfile = { nav.navigate("editProfile") })
                }
                composable("editProfile") {
                    EditProfileScreen(
                        onboarding = false,
                        onBack = { nav.popBackStack() },
                        onDone = { nav.popBackStack() }
                    )
                }
                composable("chat/{chatId}") { entry ->
                    val chatId = entry.arguments?.getString("chatId") ?: return@composable
                    ChatScreen(
                        chatId = chatId,
                        onBack = { nav.popBackStack() },
                        onOpenGroupInfo = {}   // no groups in Ember
                    )
                }
            }
        }
    }
}
