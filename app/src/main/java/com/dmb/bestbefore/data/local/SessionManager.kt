package com.dmb.bestbefore.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dmb.bestbefore.data.api.models.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {

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
        private const val KEY_PROFILE_IMAGE_URL = "profile_image_url" // from backend
        private const val KEY_CACHED_USER = "cached_user_dto"
        private const val KEY_CACHED_HALLWAY = "cached_hallway_cards"
        private const val KEY_MANUAL_PROFILE_TAGS = "manual_profile_tags"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_HAS_SEEN_TUTORIAL = "has_seen_tutorial"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = initPreferences(context)
    private val gson = Gson()

    private fun initPreferences(context: Context): SharedPreferences {
        return try {
            val encrypted = createEncryptedPrefs(context)
            // Test if existing keys can be decrypted without crashing (e.g. bad base-64 from legacy plaintext XML)
            encrypted.all
            encrypted
        } catch (e: Throwable) {
            android.util.Log.w("SessionManager", "EncryptedSharedPreferences validation failed, resetting file: ${e.message}")
            try {
                context.deleteSharedPreferences(PREF_NAME)
                val fresh = createEncryptedPrefs(context)
                fresh.all
                fresh
            } catch (e2: Throwable) {
                android.util.Log.e("SessionManager", "Fallback to standard SharedPreferences: ${e2.message}")
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
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
            putString(KEY_CACHED_USER, gson.toJson(user))
            putString(KEY_IGNORED_ROOMS, gson.toJson(user.ignoredRoomIds))
            putString(KEY_SAVED_ROOMS, gson.toJson(user.savedRoomIds))
            // Persist profileImageUrl from backend so it survives app restarts.
            if (!user.profileImageUrl.isNullOrBlank()) {
                putString(KEY_PROFILE_IMAGE_URL, user.profileImageUrl)
            } else if (!user.profileImageData.isNullOrBlank()) {
                // If only raw data is present, store it as a data URI
                putString(KEY_PROFILE_IMAGE_URL, "data:image/jpeg;base64,${user.profileImageData}")
            }
        }
    }

    fun getCachedUser(): UserDto? {
        val json = prefs.getString(KEY_CACHED_USER, null) ?: return null
        return try {
            gson.fromJson(json, UserDto::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun saveManualProfileTags(tags: List<String>) {
        prefs.edit { putString(KEY_MANUAL_PROFILE_TAGS, gson.toJson(tags)) }
    }

    fun getManualProfileTags(): List<String>? {
        val json = prefs.getString(KEY_MANUAL_PROFILE_TAGS, null) ?: return null
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHallwayCards(cards: List<com.dmb.bestbefore.data.models.HallwayCard>) {
        val sanitized = cards.map { card ->
            val safeImageUrl = if (card.imageUrl != null && card.imageUrl.startsWith("data:") && card.imageUrl.length > 2000) null else card.imageUrl
            val safePhotos = card.photos.filter { it.url.length <= 2000 }
            card.copy(imageUrl = safeImageUrl, photos = safePhotos)
        }
        prefs.edit { putString(KEY_CACHED_HALLWAY, gson.toJson(sanitized)) }
    }

    fun getHallwayCards(): List<com.dmb.bestbefore.data.models.HallwayCard> {
        val json = prefs.getString(KEY_CACHED_HALLWAY, null) ?: return emptyList()
        val type = object : TypeToken<List<com.dmb.bestbefore.data.models.HallwayCard>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getProfileMusic(): String? = prefs.getString(KEY_PROFILE_MUSIC, null)
    fun getProfilePhotoUri(): String? = prefs.getString(KEY_PROFILE_PHOTO_URI, null)
    fun getProfileImageUrl(): String? = prefs.getString(KEY_PROFILE_IMAGE_URL, null)
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

    fun hasSeenTutorial(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_TUTORIAL, false)

    fun setHasSeenTutorial(seen: Boolean) {
        prefs.edit { putBoolean(KEY_HAS_SEEN_TUTORIAL, seen) }
    }

    fun isLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    fun clearSession() {
        try {
            val seenTutorial = hasSeenTutorial()
            prefs.edit { clear() }
            setHasSeenTutorial(seenTutorial)
        } catch (e: Throwable) {
            android.util.Log.e("SessionManager", "Error clearing preferences: ${e.message}")
        }
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Error signing out Firebase: ${e.message}")
        }
    }
}
