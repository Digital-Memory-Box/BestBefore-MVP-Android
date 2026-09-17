package com.dmb.bestbefore.data.repository

import android.content.Context
import com.dmb.bestbefore.data.api.ApiService
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.UpdateMeRequest
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.local.SessionManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.IOException

import com.dmb.bestbefore.data.auth.FirebaseTokenProvider
import com.dmb.bestbefore.data.auth.TokenProvider

open class AuthRepository(
    context: Context,
    private val sessionManager: SessionManager = SessionManager.getInstance(context),
    private val api: ApiService = RetrofitClient.apiService,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val tokenProvider: TokenProvider = FirebaseTokenProvider(firebaseAuth)
) {

    /** Get a fresh Firebase ID token for the currently signed-in user. */
    open suspend fun getFirebaseIdToken(forceRefresh: Boolean = false): String? {
        return tokenProvider.getIdToken(forceRefresh)
    }

    /**
     * Sign in with Firebase email/password, then sync to MongoDB backend via POST /auth/sync.
     * Returns the fully synced [UserDto].
     */
    open suspend fun login(email: String, password: String): Result<UserDto> {
        return try {
            // 1. Firebase sign-in
            val authResult = signInWithRetry(email, password)
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Firebase sign-in returned no user"))

            // Verify email
            if (!firebaseUser.isEmailVerified) {
                try { firebaseAuth.signOut() } catch (_: Exception) {}
                return Result.failure(Exception("Please verify your email address before logging in. Check your inbox for the verification link."))
            }

            // 2. Get Firebase ID token
            val idToken = firebaseUser.getIdToken(false).await()?.token
                ?: return Result.failure(Exception("Failed to retrieve Firebase ID token"))

            // 3. Sync with backend (creates MongoDB user if first login)
            val result = syncWithBackend(idToken)
            if (result.isSuccess) {
                com.dmb.bestbefore.analytics.AnalyticsManager.logLogin("email")
            } else {
                try { firebaseAuth.signOut() } catch (_: Exception) {}
                sessionManager.clearSession()
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google ID token credential, then sync to MongoDB backend via POST /auth/sync.
     * Returns the fully synced [UserDto].
     */
    open suspend fun loginWithGoogleIdToken(idToken: String): Result<UserDto> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Firebase Google sign-in returned no user"))

            val firebaseIdToken = firebaseUser.getIdToken(false).await()?.token
                ?: return Result.failure(Exception("Failed to retrieve Firebase ID token"))

            val result = syncWithBackend(firebaseIdToken)
            if (result.isSuccess) {
                com.dmb.bestbefore.analytics.AnalyticsManager.logLogin("google")
            } else {
                try { firebaseAuth.signOut() } catch (_: Exception) {}
                sessionManager.clearSession()
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify email exists in DB, then send password reset link via Firebase.
     */
    open suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            val normalizedEmail = email.trim().lowercase()
            val response = api.forgotPassword(mapOf("email" to normalizedEmail))
            if (!response.isSuccessful) {
                val msg = if (response.code() == 404) {
                    "No account found with this email in our database."
                } else {
                    "Unable to verify account. Please try again."
                }
                return Result.failure(Exception(msg))
            }
            // Send reset email with custom continue URL pointing to our branded web UI,
            // with resilient fallback to standard reset email if whitelist is pending.
            try {
                val actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setUrl("https://bestbefore.up.railway.app/reset-password")
                    .setHandleCodeInApp(false)
                    .build()
                firebaseAuth.sendPasswordResetEmail(normalizedEmail, actionCodeSettings).await()
            } catch (actionCodeException: Exception) {
                if (actionCodeException is kotlinx.coroutines.CancellationException) throw actionCodeException
                android.util.Log.w("AuthRepository", "Custom action code email failed, falling back to standard: ${actionCodeException.message}")
                firebaseAuth.sendPasswordResetEmail(normalizedEmail).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    private suspend fun signInWithRetry(email: String, password: String): com.google.firebase.auth.AuthResult {
        var lastError: Exception? = null
        repeat(2) { attempt ->
            try {
                return firebaseAuth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                lastError = e
                val isTransientNetwork = e is FirebaseNetworkException ||
                    (e.message?.contains("network error", ignoreCase = true) == true)
                if (!isTransientNetwork || attempt == 1) {
                    throw e
                }
                delay(700)
            }
        }
        throw lastError ?: Exception("Sign in failed")
    }

    /**
     * Creating a new user via Firebase, then syncing with Backend.
     */
    open suspend fun signup(email: String, password: String, name: String? = null): Result<UserDto> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Firebase signup returned no user"))

            val idToken = firebaseUser.getIdToken(false).await()?.token
                ?: return Result.failure(Exception("Failed to retrieve Firebase ID token"))

            val syncResult = syncWithBackend(idToken)
            if (syncResult.isSuccess) {
                com.dmb.bestbefore.analytics.AnalyticsManager.logSignUp("email")
            }
            if (syncResult.isSuccess && name != null) {
                updateMe(UpdateMeRequest(name = name))
            } else {
                syncResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Call POST /auth/sync with the given Firebase ID token.
     * The backend will find or create the MongoDB user and return its profile.
     */
    open suspend fun syncWithBackend(firebaseIdToken: String): Result<UserDto> {
        return try {
            val response = api.syncAuth("Bearer $firebaseIdToken")
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
                com.dmb.bestbefore.analytics.AnalyticsManager.setUserProperties(user.id, user.userType)
                sessionManager.saveUser(user)
                sessionManager.saveAuthToken(firebaseIdToken)
                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("AuthRepository", "Sync failed: ${response.code()} - $errorBody")
                Result.failure(Exception("Backend sync failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            android.util.Log.w("AuthRepository", "Sync network issue: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Sync exception", e)
            Result.failure(e)
        }
    }

    /** Update user profile fields via PATCH /auth/me */
    open suspend fun updateMe(updates: UpdateMeRequest): Result<UserDto> {
        return try {
            val token = getFirebaseIdToken() ?: return Result.failure(Exception("Not signed in"))
            val response = api.updateMe("Bearer $token", updates)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
                cachedUser = user
                cachedUserTimestamp = System.currentTimeMillis()
                sessionManager.saveUser(user)
                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string()
                if (response.code() in 500..599) {
                    android.util.Log.w("AuthRepository", "Update failed: ${response.code()} - $errorBody")
                } else {
                    android.util.Log.e("AuthRepository", "Update failed: ${response.code()} - $errorBody")
                }
                Result.failure(Exception("Update failed: ${response.code()}"))
            }
        } catch (e: IOException) {
            android.util.Log.w("AuthRepository", "Update network issue: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("AuthRepository", "Update exception", e)
            Result.failure(e)
        }
    }

    companion object {
        private var cachedUser: UserDto? = null
        private var cachedUserTimestamp: Long = 0L
        private const val USER_CACHE_TTL_MS = 30_000L // 30 seconds

        fun invalidateUserCache() {
            cachedUser = null
            cachedUserTimestamp = 0L
        }
    }

    /**
     * Fetch the user's latest data from the backend via GET /auth/me
     */
    open suspend fun getMe(forceRefresh: Boolean = false): Result<UserDto> {
        if (!forceRefresh && cachedUser != null && (System.currentTimeMillis() - cachedUserTimestamp < USER_CACHE_TTL_MS)) {
            return Result.success(cachedUser!!)
        }
        return try {
            val token = getFirebaseIdToken() ?: return Result.failure(Exception("Not signed in"))
            val response = api.getMe("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
                cachedUser = user
                cachedUserTimestamp = System.currentTimeMillis()
                sessionManager.saveUser(user)
                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("AuthRepository", "getMe failed: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to fetch user data: ${response.code()}"))
            }
        } catch (e: IOException) {
            android.util.Log.w("AuthRepository", "getMe network issue: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "getMe exception", e)
            Result.failure(e)
        }
    }

    /**
     * Delete the user's account: backend data + Firebase Auth + local session.
     * Required by Google Play and Apple App Store policies.
     */
    open suspend fun deleteAccount(): Result<Unit> {
        return try {
            val token = getFirebaseIdToken() ?: return Result.failure(Exception("Not signed in"))
            val response = api.deleteAccount("Bearer $token")
            if (response.isSuccessful) {
                // Delete Firebase Auth user locally
                try {
                    firebaseAuth.currentUser?.delete()?.await()
                } catch (e: Exception) {
                    android.util.Log.w("AuthRepository", "Firebase user delete failed (already deleted server-side): ${e.message}")
                }
                invalidateUserCache()
                sessionManager.clearSession()
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("AuthRepository", "Account deletion failed: ${response.code()} - $errorBody")
                Result.failure(Exception("Account deletion failed: ${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Report content for UGC compliance. */
    open suspend fun reportContent(targetType: String, targetId: String, reason: String, description: String = ""): Result<Unit> {
        return try {
            val token = getFirebaseIdToken() ?: return Result.failure(Exception("Not signed in"))
            val body = mapOf("targetType" to targetType, "targetId" to targetId, "reason" to reason, "description" to description)
            val response = api.reportContent("Bearer $token", body)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Report failed: ${response.code()}"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Block a user for UGC compliance. */
    open suspend fun blockUser(blockedUserId: String): Result<Unit> {
        return try {
            val token = getFirebaseIdToken() ?: return Result.failure(Exception("Not signed in"))
            val body = mapOf("blockedUserId" to blockedUserId)
            val response = api.blockUser("Bearer $token", body)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Block failed: ${response.code()}"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    open suspend fun syncFcmToken(): Result<Unit> {
        return try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            val token = getFirebaseIdToken() ?: return Result.failure(Exception("Not signed in"))
            val response = api.updateMe("Bearer $token", UpdateMeRequest(fcmToken = fcmToken))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to sync fcmToken: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Sign out from Firebase and clear local session. */
    fun logout() {
        invalidateUserCache()
        sessionManager.clearSession()
    }

    fun getCachedToken(): String? = sessionManager.getToken()
}
