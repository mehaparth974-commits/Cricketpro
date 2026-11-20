package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AiInsightsRepository
import com.example.data.repository.AiMatchInsights
import com.example.data.repository.CommunityTournamentRepository
import com.example.data.repository.MatchRepository
import com.example.domain.AppConfig
import com.example.domain.BallEvent
import com.example.domain.CricketMatch
import com.example.domain.CricketScoringEngine
import com.example.domain.DismissalType
import com.example.domain.ExtraType
import com.example.domain.MatchComputedState
import com.example.domain.PostType
import com.example.domain.Team
import com.example.domain.Tournament
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HubData(
    val matches: List<CricketMatch>,
    val teams: List<Team>,
    val tournaments: List<Tournament>,
    val pendingSyncCount: Int,
    val appConfig: AppConfig?
)

class HubViewModel(application: Application) : AndroidViewModel(application) {
    private val matchRepo = MatchRepository(application)
    private val communityRepo = CommunityTournamentRepository(application)

    val hubState: StateFlow<UiState<HubData>> = combine(
        matchRepo.observePublicMatches(),
        matchRepo.observePublicTeams(),
        communityRepo.observeTournaments(),
        matchRepo.observePendingSyncCount(),
        communityRepo.observeAppConfig()
    ) { matches, teams, tournaments, pendingSync, config ->
        HubData(
            matches = matches,
            teams = teams,
            tournaments = tournaments,
            pendingSyncCount = pendingSync,
            appConfig = config
        )
    }.asUiState("Failed to load CricketPro hub")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun createTeam(
        name: String,
        shortName: String,
        city: String,
        captainName: String,
        playersCsv: String,
        logoData: String?,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val players = playersCsv.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
            matchRepo.createTeam(name, shortName, city, captainName, players, logoData)
                .onSuccess { teamId ->
                    _actionMessage.value = "Team $name created"
                    onCreated(teamId)
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to create team" }
        }
    }

    fun createQuickMatch(
        tournamentId: String?,
        tournamentName: String?,
        teamA: Team,
        teamB: Team,
        oversLimit: Int,
        powerplayOvers: Int,
        groundName: String,
        tossWinner: String,
        tossDecision: String,
        onMatchCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            matchRepo.createMatch(
                tournamentId = tournamentId,
                tournamentName = tournamentName,
                teamA = teamA,
                teamB = teamB,
                oversLimit = oversLimit,
                powerplayOvers = powerplayOvers,
                groundName = groundName,
                tossWinner = tossWinner,
                tossDecision = tossDecision,
                startImmediately = true
            ).onSuccess { matchId ->
                onMatchCreated(matchId)
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to create match"
            }
        }
    }
}

data class ActiveScoringData(
    val match: CricketMatch,
    val balls: List<BallEvent>,
    val computed: MatchComputedState,
    val pendingSyncCount: Int
)

class ScoringViewModel(
    application: Application,
    private val matchId: String
) : AndroidViewModel(application) {
    private val matchRepo = MatchRepository(application)
    private val communityRepo = CommunityTournamentRepository(application)
    private val aiRepo = AiInsightsRepository()

    val scoringState: StateFlow<UiState<ActiveScoringData>> = combine(
        matchRepo.observeMatch(matchId),
        matchRepo.observeMatchBalls(matchId),
        matchRepo.observePendingSyncCount()
    ) { match, balls, pendingCount ->
        if (match == null) throw IllegalStateException("Match not found")
        val computed = CricketScoringEngine.computeMatch(match, balls)
        ActiveScoringData(match, balls, computed, pendingCount)
    }.asUiState("Failed to load match")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _selectedStriker = MutableStateFlow<String?>(null)
    val selectedStriker: StateFlow<String?> = _selectedStriker.asStateFlow()

    private val _selectedNonStriker = MutableStateFlow<String?>(null)
    val selectedNonStriker: StateFlow<String?> = _selectedNonStriker.asStateFlow()

    private val _selectedBowler = MutableStateFlow<String?>(null)
    val selectedBowler: StateFlow<String?> = _selectedBowler.asStateFlow()

    private val _shotAngleDeg = MutableStateFlow(45)
    val shotAngleDeg: StateFlow<Int> = _shotAngleDeg.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _aiState = MutableStateFlow<UiState<AiMatchInsights>?>(null)
    val aiState: StateFlow<UiState<AiMatchInsights>?> = _aiState.asStateFlow()

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun setShotAngle(angleDeg: Int) {
        _shotAngleDeg.value = angleDeg.coerceIn(0, 360)
    }

    fun setStriker(name: String) {
        _selectedStriker.value = name
    }

    fun setNonStriker(name: String) {
        _selectedNonStriker.value = name
    }

    fun setBowler(name: String) {
        _selectedBowler.value = name
    }

    fun swapStrike(computed: MatchComputedState) {
        val currentS = _selectedStriker.value ?: computed.activeInnings.currentStriker
        val currentNs = _selectedNonStriker.value ?: computed.activeInnings.currentNonStriker
        _selectedStriker.value = currentNs
        _selectedNonStriker.value = currentS
    }

    fun recordDelivery(
        data: ActiveScoringData,
        runsScored: Int,
        extraType: ExtraType = ExtraType.NONE,
        extraRuns: Int = 0,
        wicket: Boolean = false,
        dismissalType: DismissalType = DismissalType.NONE,
        dismissedPlayerName: String? = null,
        fielderName: String? = null,
        nextBatterName: String? = null
    ) {
        viewModelScope.launch {
            val active = data.computed.activeInnings
            val striker = _selectedStriker.value ?: active.currentStriker
            val nonStriker = _selectedNonStriker.value ?: active.currentNonStriker
            val bowler = _selectedBowler.value ?: active.currentBowler

            val legalBallsSoFar = active.legalBalls
            val overNum = legalBallsSoFar / 6
            val ballInOver = (legalBallsSoFar % 6) + 1
            val nextSeq = (data.balls.maxOfOrNull { it.sequenceIndex } ?: -1) + 1

            val commentary = CricketScoringEngine.generateAutoCommentary(
                striker = striker,
                bowler = bowler,
                runsScored = runsScored,
                extraType = extraType,
                extraRuns = extraRuns,
                wicket = wicket || dismissalType == DismissalType.RETIRED_HURT,
                dismissalType = dismissalType,
                fielder = fielderName
            )

            val ball = BallEvent(
                matchId = data.match.id,
                sequenceIndex = nextSeq,
                innings = active.inningsNumber,
                overNumber = overNum,
                ballInOver = ballInOver.coerceIn(1, 6),
                strikerName = striker,
                nonStrikerName = nonStriker,
                bowlerName = bowler,
                runsScored = runsScored,
                extraType = extraType.name,
                extraRuns = extraRuns,
                wicket = wicket,
                dismissalType = dismissalType.name,
                dismissedPlayerName = dismissedPlayerName,
                fielderName = fielderName,
                shotAngleDeg = _shotAngleDeg.value,
                shotDistanceNorm = when {
                    runsScored == 6 -> 1.0
                    runsScored == 4 -> 0.92
                    runsScored >= 1 -> 0.55
                    else -> 0.2
                },
                powerplay = active.powerplayActive,
                commentary = commentary
            )

            matchRepo.recordBallAndSyncMatch(data.match, data.balls, ball)
                .onSuccess {
                    // Reset manual overrides so computed strike rotation takes effect cleanly,
                    // unless a replacement batter was picked
                    val simulated = CricketScoringEngine.computeMatch(data.match, data.balls + ball)
                    val nextInnings = simulated.activeInnings
                    if (!nextBatterName.isNullOrBlank()) {
                        if (dismissedPlayerName == nonStriker) {
                            _selectedStriker.value = nextInnings.currentStriker
                            _selectedNonStriker.value = nextBatterName
                        } else {
                            _selectedStriker.value = nextBatterName
                            _selectedNonStriker.value = nextInnings.currentNonStriker
                        }
                    } else {
                        _selectedStriker.value = null
                        _selectedNonStriker.value = null
                    }
                    // Prompt new bowler automatically on over completion
                    if (ball.isLegalDelivery && (legalBallsSoFar + 1) % 6 == 0) {
                        _statusBanner.value = "Over ${overNum + 1} complete! Select next bowler if needed."
                    }
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Failed to save ball"
                }
        }
    }

    fun undoLastBall(data: ActiveScoringData) {
        viewModelScope.launch {
            matchRepo.undoLastBall(data.match, data.balls)
                .onSuccess {
                    _selectedStriker.value = null
                    _selectedNonStriker.value = null
                    _statusBanner.value = "Last ball undone"
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Undo failed" }
        }
    }

    fun editBall(data: ActiveScoringData, editedBall: BallEvent) {
        viewModelScope.launch {
            matchRepo.editBallEvent(data.match, data.balls, editedBall)
                .onSuccess { _statusBanner.value = "Ball updated and scorecard recalculated" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Edit failed" }
        }
    }

    fun startSecondInnings(data: ActiveScoringData) {
        viewModelScope.launch {
            _selectedStriker.value = null
            _selectedNonStriker.value = null
            _selectedBowler.value = null
            matchRepo.startSecondInnings(data.match, data.balls)
                .onSuccess { _statusBanner.value = "2nd Innings started!" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not start 2nd innings" }
        }
    }

    fun completeMatch(data: ActiveScoringData) {
        viewModelScope.launch {
            matchRepo.completeMatchAndRollupStats(data.match, data.balls)
                .onSuccess { _statusBanner.value = "Match completed & records updated!" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to complete match" }
        }
    }

    fun saveStreamConfig(matchId: String, streamUrl: String, rtmpServer: String, rtmpKey: String) {
        viewModelScope.launch {
            matchRepo.saveStreamConfig(matchId, streamUrl, rtmpServer, rtmpKey)
                .onSuccess { _statusBanner.value = "Broadcast stream configuration saved" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to save stream settings" }
        }
    }

    fun generateAiSummary(data: ActiveScoringData) {
        viewModelScope.launch {
            _aiState.value = UiState.Loading
            try {
                aiRepo.generateMatchInsights(data.match, data.computed)
                    .onSuccess { insights ->
                        _aiState.value = UiState.Success(insights)
                        val formatted = "${insights.headline}\n\n${insights.summaryParagraph}\n\nTurning Point: ${insights.turningPoint}\nTactical Insight: ${insights.tacticalRecommendation}"
                        matchRepo.saveAiSummary(data.match.id, formatted)
                    }
                    .onFailure { err ->
                        _aiState.value = UiState.Error(err.localizedMessage ?: "AI analysis failed")
                    }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun shareMatchToCommunity(authorName: String, authorPhoto: String?, data: ActiveScoringData) {
        viewModelScope.launch {
            val inn1 = data.computed.innings1
            val inn2 = data.computed.innings2
            val headline = "${data.match.teamAShort} vs ${data.match.teamBShort} — ${data.computed.resultText}"
            val summary = "${inn1.battingTeamName}: ${inn1.totalRuns}/${inn1.wickets} (${inn1.oversDisplay} ov) | ${inn2.battingTeamName}: ${inn2.totalRuns}/${inn2.wickets} (${inn2.oversDisplay} ov). ${data.computed.resultText}"
            communityRepo.createPost(
                authorName = authorName,
                authorPhoto = authorPhoto,
                content = summary,
                postType = PostType.MATCH_RESULT,
                matchId = data.match.id,
                matchHeadline = headline
            ).onSuccess {
                _statusBanner.value = "Match result shared to Cricket Community!"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to share match"
            }
        }
    }
}
