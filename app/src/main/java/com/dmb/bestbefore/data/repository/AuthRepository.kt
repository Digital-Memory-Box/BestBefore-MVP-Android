package com.dmb.bestbefore.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.UpdateMeRequest
import com.dmb.bestbefore.data.api.models.UserDto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("BestBeforePrefs", Context.MODE_PRIVATE)
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
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
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

    /**
     * Call POST /auth/sync with the given Firebase ID token.
     * The backend will find or create the MongoDB user and return its profile.
     */
    suspend fun syncWithBackend(firebaseIdToken: String): Result<UserDto> {
        return try {
            val response = api.syncAuth("Bearer $firebaseIdToken")
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
                saveUser(user)
                saveToken(firebaseIdToken)
                Result.success(user)
            } else {
                Result.failure(Exception("Backend sync failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
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
                saveUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Update failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Sign out from Firebase and clear local session. */
    fun logout() {
        firebaseAuth.signOut()
        prefs.edit { clear() }
    }

    // ── Local cache helpers ────────────────────────────────────────────────────

    fun saveToken(token: String) {
        prefs.edit { putString("auth_token", token) }
    }

    fun getCachedToken(): String? = prefs.getString("auth_token", null)

    fun saveUser(user: UserDto) {
        prefs.edit {
            putString("user_id", user.id)
            putString("user_name", user.name)
            putString("user_email", user.email)
        }
    }
}
