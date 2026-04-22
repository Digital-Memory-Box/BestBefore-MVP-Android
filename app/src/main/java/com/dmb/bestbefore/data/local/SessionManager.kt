package com.dmb.bestbefore.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.dmb.bestbefore.data.api.models.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "BestBeforeSession"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_PROFILE_MUSIC = "profile_music"
        private const val KEY_PROFILE_PHOTO_URI = "profile_photo_uri"
        private const val KEY_THEME = "user_theme"
        private const val KEY_ACCENT_COLOR = "user_accent_color"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_BIO = "user_bio"
        private const val KEY_IGNORED_ROOMS = "ignored_room_ids"
        private const val KEY_SAVED_ROOMS = "saved_room_ids"
        private const val KEY_ROOM_EMOTIONS_PREFIX = "room_emotions_"
    }

    fun saveAuthToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUser(user: UserDto) {
        prefs.edit {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_PROFILE_MUSIC, user.profileMusic)
            putString(KEY_THEME, user.theme)
            putString(KEY_ACCENT_COLOR, user.accentColor)
            putString(KEY_USER_TYPE, user.userType)
            putString(KEY_BIO, user.bio)
            putString(KEY_IGNORED_ROOMS, gson.toJson(user.ignoredRoomIds))
            putString(KEY_SAVED_ROOMS, gson.toJson(user.savedRoomIds))
        }
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getProfileMusic(): String? = prefs.getString(KEY_PROFILE_MUSIC, null)
    fun getProfilePhotoUri(): String? = prefs.getString(KEY_PROFILE_PHOTO_URI, null)
    fun getTheme(): String = prefs.getString(KEY_THEME, "Default") ?: "Default"
    fun getAccentColor(): String = prefs.getString(KEY_ACCENT_COLOR, "#007AFF") ?: "#007AFF"
    fun getUserType(): String = prefs.getString(KEY_USER_TYPE, "normal") ?: "normal"
    fun getBio(): String? = prefs.getString(KEY_BIO, null)
    
    fun getIgnoredRoomIds(): List<String> {
        val json = prefs.getString(KEY_IGNORED_ROOMS, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getSavedRoomIds(): List<String> {
        val json = prefs.getString(KEY_SAVED_ROOMS, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveTheme(theme: String) {
        prefs.edit { putString(KEY_THEME, theme) }
    }

    fun saveAccentColor(hex: String) {
        prefs.edit { putString(KEY_ACCENT_COLOR, hex) }
    }

    fun saveProfileMusic(music: String?) {
        prefs.edit { putString(KEY_PROFILE_MUSIC, music) }
    }

    fun saveProfilePhotoUri(uri: String?) {
        prefs.edit { putString(KEY_PROFILE_PHOTO_URI, uri) }
    }

    fun saveUserEmail(email: String) {
        prefs.edit { putString(KEY_USER_EMAIL, email) }
    }

    private fun roomEmotionKey(userId: String?): String {
        return KEY_ROOM_EMOTIONS_PREFIX + (userId ?: "anonymous")
    }

    fun getRoomEmotions(userId: String?): Map<String, String> {
        val json = prefs.getString(roomEmotionKey(userId), null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    fun saveRoomEmotions(userId: String?, emotions: Map<String, String>) {
        prefs.edit { putString(roomEmotionKey(userId), gson.toJson(emotions)) }
    }

    fun isLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    fun clearSession() {
        prefs.edit { clear() }
        FirebaseAuth.getInstance().signOut()
    }
}
