package com.dmb.bestbefore.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "BestBeforeSession"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
    }

    fun saveAuthToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUser(userId: String, name: String, email: String) {
        prefs.edit {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
        }
    }

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    
    fun saveUserEmail(email: String) {
        prefs.edit { putString(KEY_USER_EMAIL, email) }
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    fun clearSession() {
        prefs.edit { clear() }
    }
}
