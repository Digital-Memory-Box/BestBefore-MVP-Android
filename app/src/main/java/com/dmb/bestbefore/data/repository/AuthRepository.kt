package com.dmb.bestbefore.data.repository

import android.content.Context
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.UpdateMeRequest
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.local.SessionManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.IOException

class AuthRepository(context: Context) {
    private val sessionManager = SessionManager(context)
    private val api = RetrofitClient.apiService
    private val firebaseAuth = FirebaseAuth.getInstance()

    /** Get a fresh Firebase ID token for the currently signed-in user. */
    suspend fun getFirebaseIdToken(forceRefresh: Boolean = false): String? {
        return try {
            firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sign in with Firebase email/password, then sync to MongoDB backend via POST /auth/sync.
     * Returns the fully synced [UserDto].
     */
    suspend fun login(email: String, password: String): Result<UserDto> {
        return try {
            // 1. Firebase sign-in
            val authResult = signInWithRetry(email, password)
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Firebase sign-in returned no user"))

            // 2. Get Firebase ID token
            val idToken = firebaseUser.getIdToken(false).await()?.token
                ?: return Result.failure(Exception("Failed to retrieve Firebase ID token"))

            // 3. Sync with backend (creates MongoDB user if first login)
            syncWithBackend(idToken)
        } catch (e: Exception) {
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
    suspend fun signup(email: String, password: String, name: String? = null): Result<UserDto> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Firebase signup returned no user"))

            val idToken = firebaseUser.getIdToken(false).await()?.token
                ?: return Result.failure(Exception("Failed to retrieve Firebase ID token"))

            // Optional: you can wait until the sync sets up the document, then issue a PATCH /me to set the name.
            // Currently, the backend sync creates the document.
            val syncResult = syncWithBackend(idToken)
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
    suspend fun syncWithBackend(firebaseIdToken: String): Result<UserDto> {
        return try {
            val response = api.syncAuth("Bearer $firebaseIdToken")
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
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
    suspend fun updateMe(updates: UpdateMeRequest): Result<UserDto> {
        return try {
            val token = getFirebaseIdToken() ?: return Result.failure(Exception("Not signed in"))
            val response = api.updateMe("Bearer $token", updates)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
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
            android.util.Log.e("AuthRepository", "Update exception", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch the user's latest data from the backend via GET /auth/me
     */
    suspend fun getMe(): Result<UserDto> {
        return try {
            val token = getFirebaseIdToken() ?: return Result.failure(Exception("Not signed in"))
            val response = api.getMe("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
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

    suspend fun syncFcmToken(): Result<Unit> {
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
        sessionManager.clearSession()
    }

    fun getCachedToken(): String? = sessionManager.getToken()
}
