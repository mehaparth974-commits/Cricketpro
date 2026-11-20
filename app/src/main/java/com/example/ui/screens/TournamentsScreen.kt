package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.ExportUtils
import com.example.domain.MatchStatus
import com.example.domain.Team
import com.example.domain.Tournament
import com.example.domain.TournamentFormat
import com.example.domain.TournamentStanding
import com.example.ui.components.CricketEmptyStateCard
import com.example.ui.components.CricketErrorCard
import com.example.ui.components.CricketLoadingCard
import com.example.ui.components.StadiumHeroBanner
import com.example.viewmodel.HubViewModel
import com.example.viewmodel.TournamentViewModel
import com.example.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentsScreen(
    currentUserId: String,
    currentUserName: String,
    tournamentViewModel: TournamentViewModel,
    hubViewModel: HubViewModel,
    selectedTournamentId: String?,
    onSelectTournament: (String?) -> Unit,
    onOpenScorer: (String) -> Unit,
    onOpenMatchCenter: (String) -> Unit
) {
    if (selectedTournamentId != null) {
        TournamentDetailView(
            tournamentId = selectedTournamentId,
            currentUserId = currentUserId,
            tournamentViewModel = tournamentViewModel,
            onBack = { onSelectTournament(null) },
            onOpenScorer = onOpenScorer,
            onOpenMatchCenter = onOpenMatchCenter
        )
        return
    }

    val state by tournamentViewModel.tournamentsState.collectAsStateWithLifecycle()
    val message by tournamentViewModel.message.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_tournament_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Tournament")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StadiumHeroBanner(
                    title = "Tournaments & Leagues",
                    subtitle = "Unlimited brackets, automatic fixtures, ICC Net Run Rate & leaderboards.",
                    badgeText = "TOURNAMENT SUITE"
                )
            }

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
                            TextButton(onClick = { tournamentViewModel.clearMessage() }) { Text("OK") }
                        }
                    }
                }
            }

            when (val ui = state) {
                is UiState.Loading -> item { CricketLoadingCard("Loading tournaments...") }
                is UiState.Error -> item { CricketErrorCard(ui.message) }
                is UiState.Success -> {
                    val data = ui.data
                    if (data.tournaments.isEmpty()) {
                        item {
                            CricketEmptyStateCard(
                                title = "No Tournaments Yet",
                                subtitle = "Create a Round-Robin, Knockout, League, or Group Stage tournament with automatic fixtures.",
                                actionLabel = "Create Tournament",
                                onAction = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(data.tournaments, key = { it.id }) { t ->
                            TournamentCard(
                                tournament = t,
                                matchCount = data.matches.count { it.tournamentId == t.id },
                                onClick = { onSelectTournament(t.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    val availableTeams = (state as? UiState.Success)?.data?.teams ?: emptyList()
    if (showCreateDialog) {
        CreateTournamentDialog(
            teams = availableTeams,
            onDismiss = { showCreateDialog = false },
            onNeedCreateTeam = {
                showCreateDialog = false
                showCreateTeamDialog = true
            },
            onCreate = { name, format, overs, pp, selectedTeams, rules ->
                tournamentViewModel.createTournament(
                    name = name,
                    format = format,
                    oversPerMatch = overs,
                    powerplayOvers = pp,
                    selectedTeams = selectedTeams,
                    rulesText = rules,
                    organizerName = currentUserName,
                    onCreated = { id -> onSelectTournament(id) }
                )
                showCreateDialog = false
            }
        )
    }

    if (showCreateTeamDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateTeamDialog = false },
            onCreate = { name, short, city, captain, players ->
                hubViewModel.createTeam(name, short, city, captain, players, null)
                showCreateTeamDialog = false
                showCreateDialog = true
            }
        )
    }
}

@Composable
private fun TournamentCard(
    tournament: Tournament,
    matchCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tournament_card_${tournament.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = tournament.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = tournament.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "${tournament.formatEnum.label} • ${tournament.oversPerMatch} Overs • ${tournament.teamIds.size} Teams • $matchCount Fixtures",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Organizer: ${tournament.organizerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TournamentDetailView(
    tournamentId: String,
    currentUserId: String,
    tournamentViewModel: TournamentViewModel,
    onBack: () -> Unit,
    onOpenScorer: (String) -> Unit,
    onOpenMatchCenter: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val detailFlow = remember(tournamentId) { tournamentViewModel.observeTournamentDetail(tournamentId) }
    val state by detailFlow.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Points Table", "Fixtures", "Leaderboard", "Rules")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (state as? UiState.Success)?.data?.tournament?.name ?: "Tournament",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tournament_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val data = (state as? UiState.Success)?.data
                    if (data != null) {
                        IconButton(
                            onClick = {
                                val top = data.standings.firstOrNull()
                                val summary = buildString {
                                    appendLine("CricketPro Tournament: ${data.tournament.name}")
                                    appendLine("Format: ${data.tournament.formatEnum.label} (${data.tournament.oversPerMatch} Overs)")
                                    appendLine("Teams: ${data.tournament.teamNames.joinToString(", ")}")
                                    if (top != null) {
                                        appendLine("Leader: ${top.teamName} (${top.points} pts, NRR ${String.format(Locale.US, "%+.3f", top.nrr)})")
                                    }
                                }
                                ExportUtils.shareText(context, data.tournament.name, summary)
                            },
                            modifier = Modifier.testTag("share_tournament_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Tournament")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val ui = state) {
            is UiState.Loading -> Box(Modifier.padding(innerPadding)) { CricketLoadingCard("Loading tournament...") }
            is UiState.Error -> Box(Modifier.padding(innerPadding).padding(16.dp)) { CricketErrorCard(ui.message) }
            is UiState.Success -> {
                val data = ui.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TabRow(selectedTabIndex = tab) {
                        tabs.forEachIndexed { idx, label ->
                            Tab(
                                selected = tab == idx,
                                onClick = { tab = idx },
                                text = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("tour_tab_$idx")
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (tab) {
                            0 -> {
                                item {
                                    PointsTableCard(standings = data.standings)
                                }
                            }
                            1 -> {
                                if (data.fixtures.isEmpty()) {
                                    item {
                                        CricketEmptyStateCard(
                                            title = "No Fixtures Generated",
                                            subtitle = "Create matches or add teams to view fixtures."
                                        )
                                    }
                                } else {
                                    items(data.fixtures, key = { it.id }) { match ->
                                        LiveMatchCard(
                                            match = match,
                                            isScorer = match.scorerUid == currentUserId || data.tournament.organizerUid == currentUserId,
                                            onOpenScorer = { onOpenScorer(match.id) },
                                            onOpenCenter = { onOpenMatchCenter(match.id) }
                                        )
                                    }
                                }
                            }
                            2 -> {
                                item {
                                    val completed = data.fixtures.count { it.matchStatus == MatchStatus.COMPLETED }
                                    val totalRuns = data.standings.sumOf { it.runsFor }
                                    val mvpList = data.fixtures.mapNotNull { it.mvpPlayerName?.takeIf { n -> n.isNotBlank() } }
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Tournament Statistics", style = MaterialTheme.typography.titleMedium)
                                            Text("Matches Completed: $completed / ${data.fixtures.size}", style = MaterialTheme.typography.bodyMedium)
                                            Text("Aggregate Tournament Runs: $totalRuns", style = MaterialTheme.typography.bodyMedium)
                                            if (mvpList.isNotEmpty()) {
                                                Text("Match MVPs: ${mvpList.distinct().joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                                items(data.standings, key = { "lb_${it.id}" }) { row ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(row.teamName, style = MaterialTheme.typography.titleSmall)
                                                Text("Runs Scored: ${row.runsFor} • Conceded: ${row.runsAgainst}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                text = "NRR ${String.format(Locale.US, "%+.3f", row.nrr)}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Tournament Rules & Format", style = MaterialTheme.typography.titleMedium)
                                            Text("Format: ${data.tournament.formatEnum.label}", style = MaterialTheme.typography.bodyMedium)
                                            Text("Overs per Match: ${data.tournament.oversPerMatch} (Powerplay: ${data.tournament.powerplayOvers} ov)", style = MaterialTheme.typography.bodyMedium)
                                            Text("Win = 2 pts, Tie/NR = 1 pt. Net Run Rate calculated per ICC overs-faced/bowled formula.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (!data.tournament.rulesText.isNullOrBlank()) {
                                                HorizontalDivider()
                                                Text("Custom Rules:", style = MaterialTheme.typography.labelLarge)
                                                Text(data.tournament.rulesText, style = MaterialTheme.typography.bodyMedium)
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
    }
}

@Composable
private fun PointsTableCard(standings: List<TournamentStanding>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Standings & Net Run Rate", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Team", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2.2f))
                Text("P", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f))
                Text("W", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f))
                Text("L", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f))
                Text("PTS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.7f))
                Text("NRR", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.1f))
            }
            HorizontalDivider()
            standings.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(2.2f)) {
                        Text(row.teamName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!row.groupName.isNullOrBlank()) {
                            Text(row.groupName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("${row.played}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f))
                    Text("${row.won}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f))
                    Text("${row.lost}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f))
                    Text("${row.points}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(0.7f))
                    Text(String.format(Locale.US, "%+.3f", row.nrr), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateTournamentDialog(
    teams: List<Team>,
    onDismiss: () -> Unit,
    onNeedCreateTeam: () -> Unit,
    onCreate: (String, TournamentFormat, Int, Int, List<Team>, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(TournamentFormat.ROUND_ROBIN) }
    var overs by remember { mutableIntStateOf(20) }
    var powerplay by remember { mutableIntStateOf(6) }
    var rulesText by remember { mutableStateOf("ICC standard powerplay & super over on tie.") }
    val selectedTeamIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tournament") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tournament Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tournament_name_input")
                )
                Text("Format", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TournamentFormat.entries.forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = { format = f },
                            label = { Text(f.label) }
                        )
                    }
                }
                Text("Overs per Match", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 16, 20, 50).forEach { ov ->
                        FilterChip(
                            selected = overs == ov,
                            onClick = {
                                overs = ov
                                powerplay = if (ov <= 10) 2 else if (ov <= 20) 6 else 10
                            },
                            label = { Text("$ov Ov") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Teams (${selectedTeamIds.size})", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = onNeedCreateTeam) { Text("+ New Team") }
                }
                if (teams.size < 2) {
                    Text(
                        "Create at least 2 teams first to auto-generate fixtures.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        teams.forEach { team ->
                            val picked = selectedTeamIds.contains(team.id)
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    if (picked) selectedTeamIds.remove(team.id) else selectedTeamIds.add(team.id)
                                },
                                label = { Text(team.shortName) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = rulesText,
                    onValueChange = { rulesText = it },
                    label = { Text("Custom Tournament Rules") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val pickedTeams = teams.filter { selectedTeamIds.contains(it.id) }
            Button(
                onClick = { onCreate(name, format, overs, powerplay, pickedTeams, rulesText) },
                enabled = name.trim().length >= 2 && pickedTeams.size >= 2,
                modifier = Modifier.testTag("confirm_create_tournament_button")
            ) {
                Text("Create & Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
