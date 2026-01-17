package com.dmb.bestbefore.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.*

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("BestBeforePrefs", Context.MODE_PRIVATE)
    private val api = RetrofitClient.apiService

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                saveToken(authResponse.token)
                saveUser(authResponse.user)
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(name: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.signup(SignupRequest(name, email, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                saveToken(authResponse.token)
                saveUser(authResponse.user)
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Signup failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    private fun saveUser(user: UserDto) {
        prefs.edit()
            .putString("user_id", user.id)
            .putString("user_name", user.name)
            .putString("user_email", user.email)
            .apply()
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }
    
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
