package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.Team
import com.example.ui.components.CricketLoadingCard
import com.example.viewmodel.HubViewModel
import com.example.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickMatchSetupScreen(
    hubViewModel: HubViewModel,
    onBack: () -> Unit,
    onMatchStarted: (String) -> Unit
) {
    BackHandler { onBack() }

    val hubState by hubViewModel.hubState.collectAsStateWithLifecycle()
    var selectedTeamA by remember { mutableStateOf<Team?>(null) }
    var selectedTeamB by remember { mutableStateOf<Team?>(null) }
    var oversLimit by remember { mutableIntStateOf(20) }
    var powerplayOvers by remember { mutableIntStateOf(6) }
    var groundName by remember { mutableStateOf("Emerald Turf Stadium") }
    var tossWinnerIsA by remember { mutableStateOf(true) }
    var tossDecision by remember { mutableStateOf("BAT") }
    var showCreateTeamDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start New Match") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("setup_back_button")) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val ui = hubState) {
                is UiState.Loading -> CricketLoadingCard("Loading squads...")
                is UiState.Error -> Text(ui.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    val teams = ui.data.teams
                    if (selectedTeamA == null && teams.isNotEmpty()) selectedTeamA = teams[0]
                    if (selectedTeamB == null && teams.size > 1) selectedTeamB = teams[1]

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Select Competing Teams", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { showCreateTeamDialog = true },
                            modifier = Modifier.testTag("setup_new_team_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Create Team")
                        }
                    }

                    if (teams.size < 2) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Create at least 2 teams to start scoring", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Add your club teams with captains and players, or create two teams right here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text("Team A (Home)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            teams.forEach { team ->
                                FilterChip(
                                    selected = selectedTeamA?.id == team.id,
                                    onClick = { selectedTeamA = team },
                                    label = { Text("${team.shortName} • ${team.name}") }
                                )
                            }
                        }

                        Text("Team B (Away)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            teams.filter { it.id != selectedTeamA?.id }.forEach { team ->
                                FilterChip(
                                    selected = selectedTeamB?.id == team.id,
                                    onClick = { selectedTeamB = team },
                                    label = { Text("${team.shortName} • ${team.name}") }
                                )
                            }
                        }
                    }

                    Text("Match Format (Overs)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 12, 16, 20, 50).forEach { ov ->
                            FilterChip(
                                selected = oversLimit == ov,
                                onClick = {
                                    oversLimit = ov
                                    powerplayOvers = when {
                                        ov <= 6 -> 2
                                        ov <= 12 -> 3
                                        ov <= 20 -> 6
                                        else -> 10
                                    }
                                },
                                label = { Text("$ov Ov") }
                            )
                        }
                    }

                    Text("Powerplay Overs: $powerplayOvers", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 2, 3, 4, 6, 10).filter { it <= oversLimit }.forEach { pp ->
                            FilterChip(
                                selected = powerplayOvers == pp,
                                onClick = { powerplayOvers = pp },
                                label = { Text("$pp Ov") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = groundName,
                        onValueChange = { groundName = it },
                        label = { Text("Cricket Ground / Venue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("match_ground_input")
                    )

                    val teamA = selectedTeamA
                    val teamB = selectedTeamB
                    if (teamA != null && teamB != null) {
                        Text("Toss Result", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = tossWinnerIsA,
                                onClick = { tossWinnerIsA = true },
                                label = { Text("${teamA.shortName} won toss") }
                            )
                            FilterChip(
                                selected = !tossWinnerIsA,
                                onClick = { tossWinnerIsA = false },
                                label = { Text("${teamB.shortName} won toss") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = tossDecision == "BAT",
                                onClick = { tossDecision = "BAT" },
                                label = { Text("Elected to BAT") }
                            )
                            FilterChip(
                                selected = tossDecision == "BOWL",
                                onClick = { tossDecision = "BOWL" },
                                label = { Text("Elected to BOWL") }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val winner = if (tossWinnerIsA) teamA.name else teamB.name
                                hubViewModel.createQuickMatch(
                                    tournamentId = null,
                                    tournamentName = null,
                                    teamA = teamA,
                                    teamB = teamB,
                                    oversLimit = oversLimit,
                                    powerplayOvers = powerplayOvers,
                                    groundName = groundName,
                                    tossWinner = winner,
                                    tossDecision = tossDecision,
                                    onMatchCreated = onMatchStarted
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("launch_live_scorer_button")
                        ) {
                            Icon(Icons.Default.SportsCricket, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Launch Live Scorer")
                        }
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
fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, shortName: String, city: String, captainName: String, playersCsv: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var captain by remember { mutableStateOf("") }
    var players by remember { mutableStateOf("Player 1, Player 2, Player 3, Player 4, Player 5, Player 6, Player 7, Player 8, Player 9, Player 10, Player 11") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Cricket Team") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (shortName.length < 3 && it.isNotBlank()) {
                            shortName = it.split(" ").filter { w -> w.isNotBlank() }.take(3)
                                .joinToString("") { w -> w.first().uppercase() }
                        }
                    },
                    label = { Text("Team Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("team_name_input")
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it.take(6).uppercase() },
                    label = { Text("Short Code (e.g. EMS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("team_short_input")
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City / Club") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = captain,
                    onValueChange = { captain = it },
                    label = { Text("Captain Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("team_captain_input")
                )
                OutlinedTextField(
                    value = players,
                    onValueChange = { players = it },
                    label = { Text("Squad Roster (comma separated)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("team_roster_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, shortName, city, captain, players) },
                enabled = name.trim().length >= 2 && shortName.trim().length >= 2 && captain.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_team_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
