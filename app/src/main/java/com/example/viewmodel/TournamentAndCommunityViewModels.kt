package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CommunityTournamentRepository
import com.example.data.repository.MatchRepository
import com.example.domain.AppNotification
import com.example.domain.CommunityPost
import com.example.domain.CricketMatch
import com.example.domain.FollowRecord
import com.example.domain.Ground
import com.example.domain.GroundPitchType
import com.example.domain.PlayerProfile
import com.example.domain.PostComment
import com.example.domain.PostType
import com.example.domain.Team
import com.example.domain.Tournament
import com.example.domain.TournamentFormat
import com.example.domain.TournamentStanding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TournamentsScreenData(
    val tournaments: List<Tournament>,
    val teams: List<Team>,
    val matches: List<CricketMatch>
)

data class TournamentDetailData(
    val tournament: Tournament,
    val standings: List<TournamentStanding>,
    val fixtures: List<CricketMatch>
)

class TournamentViewModel(application: Application) : AndroidViewModel(application) {
    private val matchRepo = MatchRepository(application)
    private val repo = CommunityTournamentRepository(application)

    val tournamentsState: StateFlow<UiState<TournamentsScreenData>> = combine(
        repo.observeTournaments(),
        matchRepo.observePublicTeams(),
        matchRepo.observePublicMatches()
    ) { tournaments, teams, matches ->
        TournamentsScreenData(tournaments, teams, matches)
    }.asUiState("Failed to load tournaments")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun observeTournamentDetail(tournamentId: String): StateFlow<UiState<TournamentDetailData>> {
        return combine(
            repo.observeTournaments(),
            repo.observeStandings(tournamentId),
            matchRepo.observePublicMatches()
        ) { tournaments, standings, matches ->
            val t = tournaments.firstOrNull { it.id == tournamentId }
                ?: throw IllegalStateException("Tournament not found")
            val tMatches = matches.filter { it.tournamentId == tournamentId }
            TournamentDetailData(t, standings, tMatches)
        }.asUiState("Failed to load tournament details")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    fun createTournament(
        name: String,
        format: TournamentFormat,
        oversPerMatch: Int,
        powerplayOvers: Int,
        selectedTeams: List<Team>,
        rulesText: String,
        organizerName: String,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repo.createTournamentWithFixtures(
                name = name,
                format = format,
                oversPerMatch = oversPerMatch,
                powerplayOvers = powerplayOvers,
                teams = selectedTeams,
                rulesText = rulesText,
                organizerName = organizerName,
                matchRepository = matchRepo
            ).onSuccess { id ->
                _message.value = "Tournament & automatic fixtures generated!"
                onCreated(id)
            }.onFailure {
                _message.value = it.localizedMessage ?: "Failed to create tournament"
            }
        }
    }
}

data class CommunityData(
    val posts: List<CommunityPost>,
    val players: List<PlayerProfile>,
    val teams: List<Team>,
    val grounds: List<Ground>,
    val follows: List<FollowRecord>,
    val notifications: List<AppNotification>
)

class CommunityViewModel(application: Application) : AndroidViewModel(application) {
    private val matchRepo = MatchRepository(application)
    private val repo = CommunityTournamentRepository(application)

    val communityState: StateFlow<UiState<CommunityData>> = combine(
        repo.observePosts(),
        repo.observePublicPlayers(),
        matchRepo.observePublicTeams(),
        repo.observeGrounds(),
        repo.observeMyFollows(),
        repo.observeNotifications()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CommunityData(
            posts = values[0] as List<CommunityPost>,
            players = values[1] as List<PlayerProfile>,
            teams = values[2] as List<Team>,
            grounds = values[3] as List<Ground>,
            follows = values[4] as List<FollowRecord>,
            notifications = values[5] as List<AppNotification>
        )
    }.asUiState("Failed to load community")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearMessage() {
        _message.value = null
    }

    fun observeComments(postId: String): StateFlow<UiState<List<PostComment>>> {
        return repo.observeComments(postId)
            .asUiState("Failed to load comments")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    fun createPost(authorName: String, authorPhoto: String?, content: String, postType: PostType) {
        viewModelScope.launch {
            repo.createPost(authorName, authorPhoto, content, postType)
                .onSuccess { _message.value = "Post published" }
                .onFailure { _message.value = it.localizedMessage ?: "Failed to publish post" }
        }
    }

    fun toggleLike(post: CommunityPost) {
        viewModelScope.launch {
            repo.toggleLike(post)
                .onFailure { _message.value = it.localizedMessage ?: "Could not update like" }
        }
    }

    fun addComment(post: CommunityPost, authorName: String, text: String) {
        viewModelScope.launch {
            repo.addComment(post, authorName, text)
                .onFailure { _message.value = it.localizedMessage ?: "Could not add comment" }
        }
    }

    fun toggleFollow(targetId: String, targetType: String, targetName: String) {
        viewModelScope.launch {
            repo.toggleFollow(targetId, targetType, targetName)
                .onSuccess { nowFollowing ->
                    _message.value = if (nowFollowing) "Following $targetName" else "Unfollowed $targetName"
                }
                .onFailure { _message.value = it.localizedMessage ?: "Follow update failed" }
        }
    }

    fun addGround(name: String, city: String, pitchType: GroundPitchType, floodlights: Boolean) {
        viewModelScope.launch {
            repo.addGround(name, city, pitchType, floodlights)
                .onSuccess { _message.value = "Ground $name added to directory" }
                .onFailure { _message.value = it.localizedMessage ?: "Failed to add ground" }
        }
    }

    fun submitReport(reporterName: String, targetType: String, targetId: String, reason: String) {
        viewModelScope.launch {
            repo.submitReport(reporterName, targetType, targetId, reason)
                .onSuccess { _message.value = "Report submitted for Admin review" }
                .onFailure { _message.value = it.localizedMessage ?: "Failed to submit report" }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repo.markNotificationRead(notificationId)
        }
    }
}
