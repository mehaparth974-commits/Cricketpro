package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.firebase.FirestoreProvider
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.domain.PlayerProfile
import com.example.domain.PrivateAccount
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AuthRepository(private val context: Context) {
    private val db: FirebaseFirestore = FirestoreProvider.getDb(context)
    private val auth = Firebase.auth

    companion object {
        private const val TAG = "Auth"
        const val BOOTSTRAPPED_ADMIN_EMAIL = "anu61284@gmail.com"
    }

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isBootstrappedAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)
    }

    fun attemptAutoSignIn(
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        scope.launch {
            try {
                val cred = credentialManager.getCredential(context, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        activityContext: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            activityContext.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        scope.launch {
            try {
                val activity = activityContext as? Activity
                    ?: throw IllegalStateException("Activity context required for Google Sign-In")
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type returned.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }

    suspend fun ensurePlayerProfileInitialized(): Result<PlayerProfile> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val playerPath = "players/$uid"
        val privatePath = "users/$uid/private/account"
        return try {
            val playerRef = db.collection("players").document(uid)
            val snap = playerRef.get().await()
            val user = auth.currentUser!!
            if (!snap.exists()) {
                val rawName = user.displayName?.takeIf { it.isNotBlank() } ?: "CricketPro Player"
                val safeName = rawName.take(60)
                val baseHandle = safeName.lowercase().replace("[^a-z0-9]".toRegex(), "").take(16).ifBlank { "player" }
                val handle = "${baseHandle}_${uid.take(4)}"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "handle" to handle,
                    "battingStyle" to "Right-Hand Bat",
                    "bowlingStyle" to "Right-Arm Medium",
                    "playingRole" to "All-Rounder",
                    "privateStats" to false,
                    "role" to "user",
                    "followersCount" to 0,
                    "matchesPlayed" to 0,
                    "runsScored" to 0,
                    "ballsFaced" to 0,
                    "highestScore" to 0,
                    "fifties" to 0,
                    "hundreds" to 0,
                    "fours" to 0,
                    "sixes" to 0,
                    "wicketsTaken" to 0,
                    "runsConceded" to 0,
                    "ballsBowled" to 0,
                    "bestBowlingWickets" to 0,
                    "bestBowlingRuns" to 0,
                    "catches" to 0,
                    "stumpings" to 0,
                    "runOuts" to 0,
                    "mvpPoints" to 0.0,
                    "badges" to listOf("Debutant"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                playerRef.set(payload).await()
            }

            val privRef = db.collection("users").document(uid).collection("private").document("account")
            val privSnap = privRef.get().await()
            if (!privSnap.exists()) {
                val privPayload = mapOf(
                    "userId" to uid,
                    "email" to (user.email ?: "").take(120),
                    "phone" to "",
                    "notificationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                privRef.set(privPayload).await()
            }

            val loaded = playerRef.get().await()
                .toObject(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: PlayerProfile(userId = uid, displayName = user.displayName ?: "Player")
            Result.success(loaded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$playerPath / $privatePath")
            Result.failure(e)
        }
    }

    fun observeMyProfile(): Flow<PlayerProfile?> = flow {
        val uid = requireUserId()
        val path = "players/$uid"
        emitAll(
            db.collection("players").document(uid)
                .snapshots()
                .map { it.toObject(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    fun observeMyPrivateAccount(): Flow<PrivateAccount?> = flow {
        val uid = requireUserId()
        val path = "users/$uid/private/account"
        emitAll(
            db.collection("users").document(uid).collection("private").document("account")
                .snapshots()
                .map { it.toObject(PrivateAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    suspend fun updateProfile(
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
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanName = displayName.trim().take(60)
        val cleanHandle = handle.trim().take(30)
        if (cleanName.isEmpty() || cleanHandle.length < 2) {
            return Result.failure(IllegalArgumentException("Name and handle (min 2 chars) are required."))
        }
        val playerPath = "players/$uid"
        val privatePath = "users/$uid/private/account"
        return try {
            val profileUpdate = mutableMapOf<String, Any?>(
                "displayName" to cleanName,
                "handle" to cleanHandle,
                "battingStyle" to battingStyle.take(40),
                "bowlingStyle" to bowlingStyle.take(40),
                "playingRole" to playingRole.take(40),
                "city" to city.trim().take(60),
                "bio" to bio.trim().take(200),
                "privateStats" to privateStats,
                "photoData" to photoData?.take(64000),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("players").document(uid).update(profileUpdate).await()
            db.collection("users").document(uid).collection("private").document("account")
                .update(
                    mapOf(
                        "email" to email.trim().take(120),
                        "phone" to phone.trim().take(30),
                        "notificationsEnabled" to notificationsEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "$playerPath / $privatePath")
            Result.failure(e)
        }
    }

    suspend fun encodeUriToCompressedBase64(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Could not open selected image")
            val original = input.use { BitmapFactory.decodeStream(it) }
                ?: throw IllegalArgumentException("Invalid image format")
            val maxDim = 220
            val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val scaled = Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1),
                true
            )
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        }
    }
}
