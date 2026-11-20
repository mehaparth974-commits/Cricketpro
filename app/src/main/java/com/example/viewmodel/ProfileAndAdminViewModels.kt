package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.CommunityTournamentRepository
import com.example.data.repository.MatchRepository
import com.example.domain.AppConfig
import com.example.domain.CricketMatch
import com.example.domain.ModerationReport
import com.example.domain.PlayerProfile
import com.example.domain.PrivateAccount
import com.example.domain.Team
import com.example.domain.Tournament
import com.example.domain.TournamentEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileScreenData(
    val profile: PlayerProfile,
    val privateAccount: PrivateAccount?,
    val myMatches: List<CricketMatch>,
    val isAdmin: Boolean,
    val earnedBadges: List<String>
)

data class AdminDashboardData(
    val players: List<PlayerProfile>,
    val teams: List<Team>,
    val tournaments: List<Tournament>,
    val matches: List<CricketMatch>,
    val reports: List<ModerationReport>,
    val appConfig: AppConfig?
)

class ProfileAdminViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepo = AuthRepository(application)
    private val matchRepo = MatchRepository(application)
    private val communityRepo = CommunityTournamentRepository(application)

    init {
        viewModelScope.launch {
            authRepo.ensurePlayerProfileInitialized()
        }
    }

    val profileState: StateFlow<UiState<ProfileScreenData>> = combine(
        authRepo.observeMyProfile(),
        authRepo.observeMyPrivateAccount(),
        matchRepo.observePublicMatches()
    ) { profile, priv, matches ->
        val p = profile ?: PlayerProfile(
            userId = authRepo.requireUserId(),
            displayName = "CricketPro Player"
        )
        val myMatches = matches.filter {
            it.scorerUid == p.userId ||
                it.teamAPlayers.any { name -> name.equals(p.displayName, ignoreCase = true) } ||
                it.teamBPlayers.any { name -> name.equals(p.displayName, ignoreCase = true) }
        }
        val isAdmin = authRepo.isBootstrappedAdmin() || p.role == "admin"
        val badges = TournamentEngine.evaluateEarnedBadges(p)
        ProfileScreenData(
            profile = p,
            privateAccount = priv,
            myMatches = myMatches,
            isAdmin = isAdmin,
            earnedBadges = badges
        )
    }.asUiState("Failed to load profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminState: StateFlow<UiState<AdminDashboardData>> = combine(
        communityRepo.observePublicPlayers(),
        matchRepo.observePublicTeams(),
        communityRepo.observeTournaments(),
        matchRepo.observePublicMatches(),
        communityRepo.observeReportsForAdmin(),
        communityRepo.observeAppConfig()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        AdminDashboardData(
            players = values[0] as List<PlayerProfile>,
            teams = values[1] as List<Team>,
            tournaments = values[2] as List<Tournament>,
            matches = values[3] as List<CricketMatch>,
            reports = values[4] as List<ModerationReport>,
            appConfig = values[5] as AppConfig?
        )
    }.asUiState("Admin permissions required or failed to load admin console")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun uploadAvatarAndSave(
        uri: Uri,
        onEncoded: (String) -> Unit
    ) {
        viewModelScope.launch {
            authRepo.encodeUriToCompressedBase64(uri)
                .onSuccess { encoded ->
                    onEncoded(encoded)
                    _message.value = "Photo ready — tap Save Profile to apply"
                }
                .onFailure {
                    _message.value = it.localizedMessage ?: "Failed to process image"
                }
        }
    }

    fun updateProfile(
        displayName: String,
        handle: String,
        battingStyle: String,
        bowlingStyle: String,
        playingRole: String,
        city: String,
        bio: String,
        privateStats: Boolean,
        photoData: String?,
        email: String,
        phone: String,
        notificationsEnabled: Boolean
    ) {
        viewModelScope.launch {
            authRepo.updateProfile(
                displayName = displayName,
                handle = handle,
                battingStyle = battingStyle,
                bowlingStyle = bowlingStyle,
                playingRole = playingRole,
                city = city,
                bio = bio,
                privateStats = privateStats,
                photoData = photoData,
                email = email,
                phone = phone,
                notificationsEnabled = notificationsEnabled
            ).onSuccess {
                _message.value = "Profile saved"
            }.onFailure {
                _message.value = it.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun resolveReport(report: ModerationReport, newStatus: String, hideTargetPost: Boolean) {
        viewModelScope.launch {
            communityRepo.adminResolveReport(report, newStatus, hideTargetPost)
                .onSuccess { _message.value = "Report marked $newStatus" }
                .onFailure { _message.value = it.localizedMessage ?: "Moderation action failed" }
        }
    }

    fun broadcastNotification(title: String, body: String) {
        viewModelScope.launch {
            if (title.isBlank() || body.isBlank()) {
                _message.value = "Title and message body are required"
                return@launch
            }
            communityRepo.adminBroadcastNotification(title, body)
                .onSuccess { _message.value = "Broadcast notification sent to all users" }
                .onFailure { _message.value = it.localizedMessage ?: "Broadcast failed" }
        }
    }

    fun updateAppConfig(
        maintenanceMode: Boolean,
        announcementBanner: String,
        defaultOvers: Int,
        allowCommunityPosts: Boolean
    ) {
        viewModelScope.launch {
            communityRepo.adminUpdateAppConfig(
                maintenanceMode = maintenanceMode,
                announcementBanner = announcementBanner,
                defaultOvers = defaultOvers,
                allowCommunityPosts = allowCommunityPosts
            ).onSuccess { _message.value = "App settings updated" }
                .onFailure { _message.value = it.localizedMessage ?: "Failed to update settings" }
        }
    }

    fun toggleUserRole(targetUid: String, currentRole: String) {
        viewModelScope.launch {
            val nextRole = if (currentRole == "admin") "user" else "admin"
            communityRepo.adminUpdateUserRole(targetUid, nextRole)
                .onSuccess { _message.value = "Updated user role to $nextRole" }
                .onFailure { _message.value = it.localizedMessage ?: "Role update failed" }
        }
    }

    fun deleteEntity(collection: String, docId: String, label: String) {
        viewModelScope.launch {
            communityRepo.adminDeleteEntity(collection, docId)
                .onSuccess { _message.value = "$label removed" }
                .onFailure { _message.value = it.localizedMessage ?: "Delete failed" }
        }
    }
}
