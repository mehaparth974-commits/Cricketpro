package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CricketMatch
import com.example.domain.MatchStatus
import com.example.ui.components.CricketEmptyStateCard
import com.example.ui.components.CricketErrorCard
import com.example.ui.components.CricketLoadingCard
import com.example.ui.components.LivePulsePill
import com.example.ui.components.StadiumHeroBanner
import com.example.ui.components.SyncStatusBadge
import com.example.ui.components.TeamCrestBadge
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.HubData
import com.example.viewmodel.HubViewModel
import com.example.viewmodel.UiState

@Composable
fun HomeHubScreen(
    currentUserId: String,
    hubViewModel: HubViewModel,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onStartQuickMatch: () -> Unit,
    onOpenScorer: (String) -> Unit,
    onOpenMatchCenter: (String) -> Unit,
    onOpenTournaments: () -> Unit
) {
    val state by hubViewModel.hubState.collectAsStateWithLifecycle()
    val actionMessage by hubViewModel.actionMessage.collectAsStateWithLifecycle()
    var showCreateTeamDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CricketPro",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Every Ball. Every Run. Every Record.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val syncCount = (state as? UiState.Success)?.data?.pendingSyncCount ?: 0
                    SyncStatusBadge(pendingCount = syncCount)
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (themeMode == ThemeMode.LIGHT) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Toggle dark or light theme"
                        )
                    }
                }
            }
        }

        if (actionMessage != null) {
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
                        Text(
                            text = actionMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(onClick = { hubViewModel.clearMessage() }) {
                            Text("OK")
                        }
                    }
                }
            }
        }

        when (val ui = state) {
            is UiState.Loading -> {
                item { CricketLoadingCard("Loading live matches & squads...") }
            }
            is UiState.Error -> {
                item { CricketErrorCard(message = ui.message) }
            }
            is UiState.Success -> {
                val data: HubData = ui.data
                val banner = data.appConfig?.announcementBanner.orEmpty()
                if (banner.isNotBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Text(banner, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Hero Pitch Banner & Quick Actions
                item {
                    StadiumHeroBanner(
                        title = "Live Match Command",
                        subtitle = "${data.matches.count { it.matchStatus == MatchStatus.LIVE || it.matchStatus == MatchStatus.INNINGS_BREAK }} live • ${data.tournaments.size} tournaments • ${data.teams.size} squads",
                        badgeText = "REAL-TIME TELEMETRY"
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onStartQuickMatch,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("hub_start_match_button")
                        ) {
                            Icon(Icons.Default.SportsCricket, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Quick Match")
                        }
                        OutlinedButton(
                            onClick = { showCreateTeamDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("hub_create_team_button")
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("New Team")
                        }
                    }
                }

                // Live Matches Section
                val liveMatches = data.matches.filter {
                    it.matchStatus == MatchStatus.LIVE || it.matchStatus == MatchStatus.INNINGS_BREAK
                }
                item {
                    SectionHeader(title = "Live Now", trailing = "${liveMatches.size} active")
                }

                if (liveMatches.isEmpty()) {
                    item {
                        CricketEmptyStateCard(
                            title = "No Live Matches Right Now",
                            subtitle = "Create two teams and launch a ball-by-ball live match in seconds.",
                            actionLabel = "Start Live Match",
                            onAction = onStartQuickMatch
                        )
                    }
                } else {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(liveMatches, key = { it.id }) { match ->
                                LiveMatchCard(
                                    match = match,
                                    isScorer = match.scorerUid == currentUserId,
                                    onOpenScorer = { onOpenScorer(match.id) },
                                    onOpenCenter = { onOpenMatchCenter(match.id) },
                                    modifier = Modifier.width(310.dp)
                                )
                            }
                        }
                    }
                }

                // Active Tournaments Strip
                if (data.tournaments.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Tournaments",
                            trailing = "View All",
                            onTrailingClick = onOpenTournaments
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(data.tournaments.take(5), key = { it.id }) { t ->
                                Card(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .clickable { onOpenTournaments() },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Text(t.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text(
                                            "${t.formatEnum.label} • ${t.oversPerMatch} ov • ${t.teamIds.size} teams",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Upcoming & Completed Matches
                val otherMatches = data.matches.filter {
                    it.matchStatus == MatchStatus.UPCOMING || it.matchStatus == MatchStatus.COMPLETED
                }
                item {
                    SectionHeader(title = "Fixtures & Results", trailing = "${otherMatches.size} matches")
                }

                if (otherMatches.isEmpty()) {
                    item {
                        Text(
                            text = "Completed and upcoming matches will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(otherMatches, key = { it.id }) { match ->
                        LiveMatchCard(
                            match = match,
                            isScorer = match.scorerUid == currentUserId,
                            onOpenScorer = { onOpenScorer(match.id) },
                            onOpenCenter = { onOpenMatchCenter(match.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showCreateTeamDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateTeamDialog = false },
            onCreate = { name, short, city, captain, players ->
                hubViewModel.createTeam(name, short, city, captain, players, null)
                showCreateTeamDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailing: String,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = if (onTrailingClick != null) Modifier.clickable { onTrailingClick() } else Modifier
        )
    }
}

@Composable
fun LiveMatchCard(
    match: CricketMatch,
    isScorer: Boolean,
    onOpenScorer: () -> Unit,
    onOpenCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onOpenCenter() }
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (match.matchStatus == MatchStatus.LIVE) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.tournamentName ?: match.groundName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                when (match.matchStatus) {
                    MatchStatus.LIVE -> LivePulsePill("LIVE")
                    MatchStatus.INNINGS_BREAK -> LivePulsePill("BREAK")
                    MatchStatus.COMPLETED -> Text("FINAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    MatchStatus.UPCOMING -> Text("UPCOMING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }

            // Team A row
            TeamScoreRow(
                shortName = match.teamAShort,
                teamName = match.teamAName,
                runs = match.teamARuns,
                wickets = match.teamAWickets,
                overs = match.formatOvers(match.teamABalls),
                isHighlight = match.currentInnings == 1 && match.matchStatus == MatchStatus.LIVE
            )

            // Team B row
            TeamScoreRow(
                shortName = match.teamBShort,
                teamName = match.teamBName,
                runs = match.teamBRuns,
                wickets = match.teamBWickets,
                overs = match.formatOvers(match.teamBBalls),
                isHighlight = match.currentInnings == 2 && match.matchStatus == MatchStatus.LIVE
            )

            Text(
                text = match.resultText.ifBlank { "${match.oversLimit} Overs • ${match.groundName}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isScorer && match.matchStatus != MatchStatus.COMPLETED) {
                    Button(
                        onClick = onOpenScorer,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resume_scoring_${match.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (match.matchStatus == MatchStatus.UPCOMING) "Start Scoring" else "Score Live")
                    }
                }
                FilledTonalButton(
                    onClick = onOpenCenter,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_center_${match.id}")
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Match Center")
                }
            }
        }
    }
}

@Composable
private fun TeamScoreRow(
    shortName: String,
    teamName: String,
    runs: Int,
    wickets: Int,
    overs: String,
    isHighlight: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            TeamCrestBadge(shortName = shortName, size = 34.dp)
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "$runs/$wickets ($overs)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
