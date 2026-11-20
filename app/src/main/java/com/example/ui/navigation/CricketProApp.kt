package com.example.ui.navigation

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.domain.MatchStatus
import com.example.ui.components.CricketEmptyStateCard
import com.example.ui.components.CricketLoadingCard
import com.example.ui.components.StadiumHeroBanner
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.HomeHubScreen
import com.example.ui.screens.LiveMatchCard
import com.example.ui.screens.LiveScoringScreen
import com.example.ui.screens.MatchCenterScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuickMatchSetupScreen
import com.example.ui.screens.TournamentsScreen
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.CommunityViewModel
import com.example.viewmodel.HubViewModel
import com.example.viewmodel.ProfileAdminViewModel
import com.example.viewmodel.ScoringViewModel
import com.example.viewmodel.TournamentViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private enum class TopTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HUB("hub", "Hub", Icons.Default.Home),
    SCORER("scorer_hub", "Scorer", Icons.Default.SportsCricket),
    TOURNAMENTS("tournaments", "Events", Icons.Default.EmojiEvents),
    COMMUNITY("community", "Community", Icons.Default.Forum),
    PROFILE("profile", "Profile", Icons.Default.Person)
}

@Composable
fun CricketProApp(
    themeMode: ThemeMode,
    hapticsEnabled: Boolean,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetHapticsEnabled: (Boolean) -> Unit
) {
    val auth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = auth.currentUser
            }
        )
    } else {
        key(signedInUser.uid) {
            SignedInCricketShell(
                currentUserId = signedInUser.uid,
                fallbackDisplayName = signedInUser.displayName ?: "CricketPro Player",
                themeMode = themeMode,
                hapticsEnabled = hapticsEnabled,
                onSetThemeMode = onSetThemeMode,
                onSetHapticsEnabled = onSetHapticsEnabled
            )
        }
    }
}

@Composable
private fun SignedInCricketShell(
    currentUserId: String,
    fallbackDisplayName: String,
    themeMode: ThemeMode,
    hapticsEnabled: Boolean,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetHapticsEnabled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val scope = rememberCoroutineScope()
    val authRepo = remember(context) { AuthRepository(context) }
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val hubViewModel: HubViewModel = viewModel()
    val tournamentViewModel: TournamentViewModel = viewModel()
    val communityViewModel: CommunityViewModel = viewModel()
    val profileViewModel: ProfileAdminViewModel = viewModel()

    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val communityState by communityViewModel.communityState.collectAsStateWithLifecycle()
    val hubState by hubViewModel.hubState.collectAsStateWithLifecycle()

    val profileData = (profileState as? UiState.Success)?.data
    val currentUserName = profileData?.profile?.displayName?.ifBlank { fallbackDisplayName } ?: fallbackDisplayName
    val currentUserPhoto = profileData?.profile?.photoData
    val isAdmin = profileData?.isAdmin ?: authRepo.isBootstrappedAdmin()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: TopTab.HUB.route
    val showPrimaryChrome = TopTab.entries.any { it.route == currentRoute }

    var selectedTournamentId by remember { mutableStateOf<String?>(null) }

    // Request runtime notification permission on Android 13+
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Dispatch local notification when new unread broadcasts/alerts arrive
    val postedNotifIds = remember { mutableStateListOf<String>() }
    val notificationsEnabled = profileData?.privateAccount?.notificationsEnabled ?: true
    LaunchedEffect(communityState, notificationsEnabled) {
        if (!notificationsEnabled) return@LaunchedEffect
        val alerts = (communityState as? UiState.Success)?.data?.notifications ?: return@LaunchedEffect
        val fresh = alerts.firstOrNull { !it.read && !postedNotifIds.contains(it.id) } ?: return@LaunchedEffect
        postedNotifIds.add(fresh.id)
        dispatchLocalAlert(context, fresh.title, fresh.body, fresh.id.hashCode())
    }

    val toggleTheme = {
        val next = if (themeMode == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT
        onSetThemeMode(next)
    }

    fun navigateTopTab(route: String) {
        if (route == TopTab.TOURNAMENTS.route) selectedTournamentId = null
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val appConfig = (hubState as? UiState.Success)?.data?.appConfig

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showPrimaryChrome && !useRail) {
                    NavigationBar {
                        TopTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showPrimaryChrome && useRail) {
                    NavigationRail {
                        Spacer(Modifier.height(12.dp))
                        TopTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (appConfig?.maintenanceMode == true) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Maintenance Mode Active — Scoring remains synced locally.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = TopTab.HUB.route,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        composable(TopTab.HUB.route) {
                            HomeHubScreen(
                                currentUserId = currentUserId,
                                hubViewModel = hubViewModel,
                                themeMode = themeMode,
                                onToggleTheme = toggleTheme,
                                onStartQuickMatch = { navController.navigate("quick_match_setup") },
                                onOpenScorer = { matchId -> navController.navigate("live_scoring/$matchId") },
                                onOpenMatchCenter = { matchId -> navController.navigate("match_center/$matchId/0") },
                                onOpenTournaments = { navigateTopTab(TopTab.TOURNAMENTS.route) }
                            )
                        }

                        composable(TopTab.SCORER.route) {
                            ScorerLauncherTab(
                                currentUserId = currentUserId,
                                hubViewModel = hubViewModel,
                                onStartQuickMatch = { navController.navigate("quick_match_setup") },
                                onOpenScorer = { matchId -> navController.navigate("live_scoring/$matchId") },
                                onOpenMatchCenter = { matchId -> navController.navigate("match_center/$matchId/0") }
                            )
                        }

                        composable(TopTab.TOURNAMENTS.route) {
                            TournamentsScreen(
                                currentUserId = currentUserId,
                                currentUserName = currentUserName,
                                tournamentViewModel = tournamentViewModel,
                                hubViewModel = hubViewModel,
                                selectedTournamentId = selectedTournamentId,
                                onSelectTournament = { selectedTournamentId = it },
                                onOpenScorer = { matchId -> navController.navigate("live_scoring/$matchId") },
                                onOpenMatchCenter = { matchId -> navController.navigate("match_center/$matchId/0") }
                            )
                        }

                        composable(TopTab.COMMUNITY.route) {
                            CommunityScreen(
                                currentUserId = currentUserId,
                                currentUserName = currentUserName,
                                currentUserPhoto = currentUserPhoto,
                                communityViewModel = communityViewModel,
                                onOpenMatchCenter = { matchId -> navController.navigate("match_center/$matchId/0") }
                            )
                        }

                        composable(TopTab.PROFILE.route) {
                            ProfileScreen(
                                profileViewModel = profileViewModel,
                                themeMode = themeMode,
                                hapticsEnabled = hapticsEnabled,
                                onSetThemeMode = onSetThemeMode,
                                onSetHapticsEnabled = onSetHapticsEnabled,
                                onOpenAdminDashboard = {
                                    if (isAdmin) navController.navigate("admin_dashboard")
                                },
                                onOpenMatchCenter = { matchId -> navController.navigate("match_center/$matchId/0") },
                                onSignOut = {
                                    authRepo.signOut(credentialManager, scope)
                                }
                            )
                        }

                        composable("quick_match_setup") {
                            QuickMatchSetupScreen(
                                hubViewModel = hubViewModel,
                                onBack = { navController.popBackStack() },
                                onMatchStarted = { matchId ->
                                    navController.navigate("live_scoring/$matchId") {
                                        popUpTo("quick_match_setup") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "live_scoring/{matchId}",
                            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
                        ) { entry ->
                            val matchId = entry.arguments?.getString("matchId").orEmpty()
                            val scoringVm: ScoringViewModel = viewModel(
                                key = "scoring_$matchId",
                                factory = object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return ScoringViewModel(application, matchId) as T
                                    }
                                }
                            )
                            LiveScoringScreen(
                                scoringViewModel = scoringVm,
                                hapticsEnabled = hapticsEnabled,
                                onBack = { navController.popBackStack() },
                                onOpenMatchCenter = { navController.navigate("match_center/$matchId/0") },
                                onOpenBroadcast = { navController.navigate("match_center/$matchId/4") }
                            )
                        }

                        composable(
                            route = "match_center/{matchId}/{initialTab}",
                            arguments = listOf(
                                navArgument("matchId") { type = NavType.StringType },
                                navArgument("initialTab") { type = NavType.IntType }
                            )
                        ) { entry ->
                            val matchId = entry.arguments?.getString("matchId").orEmpty()
                            val initialTab = entry.arguments?.getInt("initialTab") ?: 0
                            val scoringVm: ScoringViewModel = viewModel(
                                key = "center_$matchId",
                                factory = object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return ScoringViewModel(application, matchId) as T
                                    }
                                }
                            )
                            MatchCenterScreen(
                                scoringViewModel = scoringVm,
                                currentUserId = currentUserId,
                                currentUserName = currentUserName,
                                currentUserPhoto = currentUserPhoto,
                                initialTab = initialTab,
                                onBack = { navController.popBackStack() },
                                onOpenLiveScorer = { navController.navigate("live_scoring/$matchId") }
                            )
                        }

                        composable("admin_dashboard") {
                            AdminDashboardScreen(
                                isAdmin = isAdmin,
                                profileViewModel = profileViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScorerLauncherTab(
    currentUserId: String,
    hubViewModel: HubViewModel,
    onStartQuickMatch: () -> Unit,
    onOpenScorer: (String) -> Unit,
    onOpenMatchCenter: (String) -> Unit
) {
    val state by hubViewModel.hubState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StadiumHeroBanner(
                title = "Pitchside Live Scorer",
                subtitle = "One-hand scoring keypad, offline queue sync, undo/edit ball & instant wagon wheel.",
                badgeText = "BALL-BY-BALL ENGINE"
            )
        }
        item {
            Button(
                onClick = onStartQuickMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("scorer_tab_new_match_button")
            ) {
                Icon(Icons.Default.SportsCricket, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start New Match")
            }
        }
        when (val ui = state) {
            is UiState.Loading -> item { CricketLoadingCard("Loading active matches...") }
            is UiState.Error -> item { Text(ui.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                val activeMatches = ui.data.matches.filter {
                    it.scorerUid == currentUserId && it.matchStatus != MatchStatus.COMPLETED
                }
                item {
                    Text("Your Active & Upcoming Matches (${activeMatches.size})", style = MaterialTheme.typography.titleMedium)
                }
                if (activeMatches.isEmpty()) {
                    item {
                        CricketEmptyStateCard(
                            title = "No Active Matches to Score",
                            subtitle = "Tap Start New Match above to pick two teams and begin scoring ball-by-ball.",
                            actionLabel = "Start New Match",
                            onAction = onStartQuickMatch
                        )
                    }
                } else {
                    items(activeMatches, key = { it.id }) { m ->
                        LiveMatchCard(
                            match = m,
                            isScorer = true,
                            onOpenScorer = { onOpenScorer(m.id) },
                            onOpenCenter = { onOpenMatchCenter(m.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun dispatchLocalAlert(
    context: Context,
    title: String,
    body: String,
    notificationId: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "cricketpro_alerts",
            "CricketPro Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, "cricketpro_alerts")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    manager.notify(notificationId, notification)
}
