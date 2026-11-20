package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.ExportUtils
import com.example.domain.InningsComputedState
import com.example.domain.MatchStatus
import com.example.ui.components.CricketBallChip
import com.example.ui.components.CricketErrorCard
import com.example.ui.components.CricketLoadingCard
import com.example.ui.components.LivePulsePill
import com.example.ui.components.ManhattanChartCanvas
import com.example.ui.components.PartnershipBarsCard
import com.example.ui.components.RunRateGraphCanvas
import com.example.ui.components.WagonWheelCanvas
import com.example.ui.components.WormGraphCanvas
import com.example.viewmodel.ScoringViewModel
import com.example.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MatchCenterScreen(
    scoringViewModel: ScoringViewModel,
    currentUserId: String,
    currentUserName: String,
    currentUserPhoto: String?,
    initialTab: Int = 0,
    onBack: () -> Unit,
    onOpenLiveScorer: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val state by scoringViewModel.scoringState.collectAsStateWithLifecycle()
    val aiState by scoringViewModel.aiState.collectAsStateWithLifecycle()
    val statusBanner by scoringViewModel.statusBanner.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    val tabs = listOf("Summary", "Scorecard", "Analytics", "Commentary", "Broadcast")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val matchTitle = (state as? UiState.Success)?.data?.match?.let {
                        "${it.teamAShort} vs ${it.teamBShort}"
                    } ?: "Match Center"
                    Text(matchTitle)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("match_center_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val data = (state as? UiState.Success)?.data
                    if (data != null && data.match.scorerUid == currentUserId && data.match.matchStatus != MatchStatus.COMPLETED) {
                        FilledTonalButton(
                            onClick = onOpenLiveScorer,
                            modifier = Modifier.padding(end = 8.dp).testTag("center_score_live_btn")
                        ) {
                            Icon(Icons.Default.SportsCricket, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Score")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val ui = state) {
            is UiState.Loading -> Box(Modifier.padding(innerPadding)) { CricketLoadingCard("Loading Match Center...") }
            is UiState.Error -> Box(Modifier.padding(innerPadding).padding(16.dp)) { CricketErrorCard(ui.message) }
            is UiState.Success -> {
                val data = ui.data
                val match = data.match
                val computed = data.computed

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (statusBanner != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusBanner!!,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 12.dp
                    ) {
                        tabs.forEachIndexed { idx, label ->
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                text = { Text(label) },
                                modifier = Modifier.testTag("match_tab_${label.lowercase()}")
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // SUMMARY & AI INSIGHTS TAB
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(match.groundName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (computed.matchStatus == MatchStatus.LIVE) LivePulsePill("LIVE")
                                        }
                                        Text(
                                            "${computed.innings1.battingTeamName}: ${computed.innings1.totalRuns}/${computed.innings1.wickets} (${computed.innings1.oversDisplay})",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        if (computed.innings2.legalBalls > 0 || computed.innings2.batters.isNotEmpty()) {
                                            Text(
                                                "${computed.innings2.battingTeamName}: ${computed.innings2.totalRuns}/${computed.innings2.wickets} (${computed.innings2.oversDisplay})",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                        Text(
                                            computed.resultText,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Export & Share Actions
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { ExportUtils.exportMatchPdfAndShare(context, match, computed) },
                                        modifier = Modifier.testTag("export_pdf_button")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("PDF Scorecard")
                                    }
                                    FilledTonalButton(
                                        onClick = { ExportUtils.exportMatchCsvAndShare(context, match, data.balls, computed) },
                                        modifier = Modifier.testTag("export_csv_button")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Export CSV")
                                    }
                                    OutlinedButton(
                                        onClick = { scoringViewModel.shareMatchToCommunity(currentUserName, currentUserPhoto, data) },
                                        modifier = Modifier.testTag("share_community_button")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Post Result")
                                    }
                                }

                                // MVP Leaderboard
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                            Text("Match MVP Standings", style = MaterialTheme.typography.titleMedium)
                                        }
                                        if (computed.mvpLeaderboard.isEmpty()) {
                                            Text("MVP points appear once play begins.", style = MaterialTheme.typography.bodySmall)
                                        } else {
                                            computed.mvpLeaderboard.take(5).forEachIndexed { idx, mvp ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "${idx + 1}. ${mvp.playerName}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Text(
                                                        "${String.format(Locale.US, "%.1f", mvp.points)} pts (${mvp.runs}r, ${mvp.wickets}w)",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Gemini AI Insights Card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Text("AI Match Summary & Insights", style = MaterialTheme.typography.titleMedium)
                                            }
                                        }

                                        if (!match.aiSummary.isNullOrBlank() && aiState == null) {
                                            Text(match.aiSummary, style = MaterialTheme.typography.bodyMedium)
                                        }

                                        when (val ai = aiState) {
                                            is UiState.Loading -> {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                                                    Text("Analyzing ball-by-ball telemetry...", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                            is UiState.Error -> {
                                                Text(ai.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                            }
                                            is UiState.Success -> {
                                                val ins = ai.data
                                                Text(ins.headline, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                                Text(ins.summaryParagraph, style = MaterialTheme.typography.bodyMedium)
                                                Text("Turning Point: ${ins.turningPoint}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                Text("Tactical Note: ${ins.tacticalRecommendation}", style = MaterialTheme.typography.bodySmall)
                                                if (ins.highlightMoments.isNotEmpty()) {
                                                    Text("Key Highlight Moments:", style = MaterialTheme.typography.labelLarge)
                                                    ins.highlightMoments.forEach { m ->
                                                        Text("• $m", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                            }
                                            null -> Unit
                                        }

                                        Button(
                                            onClick = { scoringViewModel.generateAiSummary(data) },
                                            modifier = Modifier.fillMaxWidth().testTag("generate_ai_summary_button")
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (match.aiSummary.isNullOrBlank()) "Generate AI Match Insights" else "Refresh AI Insights")
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // SCORECARD TAB
                                InningsScorecardSection(computed.innings1)
                                if (computed.innings2.legalBalls > 0 || computed.innings2.batters.isNotEmpty()) {
                                    InningsScorecardSection(computed.innings2)
                                }
                            }

                            2 -> {
                                // ADVANCED ANALYTICS TAB
                                var innTab by remember { mutableIntStateOf(1) }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = innTab == 1,
                                        onClick = { innTab = 1 },
                                        label = { Text(computed.innings1.battingTeamName) }
                                    )
                                    if (computed.innings2.legalBalls > 0) {
                                        FilterChip(
                                            selected = innTab == 2,
                                            onClick = { innTab = 2 },
                                            label = { Text(computed.innings2.battingTeamName) }
                                        )
                                    }
                                }
                                val targetInn = if (innTab == 2 && computed.innings2.legalBalls > 0) computed.innings2 else computed.innings1
                                val innBalls = data.balls.filter { it.innings == targetInn.inningsNumber }

                                // Dot% & Boundary% KPIs
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MetricPillCard("Dot Ball %", "${targetInn.dotBallPct}%", Modifier.weight(1f))
                                    MetricPillCard("Boundary %", "${targetInn.boundaryPct}%", Modifier.weight(1f))
                                    MetricPillCard("Extras", "${targetInn.totalExtras}", Modifier.weight(1f))
                                }

                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Wagon Wheel", style = MaterialTheme.typography.titleMedium)
                                        WagonWheelCanvas(balls = innBalls)
                                    }
                                }

                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Manhattan Chart (Runs per Over)", style = MaterialTheme.typography.titleMedium)
                                        ManhattanChartCanvas(overs = targetInn.overSummaries)
                                    }
                                }

                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Worm Graph (Cumulative Score)", style = MaterialTheme.typography.titleMedium)
                                        WormGraphCanvas(
                                            inn1Overs = computed.innings1.overSummaries,
                                            inn2Overs = computed.innings2.overSummaries,
                                            team1Label = computed.innings1.battingTeamName,
                                            team2Label = computed.innings2.battingTeamName
                                        )
                                    }
                                }

                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Run-Rate Graph", style = MaterialTheme.typography.titleMedium)
                                        RunRateGraphCanvas(
                                            inn1Overs = computed.innings1.overSummaries,
                                            inn2Overs = computed.innings2.overSummaries,
                                            team1Label = computed.innings1.battingTeamName,
                                            team2Label = computed.innings2.battingTeamName
                                        )
                                    }
                                }

                                PartnershipBarsCard(partnerships = targetInn.partnerships)

                                // Player Comparison
                                val allBatters = (computed.innings1.batters + computed.innings2.batters).distinctBy { it.name }
                                if (allBatters.size >= 2) {
                                    PlayerComparisonCard(allBatters)
                                }
                            }

                            3 -> {
                                // LIVE COMMENTARY TAB
                                var filter by remember { mutableStateOf("ALL") }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("ALL", "4s", "6s", "WICKETS").forEach { f ->
                                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
                                    }
                                }
                                val filteredBalls = data.balls.sortedByDescending { it.sequenceIndex }.filter { b ->
                                    when (filter) {
                                        "4s" -> b.runsScored == 4
                                        "6s" -> b.runsScored == 6
                                        "WICKETS" -> b.wicket
                                        else -> true
                                    }
                                }
                                if (filteredBalls.isEmpty()) {
                                    Text("No matching commentary events yet.", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    filteredBalls.forEach { ball ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                CricketBallChip(ball)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "Inn ${ball.innings} • ${ball.overNumber}.${ball.ballInOver} • ${ball.bowlerName} to ${ball.strikerName}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(ball.commentary, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                // LIVE BROADCAST & STREAMING TAB
                                BroadcastStudioTab(
                                    match = match,
                                    computed = computed,
                                    isScorer = match.scorerUid == currentUserId,
                                    onSaveStreamConfig = { url, rtmp, key ->
                                        scoringViewModel.saveStreamConfig(match.id, url, rtmp, key)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InningsScorecardSection(inn: InningsComputedState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(inn.battingTeamName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("${inn.totalRuns}/${inn.wickets} (${inn.oversDisplay})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Batter", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2.2f))
                Text("R", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.6f))
                Text("B", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.6f))
                Text("4s", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f))
                Text("6s", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f))
                Text("SR", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.8f))
            }
            inn.batters.forEach { b ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(2.2f)) {
                        Text(b.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(b.dismissalText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${b.runs}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                    Text("${b.balls}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.6f))
                    Text("${b.fours}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f))
                    Text("${b.sixes}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f))
                    Text(String.format(Locale.US, "%.0f", b.strikeRate), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.8f))
                }
            }
            Text(
                "Extras: ${inn.totalExtras} (wd ${inn.wides}, nb ${inn.noBalls}, b ${inn.byes}, lb ${inn.legByes})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (inn.fallOfWickets.isNotEmpty()) {
                Text(
                    "Fall of Wickets: " + inn.fallOfWickets.joinToString(" • ") { "${it.scoreAtWicket}-${it.wicketNumber} (${it.batterName}, ${it.overDisplay})" },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Bowler", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2.2f))
                Text("O", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.6f))
                Text("M", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f))
                Text("R", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.6f))
                Text("W", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f))
                Text("ECON", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.8f))
            }
            inn.bowlers.forEach { bw ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(bw.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(2.2f))
                    Text(bw.oversDisplay, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.6f))
                    Text("${bw.maidens}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f))
                    Text("${bw.runsConceded}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.6f))
                    Text("${bw.wickets}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                    Text(String.format(Locale.US, "%.1f", bw.economy), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.8f))
                }
            }
        }
    }
}

@Composable
private fun MetricPillCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlayerComparisonCard(batters: List<com.example.domain.BatterLine>) {
    var idxA by remember { mutableIntStateOf(0) }
    var idxB by remember { mutableIntStateOf(1.coerceAtMost(batters.lastIndex)) }
    val pA = batters[idxA]
    val pB = batters[idxB]

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Player Comparison", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { idxA = (idxA + 1) % batters.size }) { Text(pA.name) }
                Text("VS", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.CenterVertically))
                OutlinedButton(onClick = { idxB = (idxB + 1) % batters.size }) { Text(pB.name) }
            }
            CompareStatRow("Runs", "${pA.runs}", "${pB.runs}")
            CompareStatRow("Balls", "${pA.balls}", "${pB.balls}")
            CompareStatRow("Strike Rate", String.format(Locale.US, "%.1f", pA.strikeRate), String.format(Locale.US, "%.1f", pB.strikeRate))
            CompareStatRow("Boundaries", "${pA.fours}x4 / ${pA.sixes}x6", "${pB.fours}x4 / ${pB.sixes}x6")
        }
    }
}

@Composable
private fun CompareStatRow(label: String, a: String, b: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(a, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(b, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
    }
}
