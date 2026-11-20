package com.example.data.repository

import android.content.Context
import com.example.data.firebase.FirestoreProvider
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.local.LocalCacheDatabase
import com.example.data.local.PendingBallSyncEntity
import com.example.domain.BallEvent
import com.example.domain.CricketMatch
import com.example.domain.CricketScoringEngine
import com.example.domain.DismissalType
import com.example.domain.ExtraType
import com.example.domain.MatchStatus
import com.example.domain.Team
import com.example.domain.TournamentEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MatchRepository(context: Context) {
    private val db: FirebaseFirestore = FirestoreProvider.getDb(context)
    private val auth = Firebase.auth
    private val cacheDao = LocalCacheDatabase.getInstance(context).localCacheDao()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing matches.")
    }

    fun observePendingSyncCount(): Flow<Int> = cacheDao.observePendingCount()

    fun observePublicMatches(): Flow<List<CricketMatch>> = flow {
        val uid = requireUserId()
        val path = "matches"
        emitAll(
            db.collection(path)
                .whereEqualTo("publicVisible", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CricketMatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMatch(matchId: String): Flow<CricketMatch?> = flow {
        val uid = requireUserId()
        val path = "matches/$matchId"
        emitAll(
            db.collection("matches").document(matchId)
                .snapshots()
                .map { it.toObject(CricketMatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeMatchBalls(matchId: String): Flow<List<BallEvent>> = flow {
        val uid = requireUserId()
        val path = "matches/$matchId/balls"
        emitAll(
            db.collection("matches").document(matchId).collection("balls")
                .whereEqualTo("publicVisible", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BallEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sequenceIndex }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicTeams(): Flow<List<Team>> = flow {
        val uid = requireUserId()
        val path = "teams"
        emitAll(
            db.collection(path)
                .whereEqualTo("publicVisible", true)
                .snapshots()
                .map { snap -> snap.toObjects(Team::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createTeam(
        name: String,
        shortName: String,
        city: String,
        captainName: String,
        playerNames: List<String>,
        logoData: String?
    ): Result<String> {
        val cleanName = name.trim().take(60)
        val cleanShort = shortName.trim().uppercase().take(8)
        if (cleanName.length < 2 || cleanShort.length < 2 || captainName.isBlank()) {
            return Result.failure(IllegalArgumentException("Team name, short code, and captain are required."))
        }
        var docPath = "teams"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("teams").document()
            docPath = docRef.path
            val roster = playerNames.map { it.trim().take(60) }.filter { it.isNotBlank() }.distinct().take(25)
                .ifEmpty { listOf(captainName.trim().take(60)) }
            val payload = mutableMapOf<String, Any?>(
                "name" to cleanName,
                "shortName" to cleanShort,
                "city" to city.trim().take(60),
                "logoData" to logoData?.take(64000),
                "ownerUid" to uid,
                "captainName" to captainName.trim().take(60),
                "playerNames" to roster,
                "wins" to 0,
                "losses" to 0,
                "ties" to 0,
                "publicVisible" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createMatch(
        tournamentId: String?,
        tournamentName: String?,
        teamA: Team,
        teamB: Team,
        oversLimit: Int,
        powerplayOvers: Int,
        groundName: String,
        tossWinner: String,
        tossDecision: String,
        startImmediately: Boolean = true
    ): Result<String> {
        var docPath = "matches"
        return try {
            val uid = requireUserId()
            val safeOvers = oversLimit.coerceIn(1, 50)
            val safePp = powerplayOvers.coerceIn(0, minOf(15, safeOvers))
            val teamABatsFirst = (tossWinner == teamA.name && tossDecision == "BAT") ||
                (tossWinner == teamB.name && tossDecision == "BOWL")
            val battingFirst = if (teamABatsFirst) "A" else "B"

            val docRef = db.collection("matches").document()
            docPath = docRef.path
            val payload = mutableMapOf<String, Any?>(
                "tournamentId" to tournamentId?.takeIf { it.isNotBlank() }?.take(128),
                "tournamentName" to tournamentName?.takeIf { it.isNotBlank() }?.take(80),
                "teamAId" to teamA.id.ifBlank { "team_a" }.take(128),
                "teamBId" to teamB.id.ifBlank { "team_b" }.take(128),
                "teamAName" to teamA.name.take(60),
                "teamBName" to teamB.name.take(60),
                "teamAShort" to teamA.shortName.take(8),
                "teamBShort" to teamB.shortName.take(8),
                "teamAPlayers" to teamA.playerNames.take(15),
                "teamBPlayers" to teamB.playerNames.take(15),
                "oversLimit" to safeOvers,
                "powerplayOvers" to safePp,
                "groundName" to groundName.trim().ifBlank { "Central Stadium" }.take(80),
                "tossWinner" to tossWinner.take(60),
                "tossDecision" to if (tossDecision == "BOWL") "BOWL" else "BAT",
                "battingFirstTeam" to battingFirst,
                "status" to if (startImmediately) MatchStatus.LIVE.name else MatchStatus.UPCOMING.name,
                "currentInnings" to 1,
                "teamARuns" to 0,
                "teamAWickets" to 0,
                "teamABalls" to 0,
                "teamBRuns" to 0,
                "teamBWickets" to 0,
                "teamBBalls" to 0,
                "targetRuns" to 0,
                "resultText" to "Toss: $tossWinner chose to ${tossDecision.lowercase()}",
                "scorerUid" to uid,
                "publicVisible" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordBallAndSyncMatch(
        match: CricketMatch,
        existingBalls: List<BallEvent>,
        newBall: BallEvent
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ballDocRef = db.collection("matches").document(match.id)
            .collection("balls").document(newBall.id.ifBlank { "ball_${newBall.sequenceIndex}_${System.currentTimeMillis()}" })
        val matchPath = "matches/${match.id}"
        return try {
            // Queue into Room outbox first for offline safety
            cacheDao.upsertPendingBall(
                PendingBallSyncEntity(
                    ballId = ballDocRef.id,
                    matchId = match.id,
                    sequenceIndex = newBall.sequenceIndex,
                    innings = newBall.innings,
                    overNumber = newBall.overNumber,
                    ballInOver = newBall.ballInOver,
                    strikerName = newBall.strikerName,
                    nonStrikerName = newBall.nonStrikerName,
                    bowlerName = newBall.bowlerName,
                    runsScored = newBall.runsScored,
                    extraType = newBall.extraType,
                    extraRuns = newBall.extraRuns,
                    wicket = newBall.wicket,
                    dismissalType = newBall.dismissalType,
                    dismissedPlayerName = newBall.dismissedPlayerName,
                    fielderName = newBall.fielderName,
                    shotAngleDeg = newBall.shotAngleDeg,
                    shotDistanceNorm = newBall.shotDistanceNorm,
                    powerplay = newBall.powerplay,
                    commentary = newBall.commentary,
                    scorerUid = uid,
                    createdAtMillis = System.currentTimeMillis()
                )
            )

            val ballPayload = mutableMapOf<String, Any?>(
                "matchId" to match.id,
                "scorerUid" to uid,
                "publicVisible" to true,
                "sequenceIndex" to newBall.sequenceIndex,
                "innings" to newBall.innings,
                "overNumber" to newBall.overNumber,
                "ballInOver" to newBall.ballInOver.coerceIn(1, 6),
                "strikerName" to newBall.strikerName.take(60),
                "nonStrikerName" to newBall.nonStrikerName.take(60),
                "bowlerName" to newBall.bowlerName.take(60),
                "runsScored" to newBall.runsScored.coerceIn(0, 7),
                "extraType" to newBall.extraType,
                "extraRuns" to newBall.extraRuns.coerceIn(0, 7),
                "wicket" to newBall.wicket,
                "dismissalType" to newBall.dismissalType,
                "dismissedPlayerName" to newBall.dismissedPlayerName?.take(60),
                "fielderName" to newBall.fielderName?.take(60),
                "shotAngleDeg" to newBall.shotAngleDeg.coerceIn(0, 360),
                "shotDistanceNorm" to newBall.shotDistanceNorm.coerceIn(0.0, 1.0),
                "powerplay" to newBall.powerplay,
                "commentary" to newBall.commentary.take(240),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            ballDocRef.set(ballPayload).await()
            cacheDao.deletePendingBall(ballDocRef.id)

            val updatedList = existingBalls + newBall.copy(id = ballDocRef.id)
            syncMatchSummaryFromBalls(match, updatedList)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, matchPath)
            Result.failure(e)
        }
    }

    suspend fun undoLastBall(
        match: CricketMatch,
        existingBalls: List<BallEvent>
    ): Result<Unit> {
        val lastBall = existingBalls.maxByOrNull { it.sequenceIndex }
            ?: return Result.failure(IllegalStateException("No balls to undo"))
        val path = "matches/${match.id}/balls/${lastBall.id}"
        return try {
            requireUserId()
            db.collection("matches").document(match.id)
                .collection("balls").document(lastBall.id)
                .delete().await()
            cacheDao.deletePendingBall(lastBall.id)
            val remaining = existingBalls.filterNot { it.id == lastBall.id }
            syncMatchSummaryFromBalls(match, remaining)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun editBallEvent(
        match: CricketMatch,
        existingBalls: List<BallEvent>,
        editedBall: BallEvent
    ): Result<Unit> {
        val path = "matches/${match.id}/balls/${editedBall.id}"
        return try {
            requireUserId()
            val updateMap = mutableMapOf<String, Any?>(
                "runsScored" to editedBall.runsScored.coerceIn(0, 7),
                "extraType" to editedBall.extraType,
                "extraRuns" to editedBall.extraRuns.coerceIn(0, 7),
                "wicket" to editedBall.wicket,
                "dismissalType" to editedBall.dismissalType,
                "dismissedPlayerName" to editedBall.dismissedPlayerName?.take(60),
                "fielderName" to editedBall.fielderName?.take(60),
                "commentary" to editedBall.commentary.take(240)
            ).filterValues { it != null }
            db.collection("matches").document(match.id)
                .collection("balls").document(editedBall.id)
                .update(updateMap).await()
            val updatedList = existingBalls.map { if (it.id == editedBall.id) editedBall else it }
            syncMatchSummaryFromBalls(match, updatedList)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun startSecondInnings(match: CricketMatch, balls: List<BallEvent>): Result<Unit> {
        val path = "matches/${match.id}"
        return try {
            requireUserId()
            val computed = CricketScoringEngine.computeMatch(match, balls)
            val target = computed.innings1.totalRuns + 1
            db.collection("matches").document(match.id).update(
                mapOf(
                    "currentInnings" to 2,
                    "status" to MatchStatus.LIVE.name,
                    "targetRuns" to target,
                    "resultText" to "${computed.innings2.battingTeamName} need $target runs to win",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun completeMatchAndRollupStats(
        match: CricketMatch,
        balls: List<BallEvent>
    ): Result<Unit> {
        val path = "matches/${match.id}"
        return try {
            val uid = requireUserId()
            val computed = CricketScoringEngine.computeMatch(match.copy(status = MatchStatus.COMPLETED.name), balls)
            val mvp = computed.mvpLeaderboard.firstOrNull()?.playerName
            val inn1 = computed.innings1
            val inn2 = computed.innings2
            val teamAFirst = match.battingFirstTeam == "A"
            val teamAState = if (teamAFirst) inn1 else inn2
            val teamBState = if (teamAFirst) inn2 else inn1

            val updatePayload = mutableMapOf<String, Any?>(
                "status" to MatchStatus.COMPLETED.name,
                "teamARuns" to teamAState.totalRuns,
                "teamAWickets" to teamAState.wickets,
                "teamABalls" to teamAState.legalBalls,
                "teamBRuns" to teamBState.totalRuns,
                "teamBWickets" to teamBState.wickets,
                "teamBBalls" to teamBState.legalBalls,
                "targetRuns" to inn2.target,
                "resultText" to computed.resultText.take(160),
                "mvpPlayerName" to mvp?.take(60),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("matches").document(match.id).update(updatePayload).await()

            // Rollup tournament standings & NRR if part of a tournament
            val tourId = match.tournamentId
            if (!tourId.isNullOrBlank()) {
                updateTournamentStanding(tourId, match.teamAId, match.teamAName, teamAState.totalRuns, teamAState.legalBalls, teamAState.wickets, teamBState.totalRuns, teamBState.legalBalls, teamBState.wickets, match.oversLimit)
                updateTournamentStanding(tourId, match.teamBId, match.teamBName, teamBState.totalRuns, teamBState.legalBalls, teamBState.wickets, teamAState.totalRuns, teamAState.legalBalls, teamAState.wickets, match.oversLimit)
            }

            // Rollup scorer's player profile career stats from matching display name if present
            rollupCurrentPlayerCareer(uid, computed)

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun updateTournamentStanding(
        tournamentId: String,
        teamId: String,
        teamName: String,
        runsFor: Int,
        ballsFaced: Int,
        wicketsLost: Int,
        runsAgainst: Int,
        ballsBowled: Int,
        wicketsTaken: Int,
        oversLimit: Int
    ) {
        if (teamId.isBlank()) return
        val ref = db.collection("tournaments").document(tournamentId).collection("standings").document(teamId)
        val snap = ref.get().await()
        if (!snap.exists()) return
        val data = snap.data ?: return

        val effectiveBallsFaced = if (wicketsLost >= 10) oversLimit * 6 else ballsFaced
        val effectiveBallsBowled = if (wicketsTaken >= 10) oversLimit * 6 else ballsBowled

        val prevPlayed = (data["played"] as? Number)?.toInt() ?: 0
        val prevWon = (data["won"] as? Number)?.toInt() ?: 0
        val prevLost = (data["lost"] as? Number)?.toInt() ?: 0
        val prevTied = (data["tied"] as? Number)?.toInt() ?: 0
        val prevPts = (data["points"] as? Number)?.toInt() ?: 0
        val prevRf = (data["runsFor"] as? Number)?.toInt() ?: 0
        val prevBf = (data["ballsFaced"] as? Number)?.toInt() ?: 0
        val prevRa = (data["runsAgainst"] as? Number)?.toInt() ?: 0
        val prevBb = (data["ballsBowled"] as? Number)?.toInt() ?: 0

        val wonInc = if (runsFor > runsAgainst) 1 else 0
        val lostInc = if (runsFor < runsAgainst) 1 else 0
        val tiedInc = if (runsFor == runsAgainst) 1 else 0
        val ptsInc = when {
            wonInc == 1 -> 2
            tiedInc == 1 -> 1
            else -> 0
        }

        val newRf = prevRf + runsFor
        val newBf = prevBf + effectiveBallsFaced
        val newRa = prevRa + runsAgainst
        val newBb = prevBb + effectiveBallsBowled
        val newNrr = TournamentEngine.calculateNrr(newRf, newBf, newRa, newBb)

        ref.update(
            mapOf(
                "played" to prevPlayed + 1,
                "won" to prevWon + wonInc,
                "lost" to prevLost + lostInc,
                "tied" to prevTied + tiedInc,
                "points" to prevPts + ptsInc,
                "runsFor" to newRf,
                "ballsFaced" to newBf,
                "runsAgainst" to newRa,
                "ballsBowled" to newBb,
                "nrr" to newNrr,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private suspend fun rollupCurrentPlayerCareer(uid: String, computed: com.example.domain.MatchComputedState) {
        val playerRef = db.collection("players").document(uid)
        val snap = playerRef.get().await()
        if (!snap.exists()) return
        val data = snap.data ?: return
        val myName = (data["displayName"] as? String).orEmpty()

        val myBat = (computed.innings1.batters + computed.innings2.batters)
            .firstOrNull { it.name.equals(myName, ignoreCase = true) }
        val myBowl = (computed.innings1.bowlers + computed.innings2.bowlers)
            .firstOrNull { it.name.equals(myName, ignoreCase = true) }
        val myMvp = computed.mvpLeaderboard
            .firstOrNull { it.playerName.equals(myName, ignoreCase = true) }

        if (myBat == null && myBowl == null) return

        val prevMatches = (data["matchesPlayed"] as? Number)?.toInt() ?: 0
        val prevRuns = (data["runsScored"] as? Number)?.toInt() ?: 0
        val prevBalls = (data["ballsFaced"] as? Number)?.toInt() ?: 0
        val prevHs = (data["highestScore"] as? Number)?.toInt() ?: 0
        val prev50s = (data["fifties"] as? Number)?.toInt() ?: 0
        val prev100s = (data["hundreds"] as? Number)?.toInt() ?: 0
        val prev4s = (data["fours"] as? Number)?.toInt() ?: 0
        val prev6s = (data["sixes"] as? Number)?.toInt() ?: 0
        val prevWkts = (data["wicketsTaken"] as? Number)?.toInt() ?: 0
        val prevConc = (data["runsConceded"] as? Number)?.toInt() ?: 0
        val prevBowled = (data["ballsBowled"] as? Number)?.toInt() ?: 0
        val prevBestW = (data["bestBowlingWickets"] as? Number)?.toInt() ?: 0
        val prevBestR = (data["bestBowlingRuns"] as? Number)?.toInt() ?: 0
        val prevMvp = (data["mvpPoints"] as? Number)?.toDouble() ?: 0.0

        val addRuns = myBat?.runs ?: 0
        val addWkts = myBowl?.wickets ?: 0
        val addConc = myBowl?.runsConceded ?: 0
        val isBetterBest = addWkts > prevBestW || (addWkts == prevBestW && addWkts > 0 && addConc < prevBestR)

        playerRef.update(
            mapOf(
                "matchesPlayed" to prevMatches + 1,
                "runsScored" to prevRuns + addRuns,
                "ballsFaced" to prevBalls + (myBat?.balls ?: 0),
                "highestScore" to maxOf(prevHs, addRuns),
                "fifties" to prev50s + if (addRuns in 50..99) 1 else 0,
                "hundreds" to prev100s + if (addRuns >= 100) 1 else 0,
                "fours" to prev4s + (myBat?.fours ?: 0),
                "sixes" to prev6s + (myBat?.sixes ?: 0),
                "wicketsTaken" to prevWkts + addWkts,
                "runsConceded" to prevConc + addConc,
                "ballsBowled" to prevBowled + (myBowl?.legalBalls ?: 0),
                "bestBowlingWickets" to if (isBetterBest) addWkts else prevBestW,
                "bestBowlingRuns" to if (isBetterBest) addConc else prevBestR,
                "mvpPoints" to prevMvp + (myMvp?.points ?: 0.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private suspend fun syncMatchSummaryFromBalls(match: CricketMatch, balls: List<BallEvent>) {
        val computed = CricketScoringEngine.computeMatch(match, balls)
        val teamAFirst = match.battingFirstTeam == "A"
        val teamAState = if (teamAFirst) computed.innings1 else computed.innings2
        val teamBState = if (teamAFirst) computed.innings2 else computed.innings1
        val nextStatus = if (computed.matchStatus == MatchStatus.COMPLETED) MatchStatus.LIVE.name else computed.matchStatus.name

        db.collection("matches").document(match.id).update(
            mapOf(
                "status" to nextStatus,
                "currentInnings" to computed.activeInnings.inningsNumber,
                "teamARuns" to teamAState.totalRuns,
                "teamAWickets" to teamAState.wickets,
                "teamABalls" to teamAState.legalBalls,
                "teamBRuns" to teamBState.totalRuns,
                "teamBWickets" to teamBState.wickets,
                "teamBBalls" to teamBState.legalBalls,
                "targetRuns" to computed.innings2.target,
                "resultText" to computed.resultText.take(160),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun saveStreamConfig(
        matchId: String,
        streamUrl: String,
        rtmpServer: String,
        rtmpKey: String
    ): Result<Unit> {
        val path = "matches/$matchId"
        return try {
            requireUserId()
            db.collection("matches").document(matchId).update(
                mapOf(
                    "streamUrl" to streamUrl.trim().take(500),
                    "rtmpServer" to rtmpServer.trim().take(300),
                    "rtmpKey" to rtmpKey.trim().take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAiSummary(matchId: String, summary: String): Result<Unit> {
        val path = "matches/$matchId"
        return try {
            requireUserId()
            db.collection("matches").document(matchId).update(
                mapOf(
                    "aiSummary" to summary.take(3000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
