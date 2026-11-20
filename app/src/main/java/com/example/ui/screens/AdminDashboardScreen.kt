package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CricketEmptyStateCard
import com.example.ui.components.CricketErrorCard
import com.example.ui.components.CricketLoadingCard
import com.example.viewmodel.ProfileAdminViewModel
import com.example.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    profileViewModel: ProfileAdminViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    if (!isAdmin) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Console") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding).padding(16.dp)) {
                CricketErrorCard("Administrator role is required to access this console.")
            }
        }
        return
    }

    val adminState by profileViewModel.adminState.collectAsStateWithLifecycle()
    val message by profileViewModel.message.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Analytics & Config", "Moderation", "Users", "Entities")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (message != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message!!, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        TextButton(onClick = { profileViewModel.clearMessage() }) { Text("OK") }
                    }
                }
            }

            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                tabs.forEachIndexed { idx, label ->
                    Tab(selected = tab == idx, onClick = { tab = idx }, text = { Text(label) })
                }
            }

            when (val ui = adminState) {
                is UiState.Loading -> CricketLoadingCard("Loading Admin Telemetry...")
                is UiState.Error -> Box(Modifier.padding(16.dp)) { CricketErrorCard(ui.message) }
                is UiState.Success -> {
                    val data = ui.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (tab) {
                            0 -> {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Platform Analytics", style = MaterialTheme.typography.titleMedium)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                AdminMetric("Players", "${data.players.size}")
                                                AdminMetric("Teams", "${data.teams.size}")
                                                AdminMetric("Tournaments", "${data.tournaments.size}")
                                                AdminMetric("Matches", "${data.matches.size}")
                                            }
                                            Text(
                                                "Open Reports: ${data.reports.count { it.status == "OPEN" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }

                                item {
                                    var notifTitle by remember { mutableStateOf("") }
                                    var notifBody by remember { mutableStateOf("") }
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Broadcast Global Notification", style = MaterialTheme.typography.titleMedium)
                                            OutlinedTextField(
                                                value = notifTitle,
                                                onValueChange = { notifTitle = it },
                                                label = { Text("Title") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = notifBody,
                                                onValueChange = { notifBody = it },
                                                label = { Text("Message Body") },
                                                minLines = 2,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Button(
                                                onClick = {
                                                    profileViewModel.broadcastNotification(notifTitle, notifBody)
                                                    notifTitle = ""
                                                    notifBody = ""
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("admin_send_broadcast_button")
                                            ) {
                                                Icon(Icons.Default.Campaign, contentDescription = null)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Send Broadcast")
                                            }
                                        }
                                    }
                                }

                                item {
                                    var banner by remember { mutableStateOf(data.appConfig?.announcementBanner.orEmpty()) }
                                    var maintenance by remember { mutableStateOf(data.appConfig?.maintenanceMode ?: false) }
                                    var allowPosts by remember { mutableStateOf(data.appConfig?.allowCommunityPosts ?: true) }
                                    var defaultOvers by remember { mutableStateOf((data.appConfig?.defaultOvers ?: 20).toString()) }

                                    LaunchedEffect(data.appConfig) {
                                        data.appConfig?.let {
                                            banner = it.announcementBanner
                                            maintenance = it.maintenanceMode
                                            allowPosts = it.allowCommunityPosts
                                            defaultOvers = it.defaultOvers.toString()
                                        }
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("App Settings", style = MaterialTheme.typography.titleMedium)
                                            OutlinedTextField(
                                                value = banner,
                                                onValueChange = { banner = it },
                                                label = { Text("Hub Announcement Banner") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = defaultOvers,
                                                onValueChange = { defaultOvers = it.filter(Char::isDigit) },
                                                label = { Text("Default Match Overs") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Maintenance Mode")
                                                Switch(checked = maintenance, onCheckedChange = { maintenance = it })
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Allow Community Posts")
                                                Switch(checked = allowPosts, onCheckedChange = { allowPosts = it })
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    profileViewModel.updateAppConfig(
                                                        maintenanceMode = maintenance,
                                                        announcementBanner = banner,
                                                        defaultOvers = defaultOvers.toIntOrNull() ?: 20,
                                                        allowCommunityPosts = allowPosts
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("admin_save_config_button")
                                            ) {
                                                Text("Save App Settings")
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                if (data.reports.isEmpty()) {
                                    item {
                                        CricketEmptyStateCard(
                                            title = "No Moderation Reports",
                                            subtitle = "Reported posts or content will appear here for review."
                                        )
                                    }
                                } else {
                                    items(data.reports, key = { it.id }) { r ->
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("${r.targetType} • ${r.status}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                                    Text("By ${r.reporterName}", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Text(r.reason, style = MaterialTheme.typography.bodyMedium)
                                                if (r.status == "OPEN") {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Button(
                                                            onClick = { profileViewModel.resolveReport(r, "RESOLVED", true) },
                                                            modifier = Modifier.weight(1f)
                                                        ) { Text("Hide & Resolve") }
                                                        OutlinedButton(
                                                            onClick = { profileViewModel.resolveReport(r, "DISMISSED", false) },
                                                            modifier = Modifier.weight(1f)
                                                        ) { Text("Dismiss") }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                items(data.players, key = { it.userId }) { player ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(player.displayName, style = MaterialTheme.typography.titleSmall)
                                                Text("@${player.handle} • Role: ${player.role}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            OutlinedButton(onClick = { profileViewModel.toggleUserRole(player.userId, player.role) }) {
                                                Text(if (player.role == "admin") "Set User" else "Make Admin")
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                item { Text("Manage Teams (${data.teams.size})", style = MaterialTheme.typography.titleMedium) }
                                items(data.teams, key = { "adm_t_${it.id}" }) { t ->
                                    AdminEntityRow(t.name, "${t.shortName} • Capt: ${t.captainName}") {
                                        profileViewModel.deleteEntity("teams", t.id, t.name)
                                    }
                                }
                                item { Text("Manage Tournaments (${data.tournaments.size})", style = MaterialTheme.typography.titleMedium) }
                                items(data.tournaments, key = { "adm_tr_${it.id}" }) { tr ->
                                    AdminEntityRow(tr.name, "${tr.formatEnum.label} • ${tr.oversPerMatch} ov") {
                                        profileViewModel.deleteEntity("tournaments", tr.id, tr.name)
                                    }
                                }
                                item { Text("Manage Matches (${data.matches.size})", style = MaterialTheme.typography.titleMedium) }
                                items(data.matches, key = { "adm_m_${it.id}" }) { m ->
                                    AdminEntityRow("${m.teamAShort} vs ${m.teamBShort}", m.resultText.ifBlank { m.status }) {
                                        profileViewModel.deleteEntity("matches", m.id, "${m.teamAShort} vs ${m.teamBShort}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AdminEntityRow(title: String, subtitle: String, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete $title", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
