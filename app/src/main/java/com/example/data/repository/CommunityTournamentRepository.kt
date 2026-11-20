package com.example.data.repository

import android.content.Context
import com.example.data.firebase.FirestoreProvider
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.domain.AppConfig
import com.example.domain.AppNotification
import com.example.domain.CommunityPost
import com.example.domain.FollowRecord
import com.example.domain.Ground
import com.example.domain.GroundPitchType
import com.example.domain.ModerationReport
import com.example.domain.PlayerProfile
import com.example.domain.PostComment
import com.example.domain.PostType
import com.example.domain.Team
import com.example.domain.Tournament
import com.example.domain.TournamentEngine
import com.example.domain.TournamentFormat
import com.example.domain.TournamentStanding
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CommunityTournamentRepository(context: Context) {
    private val db: FirebaseFirestore = FirestoreProvider.getDb(context)
    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing community/tournaments.")
    }

    fun observeTournaments(): Flow<List<Tournament>> = flow {
        val uid = requireUserId()
        val path = "tournaments"
        emitAll(
            db.collection(path)
                .whereEqualTo("publicVisible", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeStandings(tournamentId: String): Flow<List<TournamentStanding>> = flow {
        val uid = requireUserId()
        val path = "tournaments/$tournamentId/standings"
        emitAll(
            db.collection("tournaments").document(tournamentId).collection("standings")
                .whereEqualTo("publicVisible", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TournamentStanding::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<TournamentStanding> { it.points }.thenByDescending { it.nrr })
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createTournamentWithFixtures(
        name: String,
        format: TournamentFormat,
        oversPerMatch: Int,
        powerplayOvers: Int,
        teams: List<Team>,
        rulesText: String,
        organizerName: String,
        matchRepository: MatchRepository
    ): Result<String> {
        val cleanName = name.trim().take(80)
        if (cleanName.length < 2 || teams.size < 2) {
            return Result.failure(IllegalArgumentException("Tournament name and at least 2 teams are required."))
        }
        var docPath = "tournaments"
        return try {
            val uid = requireUserId()
            val safeOvers = oversPerMatch.coerceIn(1, 50)
            val safePp = powerplayOvers.coerceIn(0, minOf(15, safeOvers))
            val tourRef = db.collection("tournaments").document()
            docPath = tourRef.path

            val payload = mapOf(
                "name" to cleanName,
                "format" to format.name,
                "oversPerMatch" to safeOvers,
                "powerplayOvers" to safePp,
                "organizerUid" to uid,
                "organizerName" to organizerName.ifBlank { "Organizer" }.take(60),
                "teamIds" to teams.map { it.id }.take(32),
                "teamNames" to teams.map { it.name.take(60) }.take(32),
                "rulesText" to rulesText.trim().take(1000),
                "bannerTheme" to "EMERALD",
                "status" to "ONGOING",
                "publicVisible" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            tourRef.set(payload).await()

            // Initialize standings rows
            val mid = (teams.size + 1) / 2
            teams.forEachIndexed { idx, team ->
                val group = if (format == TournamentFormat.GROUP_STAGE) {
                    if (idx < mid) "Group A" else "Group B"
                } else null
                val standingPayload = mutableMapOf<String, Any?>(
                    "tournamentId" to tourRef.id,
                    "teamId" to team.id,
                    "teamName" to team.name.take(60),
                    "groupName" to group,
                    "organizerUid" to uid,
                    "played" to 0,
                    "won" to 0,
                    "lost" to 0,
                    "tied" to 0,
                    "noResult" to 0,
                    "points" to 0,
                    "runsFor" to 0,
                    "ballsFaced" to 0,
                    "runsAgainst" to 0,
                    "ballsBowled" to 0,
                    "nrr" to 0.0,
                    "publicVisible" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                tourRef.collection("standings").document(team.id).set(standingPayload).await()
            }

            // Generate fixtures as UPCOMING matches
            val fixtures = TournamentEngine.generateFixtures(format, teams.map { it.id to it.name })
            val teamById = teams.associateBy { it.id }
            for (fixture in fixtures.take(20)) {
                val tA = teamById[fixture.teamAId] ?: continue
                val tB = teamById[fixture.teamBId] ?: continue
                matchRepository.createMatch(
                    tournamentId = tourRef.id,
                    tournamentName = cleanName,
                    teamA = tA,
                    teamB = tB,
                    oversLimit = safeOvers,
                    powerplayOvers = safePp,
                    groundName = fixture.roundLabel,
                    tossWinner = tA.name,
                    tossDecision = "BAT",
                    startImmediately = false
                )
            }
            Result.success(tourRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observePosts(): Flow<List<CommunityPost>> = flow {
        val uid = requireUserId()
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("hidden", false)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeComments(postId: String): Flow<List<PostComment>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createPost(
        authorName: String,
        authorPhoto: String?,
        content: String,
        postType: PostType,
        matchId: String? = null,
        matchHeadline: String? = null
    ): Result<Unit> {
        val cleanContent = content.trim().take(1000)
        if (cleanContent.isEmpty()) return Result.failure(IllegalArgumentException("Post content cannot be empty."))
        var docPath = "posts"
        return try {
            val uid = requireUserId()
            val ref = db.collection("posts").document()
            docPath = ref.path
            val payload = mutableMapOf<String, Any?>(
                "authorUid" to uid,
                "authorName" to authorName.ifBlank { "Player" }.take(60),
                "authorPhoto" to authorPhoto?.take(64000),
                "content" to cleanContent,
                "postType" to postType.name,
                "matchId" to matchId?.take(128),
                "matchHeadline" to matchHeadline?.take(160),
                "likesCount" to 0,
                "commentsCount" to 0,
                "hidden" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(post: CommunityPost): Result<Unit> {
        val path = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            val likeRef = db.collection("posts").document(post.id).collection("likes").document(uid)
            val snap = likeRef.get().await()
            if (snap.exists()) {
                likeRef.delete().await()
                val newCount = (post.likesCount - 1).coerceAtLeast(0)
                db.collection("posts").document(post.id).update(
                    mapOf("likesCount" to newCount, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            } else {
                likeRef.set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
                db.collection("posts").document(post.id).update(
                    mapOf("likesCount" to post.likesCount + 1, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(post: CommunityPost, authorName: String, text: String): Result<Unit> {
        val clean = text.trim().take(400)
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val path = "posts/${post.id}/comments"
        return try {
            val uid = requireUserId()
            val commentRef = db.collection("posts").document(post.id).collection("comments").document()
            commentRef.set(
                mapOf(
                    "postId" to post.id,
                    "authorUid" to uid,
                    "authorName" to authorName.ifBlank { "Player" }.take(60),
                    "text" to clean,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("posts").document(post.id).update(
                mapOf("commentsCount" to post.commentsCount + 1, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observePublicPlayers(): Flow<List<PlayerProfile>> = flow {
        val uid = requireUserId()
        val path = "players"
        emitAll(
            db.collection(path)
                .whereEqualTo("privateStats", false)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.mvpPoints }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeGrounds(): Flow<List<Ground>> = flow {
        val uid = requireUserId()
        val path = "grounds"
        emitAll(
            db.collection(path)
                .whereEqualTo("publicVisible", true)
                .snapshots()
                .map { it.toObjects(Ground::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addGround(name: String, city: String, pitchType: GroundPitchType, floodlights: Boolean): Result<Unit> {
        val cleanName = name.trim().take(80)
        val cleanCity = city.trim().take(60)
        if (cleanName.length < 2 || cleanCity.length < 2) {
            return Result.failure(IllegalArgumentException("Ground name and city are required."))
        }
        var path = "grounds"
        return try {
            val uid = requireUserId()
            val ref = db.collection("grounds").document()
            path = ref.path
            ref.set(
                mapOf(
                    "name" to cleanName,
                    "city" to cleanCity,
                    "pitchType" to pitchType.name,
                    "floodlights" to floodlights,
                    "publicVisible" to true,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeMyFollows(): Flow<List<FollowRecord>> = flow {
        val uid = requireUserId()
        val path = "follows"
        emitAll(
            db.collection(path)
                .whereEqualTo("followerUid", uid)
                .snapshots()
                .map { it.toObjects(FollowRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun toggleFollow(targetId: String, targetType: String, targetName: String): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docId = "${uid}_${targetType}_${targetId}".take(128)
        val path = "follows/$docId"
        return try {
            val ref = db.collection("follows").document(docId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.delete().await()
                Result.success(false)
            } else {
                ref.set(
                    mapOf(
                        "followerUid" to uid,
                        "targetId" to targetId.take(128),
                        "targetType" to targetType,
                        "targetName" to targetName.take(80),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeNotifications(): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        val path = "notifications"
        val personal = db.collection(path)
            .whereEqualTo("recipientUid", uid)
            .snapshots()
            .map { it.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val broadcasts = db.collection(path)
            .whereEqualTo("recipientUid", "ALL")
            .snapshots()
            .map { it.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        emitAll(
            combine(personal, broadcasts) { p, b ->
                (p + b).distinctBy { it.id }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }.catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                throw error
            }
        )
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val path = "notifications/$notificationId"
        return try {
            requireUserId()
            db.collection("notifications").document(notificationId)
                .update(mapOf("read" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        reporterName: String,
        targetType: String,
        targetId: String,
        reason: String
    ): Result<Unit> {
        val cleanReason = reason.trim().take(300)
        if (cleanReason.length < 3) return Result.failure(IllegalArgumentException("Reason must be at least 3 characters."))
        var path = "reports"
        return try {
            val uid = requireUserId()
            val ref = db.collection("reports").document()
            path = ref.path
            ref.set(
                mapOf(
                    "reporterUid" to uid,
                    "reporterName" to reporterName.ifBlank { "User" }.take(60),
                    "targetType" to targetType,
                    "targetId" to targetId.take(128),
                    "reason" to cleanReason,
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeReportsForAdmin(): Flow<List<ModerationReport>> = flow {
        val uid = requireUserId()
        val path = "reports"
        emitAll(
            db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAppConfig(): Flow<AppConfig?> = flow {
        val uid = requireUserId()
        val path = "app_config/global"
        emitAll(
            db.collection("app_config").document("global")
                .snapshots()
                .map { it.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.GET, path)
                    throw error
                }
        )
    }

    // Admin Actions
    suspend fun adminResolveReport(report: ModerationReport, newStatus: String, hideTargetPost: Boolean): Result<Unit> {
        val path = "reports/${report.id}"
        return try {
            requireUserId()
            db.collection("reports").document(report.id).update(
                mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            if (hideTargetPost && report.targetType == "POST" && report.targetId.isNotBlank()) {
                db.collection("posts").document(report.targetId).update(
                    mapOf("hidden" to true, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminBroadcastNotification(title: String, body: String): Result<Unit> {
        var path = "notifications"
        return try {
            requireUserId()
            val ref = db.collection("notifications").document()
            path = ref.path
            ref.set(
                mapOf(
                    "recipientUid" to "ALL",
                    "title" to title.trim().take(100),
                    "body" to body.trim().take(300),
                    "type" to "SYSTEM",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateAppConfig(
        maintenanceMode: Boolean,
        announcementBanner: String,
        defaultOvers: Int,
        allowCommunityPosts: Boolean
    ): Result<Unit> {
        val path = "app_config/global"
        return try {
            val uid = requireUserId()
            db.collection("app_config").document("global").set(
                mapOf(
                    "maintenanceMode" to maintenanceMode,
                    "announcementBanner" to announcementBanner.trim().take(200),
                    "defaultOvers" to defaultOvers.coerceIn(1, 50),
                    "allowCommunityPosts" to allowCommunityPosts,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserRole(targetUid: String, newRole: String): Result<Unit> {
        val path = "players/$targetUid"
        return try {
            requireUserId()
            db.collection("players").document(targetUid).update(
                mapOf("role" to if (newRole == "admin") "admin" else "user", "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            if (newRole == "admin") {
                db.collection("admins").document(targetUid).set(
                    mapOf("uid" to targetUid, "createdAt" to FieldValue.serverTimestamp())
                ).await()
            } else {
                db.collection("admins").document(targetUid).delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteEntity(collection: String, docId: String): Result<Unit> {
        val path = "$collection/$docId"
        return try {
            requireUserId()
            db.collection(collection).document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
