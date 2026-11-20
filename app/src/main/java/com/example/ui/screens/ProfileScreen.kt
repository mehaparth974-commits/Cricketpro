package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.PlayerProfile
import com.example.domain.PrivateAccount
import com.example.ui.components.CricketErrorCard
import com.example.ui.components.CricketLoadingCard
import com.example.ui.components.TeamCrestBadge
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.ProfileAdminViewModel
import com.example.viewmodel.ProfileScreenData
import com.example.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileAdminViewModel,
    themeMode: ThemeMode,
    hapticsEnabled: Boolean,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetHapticsEnabled: (Boolean) -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onOpenMatchCenter: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val state by profileViewModel.profileState.collectAsStateWithLifecycle()
    val message by profileViewModel.message.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }

    val currentData = (state as? UiState.Success)?.data

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && currentData != null) {
            profileViewModel.uploadAvatarAndSave(uri) { encoded ->
                val p = currentData.profile
                val priv = currentData.privateAccount
                profileViewModel.updateProfile(
                    displayName = p.displayName,
                    handle = p.handle.ifBlank { "player" },
                    battingStyle = p.battingStyle,
                    bowlingStyle = p.bowlingStyle,
                    playingRole = p.playingRole,
                    city = p.city.orEmpty(),
                    bio = p.bio.orEmpty(),
                    privateStats = p.privateStats,
                    photoData = encoded,
                    email = priv?.email.orEmpty(),
                    phone = priv?.phone.orEmpty(),
                    notificationsEnabled = priv?.notificationsEnabled ?: true
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (message != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message!!, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        TextButton(onClick = { profileViewModel.clearMessage() }) { Text("OK") }
                    }
                }
            }
        }

        when (val ui = state) {
            is UiState.Loading -> item { CricketLoadingCard("Loading player career...") }
            is UiState.Error -> item { CricketErrorCard(ui.message) }
            is UiState.Success -> {
                val data: ProfileScreenData = ui.data
                val p = data.profile

                // Profile Header
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                TeamCrestBadge(
                                    shortName = p.displayName.take(2),
                                    logoData = p.photoData,
                                    size = 72.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(p.displayName, style = MaterialTheme.typography.headlineSmall)
                                        if (data.isAdmin) {
                                            Icon(Icons.Default.Verified, contentDescription = "Admin", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text("@${p.handle} • ${p.playingRole}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Text("${p.battingStyle} • ${p.bowlingStyle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!p.city.isNullOrBlank()) {
                                        Text(p.city, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if (!p.bio.isNullOrBlank()) {
                                Text(p.bio, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier.weight(1f).testTag("pick_avatar_button")
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Photo")
                                }
                                OutlinedButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier.weight(1f).testTag("edit_profile_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit & Privacy")
                                }
                            }
                        }
                    }
                }

                // Career Highlights Grid
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Batting & Career Records", style = MaterialTheme.typography.titleMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CareerStat("Matches", "${p.matchesPlayed}")
                                CareerStat("Runs", "${p.runsScored}")
                                CareerStat("High Score", "${p.highestScore}")
                                CareerStat("MVP", String.format(Locale.US, "%.1f", p.mvpPoints))
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CareerStat("Average", String.format(Locale.US, "%.1f", p.battingAverage))
                                CareerStat("Strike Rate", String.format(Locale.US, "%.1f", p.strikeRate))
                                CareerStat("4s / 6s", "${p.fours}/${p.sixes}")
                                CareerStat("50s / 100s", "${p.fifties}/${p.hundreds}")
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Bowling & Fielding", style = MaterialTheme.typography.titleMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CareerStat("Wickets", "${p.wicketsTaken}")
                                CareerStat("Best", p.bestBowlingText)
                                CareerStat("Economy", String.format(Locale.US, "%.2f", p.bowlingEconomy))
                                CareerStat("Catches", "${p.catches + p.stumpings + p.runOuts}")
                            }
                        }
                    }
                }

                // Badges
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Achievements & Badges", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                data.earnedBadges.forEach { badge ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = badge,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Preferences & Admin Entry
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Appearance & Controls", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    ThemeMode.DARK to "Broadcast Dark",
                                    ThemeMode.LIGHT to "Daylight Light",
                                    ThemeMode.SYSTEM to "System"
                                ).forEach { (mode, label) ->
                                    FilterChip(
                                        selected = themeMode == mode,
                                        onClick = { onSetThemeMode(mode) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pitchside Haptic Feedback", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = hapticsEnabled, onCheckedChange = onSetHapticsEnabled)
                            }
                            if (data.isAdmin) {
                                Button(
                                    onClick = onOpenAdminDashboard,
                                    modifier = Modifier.fillMaxWidth().testTag("open_admin_dashboard_button")
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Open Admin Dashboard")
                                }
                            }
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier.fillMaxWidth().testTag("sign_out_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Sign Out")
                            }
                        }
                    }
                }

                // Match History
                item {
                    Text("Recent Match History (${data.myMatches.size})", style = MaterialTheme.typography.titleMedium)
                }
                if (data.myMatches.isEmpty()) {
                    item {
                        Text(
                            "Matches you score or play in will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(data.myMatches.take(10), key = { it.id }) { m ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth().clickable { onOpenMatchCenter(m.id) }
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${m.teamAShort} vs ${m.teamBShort}", style = MaterialTheme.typography.titleSmall)
                                Text(m.resultText.ifBlank { "${m.oversLimit} Overs • ${m.groundName}" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && currentData != null) {
        EditProfileDialog(
            profile = currentData.profile,
            privateAccount = currentData.privateAccount,
            onDismiss = { showEditDialog = false },
            onSave = { name, handle, bat, bowl, role, city, bio, privStats, email, phone, notif ->
                profileViewModel.updateProfile(
                    displayName = name,
                    handle = handle,
                    battingStyle = bat,
                    bowlingStyle = bowl,
                    playingRole = role,
                    city = city,
                    bio = bio,
                    privateStats = privStats,
                    photoData = currentData.profile.photoData,
                    email = email,
                    phone = phone,
                    notificationsEnabled = notif
                )
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun CareerStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditProfileDialog(
    profile: PlayerProfile,
    privateAccount: PrivateAccount?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, Boolean, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(profile.displayName) }
    var handle by remember { mutableStateOf(profile.handle) }
    var role by remember { mutableStateOf(profile.playingRole) }
    var bat by remember { mutableStateOf(profile.battingStyle) }
    var bowl by remember { mutableStateOf(profile.bowlingStyle) }
    var city by remember { mutableStateOf(profile.city.orEmpty()) }
    var bio by remember { mutableStateOf(profile.bio.orEmpty()) }
    var privateStats by remember { mutableStateOf(profile.privateStats) }
    var email by remember { mutableStateOf(privateAccount?.email.orEmpty()) }
    var phone by remember { mutableStateOf(privateAccount?.phone.orEmpty()) }
    var notif by remember { mutableStateOf(privateAccount?.notificationsEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile & Privacy") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true)
                OutlinedTextField(value = handle, onValueChange = { handle = it }, label = { Text("Handle") }, singleLine = true)
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Playing Role") }, singleLine = true)
                OutlinedTextField(value = bat, onValueChange = { bat = it }, label = { Text("Batting Style") }, singleLine = true)
                OutlinedTextField(value = bowl, onValueChange = { bowl = it }, label = { Text("Bowling Style") }, singleLine = true)
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City / Club") }, singleLine = true)
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, minLines = 2)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Private Contact Email") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Private Mobile Number") }, singleLine = true)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Private Stats Mode", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = privateStats, onCheckedChange = { privateStats = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications Enabled", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = notif, onCheckedChange = { notif = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, handle, bat, bowl, role, city, bio, privateStats, email, phone, notif) },
                enabled = name.trim().isNotEmpty() && handle.trim().length >= 2
            ) { Text("Save Profile") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
