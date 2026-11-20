package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.BallEvent
import com.example.domain.DismissalType
import com.example.domain.ExtraType
import com.example.domain.MatchStatus
import com.example.ui.components.CricketBallChip
import com.example.ui.components.CricketErrorCard
import com.example.ui.components.CricketLoadingCard
import com.example.ui.components.LivePulsePill
import com.example.ui.components.SyncStatusBadge
import com.example.ui.components.WagonWheelCanvas
import com.example.ui.theme.LocalCricketColors
import com.example.viewmodel.ActiveScoringData
import com.example.viewmodel.ScoringViewModel
import com.example.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveScoringScreen(
    scoringViewModel: ScoringViewModel,
    hapticsEnabled: Boolean,
    onBack: () -> Unit,
    onOpenMatchCenter: () -> Unit,
    onOpenBroadcast: () -> Unit
) {
    BackHandler { onBack() }

    val state by scoringViewModel.scoringState.collectAsStateWithLifecycle()
    val banner by scoringViewModel.statusBanner.collectAsStateWithLifecycle()
    val selStriker by scoringViewModel.selectedStriker.collectAsStateWithLifecycle()
    val selNonStriker by scoringViewModel.selectedNonStriker.collectAsStateWithLifecycle()
    val selBowler by scoringViewModel.selectedBowler.collectAsStateWithLifecycle()
    val shotAngle by scoringViewModel.shotAngleDeg.collectAsStateWithLifecycle()

    val haptics = LocalHapticFeedback.current

    var showWicketDialog by remember { mutableStateOf(false) }
    var pendingExtraType by remember { mutableStateOf<ExtraType?>(null) }
    var showPlayerPicker by remember { mutableStateOf<String?>(null) } // "STRIKER", "NON_STRIKER", "BOWLER"
    var editingBall by remember { mutableStateOf<BallEvent?>(null) }
    var showWagonZonePicker by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LivePulsePill("SCORER")
                        val sync = (state as? UiState.Success)?.data?.pendingSyncCount ?: 0
                        SyncStatusBadge(pendingCount = sync)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("scorer_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenBroadcast, modifier = Modifier.testTag("scorer_broadcast_button")) {
                        Icon(Icons.Default.LiveTv, contentDescription = "Live broadcast studio")
                    }
                    IconButton(onClick = onOpenMatchCenter, modifier = Modifier.testTag("scorer_analytics_button")) {
                        Icon(Icons.Default.Analytics, contentDescription = "Match analytics")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val ui = state) {
            is UiState.Loading -> Box(Modifier.padding(innerPadding)) { CricketLoadingCard("Initializing live pitchside scorer...") }
            is UiState.Error -> Box(Modifier.padding(innerPadding).padding(16.dp)) { CricketErrorCard(ui.message) }
            is UiState.Success -> {
                val data = ui.data
                val match = data.match
                val computed = data.computed
                val active = computed.activeInnings

                val activeStriker = selStriker ?: active.currentStriker
                val activeNonStriker = selNonStriker ?: active.currentNonStriker
                val activeBowler = selBowler ?: active.currentBowler

                val strikerStats = active.batters.firstOrNull { it.name == activeStriker }
                val nonStrikerStats = active.batters.firstOrNull { it.name == activeNonStriker }
                val bowlerStats = active.bowlers.firstOrNull { it.name == activeBowler }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Scrollable Telemetry Top Section
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (banner != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(banner!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { scoringViewModel.clearBanner() }) { Text("Dismiss") }
                                }
                            }
                        }

                        // Primary Broadcast Scoreboard Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${active.battingTeamName} • Innings ${active.inningsNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (active.powerplayActive) {
                                            BadgeChip("PP", MaterialTheme.colorScheme.primary)
                                        }
                                        if (active.freeHitActive) {
                                            BadgeChip("FREE HIT", MaterialTheme.colorScheme.tertiary)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    AnimatedContent(
                                        targetState = "${active.totalRuns}/${active.wickets}",
                                        label = "score_anim"
                                    ) { scoreText ->
                                        Text(
                                            text = scoreText,
                                            style = MaterialTheme.typography.displayLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Overs ${active.oversDisplay} / ${match.oversLimit}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "CRR ${String.format(Locale.US, "%.2f", active.crr)}" +
                                                if (active.target > 0) " • RRR ${String.format(Locale.US, "%.2f", active.rrr)}" else "",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = computed.resultText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Striker / Non-Striker / Bowler Quick Selectors
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PlayerTapPill(
                                        label = "STRIKER *",
                                        name = activeStriker,
                                        stat = "${strikerStats?.runs ?: 0}(${strikerStats?.balls ?: 0})",
                                        highlighted = true,
                                        onClick = { showPlayerPicker = "STRIKER" },
                                        modifier = Modifier.weight(1f).testTag("pick_striker_button")
                                    )
                                    PlayerTapPill(
                                        label = "NON-STRIKER",
                                        name = activeNonStriker,
                                        stat = "${nonStrikerStats?.runs ?: 0}(${nonStrikerStats?.balls ?: 0})",
                                        highlighted = false,
                                        onClick = { showPlayerPicker = "NON_STRIKER" },
                                        modifier = Modifier.weight(1f).testTag("pick_non_striker_button")
                                    )
                                }
                                PlayerTapPill(
                                    label = "BOWLER",
                                    name = activeBowler,
                                    stat = "${bowlerStats?.wickets ?: 0}/${bowlerStats?.runsConceded ?: 0} (${bowlerStats?.oversDisplay ?: "0.0"})",
                                    highlighted = false,
                                    onClick = { showPlayerPicker = "BOWLER" },
                                    modifier = Modifier.fillMaxWidth().testTag("pick_bowler_button")
                                )
                            }
                        }

                        // Current Over Ball Chips (Tap to Edit)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "This Over (tap ball to edit)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { showWagonZonePicker = !showWagonZonePicker }) {
                                    Text(if (showWagonZonePicker) "Hide Shot Zone" else "Shot Zone: ${angleToZoneName(shotAngle)}")
                                }
                            }
                            if (active.currentOverBalls.isEmpty()) {
                                Text(
                                    "New over ready — tap keypad below to score.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(active.currentOverBalls, key = { it.id.ifBlank { it.sequenceIndex.toString() } }) { ball ->
                                        CricketBallChip(
                                            ball = ball,
                                            modifier = Modifier
                                                .clickable { editingBall = ball }
                                                .testTag("ball_chip_${ball.sequenceIndex}")
                                        )
                                    }
                                }
                            }
                        }

                        if (showWagonZonePicker) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Tap ground to set next shot direction", style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.height(6.dp))
                                    WagonWheelCanvas(
                                        balls = data.balls.filter { it.innings == active.inningsNumber },
                                        selectedAngleDeg = shotAngle,
                                        onSelectAngle = { scoringViewModel.setShotAngle(it) }
                                    )
                                }
                            }
                        }

                        // Innings Break / Match Completion Prompt
                        if (computed.matchStatus == MatchStatus.INNINGS_BREAK && active.inningsNumber == 1) {
                            Button(
                                onClick = { scoringViewModel.startSecondInnings(data) },
                                modifier = Modifier.fillMaxWidth().testTag("start_second_innings_button")
                            ) {
                                Text("Start 2nd Innings (Target ${active.totalRuns + 1})")
                            }
                        } else if (active.inningsNumber == 2 && active.inningsCompleted && match.matchStatus != MatchStatus.COMPLETED) {
                            Button(
                                onClick = { scoringViewModel.completeMatch(data) },
                                modifier = Modifier.fillMaxWidth().testTag("complete_match_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Finish & Save Match Result")
                            }
                        }
                    }

                    // Bottom-Anchored One-Hand Pitchside Keypad
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Quick Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                item {
                                    QuickChip("WD") { triggerHaptic(); pendingExtraType = ExtraType.WIDE }
                                }
                                item {
                                    QuickChip("NB") { triggerHaptic(); pendingExtraType = ExtraType.NO_BALL }
                                }
                                item {
                                    QuickChip("BYE") { triggerHaptic(); pendingExtraType = ExtraType.BYE }
                                }
                                item {
                                    QuickChip("LB") { triggerHaptic(); pendingExtraType = ExtraType.LEG_BYE }
                                }
                                item {
                                    QuickChip("SWAP", Icons.Default.SwapHoriz) {
                                        triggerHaptic()
                                        scoringViewModel.swapStrike(computed)
                                    }
                                }
                                item {
                                    QuickChip("UNDO", Icons.AutoMirrored.Filled.Undo) {
                                        triggerHaptic()
                                        scoringViewModel.undoLastBall(data)
                                    }
                                }
                                item {
                                    QuickChip("MORE", Icons.Default.MoreHoriz) {
                                        showMoreActions = true
                                    }
                                }
                            }

                            // Row 2: 0, 1, 2, 3
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0, 1, 2, 3).forEach { r ->
                                    RunKeypadButton(
                                        label = r.toString(),
                                        bg = MaterialTheme.colorScheme.surface,
                                        fg = MaterialTheme.colorScheme.onSurface,
                                        onClick = {
                                            triggerHaptic()
                                            scoringViewModel.recordDelivery(data, runsScored = r)
                                        },
                                        modifier = Modifier.weight(1f).testTag("run_btn_$r")
                                    )
                                }
                            }

                            // Row 3: 4, 6, WICKET
                            val cricketColors = LocalCricketColors.current
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RunKeypadButton(
                                    label = "4",
                                    bg = cricketColors.boundaryFour,
                                    fg = Color.Black,
                                    onClick = {
                                        triggerHaptic()
                                        scoringViewModel.recordDelivery(data, runsScored = 4)
                                    },
                                    modifier = Modifier.weight(1f).testTag("run_btn_4")
                                )
                                RunKeypadButton(
                                    label = "6",
                                    bg = cricketColors.boundarySix,
                                    fg = Color.Black,
                                    onClick = {
                                        triggerHaptic()
                                        scoringViewModel.recordDelivery(data, runsScored = 6)
                                    },
                                    modifier = Modifier.weight(1f).testTag("run_btn_6")
                                )
                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        showWicketDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = cricketColors.wicket,
                                        contentColor = Color.White
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .height(54.dp)
                                        .testTag("wicket_btn")
                                ) {
                                    Text(
                                        "WICKET",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Dialogs
                if (pendingExtraType != null) {
                    ExtraDeliveryDialog(
                        extraType = pendingExtraType!!,
                        onDismiss = { pendingExtraType = null },
                        onConfirm = { batRuns, extraRuns ->
                            scoringViewModel.recordDelivery(
                                data = data,
                                runsScored = batRuns,
                                extraType = pendingExtraType!!,
                                extraRuns = extraRuns
                            )
                            pendingExtraType = null
                        }
                    )
                }

                if (showWicketDialog) {
                    val battingSquad = if ((active.inningsNumber == 1 && match.battingFirstTeam == "A") ||
                        (active.inningsNumber == 2 && match.battingFirstTeam == "B")
                    ) match.teamAPlayers else match.teamBPlayers
                    val bowlingSquad = if ((active.inningsNumber == 1 && match.battingFirstTeam == "A") ||
                        (active.inningsNumber == 2 && match.battingFirstTeam == "B")
                    ) match.teamBPlayers else match.teamAPlayers

                    WicketDialog(
                        striker = activeStriker,
                        nonStriker = activeNonStriker,
                        battingSquad = battingSquad,
                        bowlingSquad = bowlingSquad,
                        onDismiss = { showWicketDialog = false },
                        onConfirm = { dismissal, dismissedName, fielder, nextBatter, runsCompleted ->
                            scoringViewModel.recordDelivery(
                                data = data,
                                runsScored = runsCompleted,
                                wicket = dismissal.countsAsWicket,
                                dismissalType = dismissal,
                                dismissedPlayerName = dismissedName,
                                fielderName = fielder,
                                nextBatterName = nextBatter
                            )
                            showWicketDialog = false
                        }
                    )
                }

                if (showPlayerPicker != null) {
                    val role = showPlayerPicker!!
                    val battingSquad = if ((active.inningsNumber == 1 && match.battingFirstTeam == "A") ||
                        (active.inningsNumber == 2 && match.battingFirstTeam == "B")
                    ) match.teamAPlayers else match.teamBPlayers
                    val bowlingSquad = if ((active.inningsNumber == 1 && match.battingFirstTeam == "A") ||
                        (active.inningsNumber == 2 && match.battingFirstTeam == "B")
                    ) match.teamBPlayers else match.teamAPlayers
                    val list = if (role == "BOWLER") bowlingSquad else battingSquad

                    PlayerSelectDialog(
                        title = when (role) {
                            "STRIKER" -> "Select Striker"
                            "NON_STRIKER" -> "Select Non-Striker"
                            else -> "Select Bowler"
                        },
                        players = list,
                        onDismiss = { showPlayerPicker = null },
                        onSelect = { chosen ->
                            when (role) {
                                "STRIKER" -> scoringViewModel.setStriker(chosen)
                                "NON_STRIKER" -> scoringViewModel.setNonStriker(chosen)
                                else -> scoringViewModel.setBowler(chosen)
                            }
                            showPlayerPicker = null
                        }
                    )
                }

                if (editingBall != null) {
                    EditBallDialog(
                        ball = editingBall!!,
                        onDismiss = { editingBall = null },
                        onSave = { updated ->
                            scoringViewModel.editBall(data, updated)
                            editingBall = null
                        }
                    )
                }

                if (showMoreActions) {
                    AlertDialog(
                        onDismissRequest = { showMoreActions = false },
                        title = { Text("Match Actions") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (active.inningsNumber == 1) {
                                    Button(
                                        onClick = {
                                            showMoreActions = false
                                            scoringViewModel.startSecondInnings(data)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("End 1st Innings & Start 2nd") }
                                }
                                OutlinedButton(
                                    onClick = {
                                        showMoreActions = false
                                        scoringViewModel.completeMatch(data)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Complete Match & Calculate Result") }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showMoreActions = false }) { Text("Close") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PlayerTapPill(
    label: String,
    name: String,
    stat: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Text(stat, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(40.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun RunKeypadButton(
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.height(54.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtraDeliveryDialog(
    extraType: ExtraType,
    onDismiss: () -> Unit,
    onConfirm: (batRuns: Int, extraRuns: Int) -> Unit
) {
    var selectedRuns by remember { mutableIntStateOf(if (extraType == ExtraType.BYE || extraType == ExtraType.LEG_BYE) 1 else 0) }
    var runsOffBat by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record ${extraType.name.replace('_', ' ')}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select additional runs taken on this ball:", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..6).forEach { r ->
                        FilterChip(
                            selected = selectedRuns == r,
                            onClick = { selectedRuns = r },
                            label = { Text("+$r") }
                        )
                    }
                }
                if (extraType == ExtraType.NO_BALL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !runsOffBat,
                            onClick = { runsOffBat = false },
                            label = { Text("Byes / Penalty") }
                        )
                        FilterChip(
                            selected = runsOffBat,
                            onClick = { runsOffBat = true },
                            label = { Text("Off the Bat") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (extraType == ExtraType.NO_BALL && runsOffBat) {
                    onConfirm(selectedRuns, 0)
                } else {
                    onConfirm(0, selectedRuns)
                }
            }) {
                Text("Record")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WicketDialog(
    striker: String,
    nonStriker: String,
    battingSquad: List<String>,
    bowlingSquad: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (DismissalType, String, String?, String?, Int) -> Unit
) {
    var dismissalType by remember { mutableStateOf(DismissalType.CAUGHT) }
    var dismissedPlayer by remember { mutableStateOf(striker) }
    var fielder by remember { mutableStateOf(bowlingSquad.firstOrNull().orEmpty()) }
    var nextBatter by remember { mutableStateOf(battingSquad.firstOrNull { it != striker && it != nonStriker }.orEmpty()) }
    var runsCompleted by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Wicket / Retirement") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Dismissal Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DismissalType.entries.filter { it != DismissalType.NONE }.forEach { type ->
                        FilterChip(
                            selected = dismissalType == type,
                            onClick = { dismissalType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
                Text("Dismissed Batter", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = dismissedPlayer == striker, onClick = { dismissedPlayer = striker }, label = { Text(striker) })
                    FilterChip(selected = dismissedPlayer == nonStriker, onClick = { dismissedPlayer = nonStriker }, label = { Text(nonStriker) })
                }
                if (dismissalType == DismissalType.CAUGHT || dismissalType == DismissalType.RUN_OUT || dismissalType == DismissalType.STUMPED) {
                    OutlinedTextField(
                        value = fielder,
                        onValueChange = { fielder = it },
                        label = { Text("Fielder / Keeper") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (dismissalType == DismissalType.RUN_OUT) {
                    Text("Runs Completed Before Run Out: $runsCompleted", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..3).forEach { r ->
                            FilterChip(selected = runsCompleted == r, onClick = { runsCompleted = r }, label = { Text("$r") })
                        }
                    }
                }
                OutlinedTextField(
                    value = nextBatter,
                    onValueChange = { nextBatter = it },
                    label = { Text("Incoming Batter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        dismissalType,
                        dismissedPlayer,
                        fielder.takeIf { it.isNotBlank() },
                        nextBatter.takeIf { it.isNotBlank() },
                        runsCompleted
                    )
                },
                modifier = Modifier.testTag("confirm_wicket_button")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PlayerSelectDialog(
    title: String,
    players: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var customName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                players.forEach { p ->
                    OutlinedButton(
                        onClick = { onSelect(p) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(p)
                    }
                }
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Or enter player name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (customName.isNotBlank()) {
                Button(onClick = { onSelect(customName.trim()) }) { Text("Select") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditBallDialog(
    ball: BallEvent,
    onDismiss: () -> Unit,
    onSave: (BallEvent) -> Unit
) {
    var runs by remember { mutableIntStateOf(ball.runsScored) }
    var extraType by remember { mutableStateOf(ball.extraTypeEnum) }
    var wicket by remember { mutableStateOf(ball.wicket) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Ball ${ball.overNumber}.${ball.ballInOver}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Runs Scored", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 1, 2, 3, 4, 6).forEach { r ->
                        FilterChip(selected = runs == r, onClick = { runs = r }, label = { Text("$r") })
                    }
                }
                Text("Extra Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExtraType.entries.forEach { et ->
                        FilterChip(selected = extraType == et, onClick = { extraType = et }, label = { Text(et.name) })
                    }
                }
                FilterChip(
                    selected = wicket,
                    onClick = { wicket = !wicket },
                    label = { Text(if (wicket) "Wicket: YES" else "Wicket: NO") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    ball.copy(
                        runsScored = runs,
                        extraType = extraType.name,
                        wicket = wicket,
                        dismissalType = if (wicket && ball.dismissalType == DismissalType.NONE.name) DismissalType.CAUGHT.name else if (!wicket) DismissalType.NONE.name else ball.dismissalType
                    )
                )
            }) { Text("Save Ball") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun angleToZoneName(deg: Int): String = when (((deg + 22) % 360) / 45) {
    0 -> "Long-Off"
    1 -> "Cover"
    2 -> "Point"
    3 -> "Third Man"
    4 -> "Fine Leg"
    5 -> "Square Leg"
    6 -> "Mid-Wicket"
    else -> "Long-On"
}
