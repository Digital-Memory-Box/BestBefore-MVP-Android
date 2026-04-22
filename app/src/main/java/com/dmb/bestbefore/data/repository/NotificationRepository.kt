package com.dmb.bestbefore.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.models.AppNotification
import com.dmb.bestbefore.data.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository(private val context: Context? = null) {
    private val api = RetrofitClient.apiService
    private val gson = Gson()
    private val prefs: SharedPreferences? = context?.applicationContext
        ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val notificationsFlow = MutableStateFlow(loadNotifications())
    val notifications: StateFlow<List<AppNotification>> = notificationsFlow.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_NOTIFICATIONS) {
            notificationsFlow.value = loadNotifications()
        }
    }

    init {
        prefs?.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun loadNotifications(): List<AppNotification> {
        val localPrefs = prefs ?: return emptyList()
        val json = localPrefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<AppNotification>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveNotifications(list: List<AppNotification>) {
        val localPrefs = prefs ?: return
        localPrefs.edit { putString(KEY_NOTIFICATIONS, gson.toJson(list)) }
    }

    fun addNotification(notification: AppNotification) {
        if (prefs == null) return
        val updated = listOf(notification) + notificationsFlow.value.filter { it.id != notification.id }
        saveNotifications(updated)
        notificationsFlow.value = updated
    }

    fun mergeNotifications(incoming: List<AppNotification>) {
        if (prefs == null) return
        if (incoming.isEmpty()) return
        val combined = (notificationsFlow.value + incoming)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.timestamp }
        saveNotifications(combined)
        notificationsFlow.value = combined
    }

    fun removeNotification(id: String) {
        if (prefs == null) return
        val updated = notificationsFlow.value.filterNot { it.id == id }
        saveNotifications(updated)
        notificationsFlow.value = updated
    }

    fun clearAll() {
        if (prefs == null) return
        saveNotifications(emptyList())
        notificationsFlow.value = emptyList()
    }

    suspend fun getNotifications(): Result<List<AppNotification>> {
        return try {
            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                ?: return Result.failure(Exception("Not authenticated"))
            
            val response = api.getNotifications("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val notifications = response.body()!!.map { map ->
                    val typeStr = map["type"] as? String
                    val type = when (typeStr) {
                        "INVITATION" -> NotificationType.INVITATION
                        "ROOM_CREATED" -> NotificationType.ROOM_CREATED
                        "JOIN_REQUEST" -> NotificationType.JOIN_REQUEST
                        else -> NotificationType.GENERAL
                    }
                    
                    AppNotification(
                        id = map["_id"] as? String ?: java.util.UUID.randomUUID().toString(),
                        title = map["title"] as? String ?: "",
                        message = map["body"] as? String ?: "",
                        type = type,
                        relatedRoomId = map["roomId"] as? String,
                        relatedRoomName = map["roomName"] as? String
                    )
                }
                Result.success(notifications)
            } else {
                Result.failure(Exception("Failed to fetch notifications"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToInvitation(notificationId: String, accept: Boolean): Result<Unit> {
        return try {
            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                ?: return Result.failure(Exception("Not authenticated"))
            
            val action = if (accept) "accept" else "ignore"
            val response = api.respondToNotification("Bearer $token", notificationId, mapOf("action" to action))
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to respond to notification"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val PREFS_NAME = "BestBeforeNotifications"
        private const val KEY_NOTIFICATIONS = "notifications"
    }
}
