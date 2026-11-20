package com.example.domain

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ExtraType { NONE, WIDE, NO_BALL, BYE, LEG_BYE }

enum class DismissalType(val label: String, val creditsBowler: Boolean, val countsAsWicket: Boolean) {
    NONE("Not Out", false, false),
    BOWLED("Bowled", true, true),
    CAUGHT("Caught", true, true),
    LBW("LBW", true, true),
    RUN_OUT("Run Out", false, true),
    STUMPED("Stumped", true, true),
    HIT_WICKET("Hit Wicket", true, true),
    RETIRED_HURT("Retired Hurt", false, false),
    RETIRED_OUT("Retired Out", false, true)
}

enum class MatchStatus { UPCOMING, LIVE, INNINGS_BREAK, COMPLETED }

enum class TournamentFormat(val label: String) {
    ROUND_ROBIN("Round-Robin"),
    KNOCKOUT("Knockout"),
    LEAGUE("League"),
    GROUP_STAGE("Group Stage + Knockouts")
}

enum class PostType(val label: String) {
    UPDATE("Update"),
    MATCH_RESULT("Match Result"),
    NEWS("Cricket News"),
    HIGHLIGHT("Highlight")
}

enum class GroundPitchType(val label: String) {
    TURF("Turf"),
    MATTING("Matting"),
    CEMENT("Cement"),
    ASTRO("AstroTurf")
}

inline fun <reified T : Enum<T>> safeEnum(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
}

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val photoData: String? = null,
    val battingStyle: String = "Right-Hand Bat",
    val bowlingStyle: String = "Right-Arm Medium",
    val playingRole: String = "All-Rounder",
    val city: String? = null,
    val bio: String? = null,
    val privateStats: Boolean = false,
    val role: String = "user",
    val followersCount: Int = 0,
    val matchesPlayed: Int = 0,
    val runsScored: Int = 0,
    val ballsFaced: Int = 0,
    val highestScore: Int = 0,
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val wicketsTaken: Int = 0,
    val runsConceded: Int = 0,
    val ballsBowled: Int = 0,
    val bestBowlingWickets: Int = 0,
    val bestBowlingRuns: Int = 0,
    val catches: Int = 0,
    val stumpings: Int = 0,
    val runOuts: Int = 0,
    val mvpPoints: Double = 0.0,
    val badges: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val battingAverage: Double
        get() = if (matchesPlayed > 0) runsScored.toDouble() / matchesPlayed else 0.0

    val strikeRate: Double
        get() = if (ballsFaced > 0) (runsScored * 100.0) / ballsFaced else 0.0

    val bowlingEconomy: Double
        get() = if (ballsBowled > 0) (runsConceded * 6.0) / ballsBowled else 0.0

    val bestBowlingText: String
        get() = if (bestBowlingWickets == 0 && bestBowlingRuns == 0) "-" else "$bestBowlingWickets/$bestBowlingRuns"
}

@Keep
@IgnoreExtraProperties
data class PrivateAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String? = null,
    val phone: String? = null,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Team(
    @DocumentId val id: String = "",
    val name: String = "",
    val shortName: String = "",
    val city: String? = null,
    val logoData: String? = null,
    val ownerUid: String = "",
    val captainName: String = "",
    val playerNames: List<String> = emptyList(),
    val wins: Int = 0,
    val losses: Int = 0,
    val ties: Int = 0,
    val publicVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val name: String = "",
    val format: String = TournamentFormat.ROUND_ROBIN.name,
    val oversPerMatch: Int = 20,
    val powerplayOvers: Int = 6,
    val organizerUid: String = "",
    val organizerName: String = "",
    val teamIds: List<String> = emptyList(),
    val teamNames: List<String> = emptyList(),
    val rulesText: String? = null,
    val bannerTheme: String? = "EMERALD",
    val status: String = "ONGOING",
    val winnerTeamName: String? = null,
    val publicVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val formatEnum: TournamentFormat
        get() = safeEnum(format, TournamentFormat.ROUND_ROBIN)
}

@Keep
@IgnoreExtraProperties
data class TournamentStanding(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val groupName: String? = null,
    val organizerUid: String = "",
    val played: Int = 0,
    val won: Int = 0,
    val lost: Int = 0,
    val tied: Int = 0,
    val noResult: Int = 0,
    val points: Int = 0,
    val runsFor: Int = 0,
    val ballsFaced: Int = 0,
    val runsAgainst: Int = 0,
    val ballsBowled: Int = 0,
    val nrr: Double = 0.0,
    val publicVisible: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CricketMatch(
    @DocumentId val id: String = "",
    val tournamentId: String? = null,
    val tournamentName: String? = null,
    val teamAId: String = "",
    val teamBId: String = "",
    val teamAName: String = "",
    val teamBName: String = "",
    val teamAShort: String = "",
    val teamBShort: String = "",
    val teamAPlayers: List<String> = emptyList(),
    val teamBPlayers: List<String> = emptyList(),
    val oversLimit: Int = 20,
    val powerplayOvers: Int = 6,
    val groundName: String = "",
    val tossWinner: String = "",
    val tossDecision: String = "BAT",
    val battingFirstTeam: String = "A",
    val status: String = MatchStatus.UPCOMING.name,
    val currentInnings: Int = 1,
    val teamARuns: Int = 0,
    val teamAWickets: Int = 0,
    val teamABalls: Int = 0,
    val teamBRuns: Int = 0,
    val teamBWickets: Int = 0,
    val teamBBalls: Int = 0,
    val targetRuns: Int = 0,
    val resultText: String = "",
    val mvpPlayerName: String? = null,
    val streamUrl: String? = null,
    val rtmpServer: String? = null,
    val rtmpKey: String? = null,
    val aiSummary: String? = null,
    val scorerUid: String = "",
    val publicVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val matchStatus: MatchStatus
        get() = safeEnum(status, MatchStatus.UPCOMING)

    fun formatOvers(balls: Int): String = "${balls / 6}.${balls % 6}"
}

@Keep
@IgnoreExtraProperties
data class BallEvent(
    @DocumentId val id: String = "",
    val matchId: String = "",
    val scorerUid: String = "",
    val publicVisible: Boolean = true,
    val sequenceIndex: Int = 0,
    val innings: Int = 1,
    val overNumber: Int = 0,
    val ballInOver: Int = 1,
    val strikerName: String = "",
    val nonStrikerName: String = "",
    val bowlerName: String = "",
    val runsScored: Int = 0,
    val extraType: String = ExtraType.NONE.name,
    val extraRuns: Int = 0,
    val wicket: Boolean = false,
    val dismissalType: String = DismissalType.NONE.name,
    val dismissedPlayerName: String? = null,
    val fielderName: String? = null,
    val shotAngleDeg: Int = 0,
    val shotDistanceNorm: Double = 0.0,
    val powerplay: Boolean = false,
    val commentary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val extraTypeEnum: ExtraType
        get() = safeEnum(extraType, ExtraType.NONE)

    val dismissalTypeEnum: DismissalType
        get() = safeEnum(dismissalType, DismissalType.NONE)

    val isLegalDelivery: Boolean
        get() = extraTypeEnum != ExtraType.WIDE && extraTypeEnum != ExtraType.NO_BALL

    val totalBallRuns: Int
        get() = when (extraTypeEnum) {
            ExtraType.NONE -> runsScored
            ExtraType.WIDE -> 1 + extraRuns
            ExtraType.NO_BALL -> 1 + runsScored + extraRuns
            ExtraType.BYE, ExtraType.LEG_BYE -> extraRuns
        }

    val badgeLabel: String
        get() = when {
            wicket -> "W"
            extraTypeEnum == ExtraType.WIDE -> if (extraRuns > 0) "${1 + extraRuns}wd" else "WD"
            extraTypeEnum == ExtraType.NO_BALL -> if (runsScored > 0) "${runsScored}nb" else "NB"
            extraTypeEnum == ExtraType.BYE -> "${extraRuns}b"
            extraTypeEnum == ExtraType.LEG_BYE -> "${extraRuns}lb"
            else -> runsScored.toString()
        }
}

@Keep
@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorPhoto: String? = null,
    val content: String = "",
    val postType: String = PostType.UPDATE.name,
    val matchId: String? = null,
    val matchHeadline: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val hidden: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val postTypeEnum: PostType
        get() = safeEnum(postType, PostType.UPDATE)
}

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Ground(
    @DocumentId val id: String = "",
    val name: String = "",
    val city: String = "",
    val pitchType: String = GroundPitchType.TURF.name,
    val floodlights: Boolean = false,
    val publicVisible: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowRecord(
    @DocumentId val id: String = "",
    val followerUid: String = "",
    val targetId: String = "",
    val targetType: String = "PLAYER",
    val targetName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val reporterName: String = "",
    val targetType: String = "POST",
    val targetId: String = "",
    val reason: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientUid: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "SYSTEM",
    val referenceId: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "global",
    val maintenanceMode: Boolean = false,
    val announcementBanner: String = "",
    val defaultOvers: Int = 20,
    val allowCommunityPosts: Boolean = true,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
