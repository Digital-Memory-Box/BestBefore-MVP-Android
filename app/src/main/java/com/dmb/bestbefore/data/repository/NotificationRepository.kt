package com.dmb.bestbefore.data.repository

import android.content.Context
import android.util.Log
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.models.AppNotification
import com.dmb.bestbefore.data.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class NotificationRepository(private val context: Context? = null) {
    companion object {
        private const val PREFS_NAME = "bestbefore_notifications"
        private const val KEY_NOTIFICATIONS = "notifications_json"
        private const val MAX_LOCAL_NOTIFICATIONS = 200
    }

    private val api = RetrofitClient.apiService
    private val _notifications = MutableStateFlow(loadLocalNotifications())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    fun addNotification(notification: AppNotification) {
        val updated = (_notifications.value.filterNot { it.id == notification.id } + notification)
            .sortedByDescending { it.timestamp }
            .take(MAX_LOCAL_NOTIFICATIONS)
        _notifications.value = updated
        persistLocalNotifications(updated)
    }

    fun removeNotification(id: String) {
        val updated = _notifications.value.filterNot { it.id == id }
        _notifications.value = updated
        persistLocalNotifications(updated)
    }

    fun clearAll() {
        _notifications.value = emptyList()
        persistLocalNotifications(emptyList())
    }

    fun mergeNotifications(remote: List<AppNotification>) {
        val mergedById = linkedMapOf<String, AppNotification>()
        (_notifications.value + remote).forEach { notif ->
            mergedById[notif.id] = notif
        }
        val merged = mergedById.values
            .sortedByDescending { it.timestamp }
            .take(MAX_LOCAL_NOTIFICATIONS)
        _notifications.value = merged
        persistLocalNotifications(merged)
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
                        "ROOM_UNLOCKED" -> NotificationType.ROOM_UNLOCKED
                        "QR_JOIN_REQUESTED", "INVITE_REQUESTED" -> NotificationType.JOIN_REQUEST
                        "JOIN_REQUEST" -> NotificationType.JOIN_REQUEST
                        else -> NotificationType.GENERAL
                    }

                    val title = (map["title"] as? String)
                        ?: (map["subject"] as? String)
                        ?: "BestBefore"
                    val message = (map["body"] as? String)
                        ?: (map["message"] as? String)
                        ?: ""

                    AppNotification(
                        id = map["_id"] as? String ?: java.util.UUID.randomUUID().toString(),
                        title = title,
                        message = message,
                        timestamp = parseTimestamp(map),
                        type = type,
                        relatedRoomId = map["roomId"] as? String,
                        relatedRoomName = map["roomName"] as? String,
                        requesterEmail = (map["requesterEmail"] as? String) ?: (map["fromEmail"] as? String)
                    )
                }
                mergeNotifications(notifications)
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
                removeNotification(notificationId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to respond to notification"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTimestamp(map: Map<String, Any>): Long {
        val rawTimestamp = map["timestamp"]
        if (rawTimestamp is Number) return rawTimestamp.toLong()

        val createdAt = map["createdAt"] as? String
        if (!createdAt.isNullOrBlank()) {
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            patterns.forEach { pattern ->
                runCatching {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    return sdf.parse(createdAt)?.time ?: System.currentTimeMillis()
                }
            }
        }

        return System.currentTimeMillis()
    }

    private fun loadLocalNotifications(): List<AppNotification> {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return emptyList()
        val raw = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()

        return runCatching {
            val jsonArray = JSONArray(raw)
            buildList {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    add(
                        AppNotification(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            title = obj.optString("title", "BestBefore"),
                            message = obj.optString("message", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            type = parseType(obj.optString("type", NotificationType.GENERAL.name)),
                            relatedRoomId = obj.optNullableString("relatedRoomId"),
                            relatedRoomName = obj.optNullableString("relatedRoomName"),
                            requesterEmail = obj.optNullableString("requesterEmail")
                        )
                    )
                }
            }
        }.getOrElse {
            Log.e("NotificationRepository", "Failed to parse local notifications", it)
            emptyList()
        }
    }

    private fun persistLocalNotifications(list: List<AppNotification>) {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val jsonArray = JSONArray()
        list.forEach { n ->
            jsonArray.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("message", n.message)
                    put("timestamp", n.timestamp)
                    put("type", n.type.name)
                    put("relatedRoomId", n.relatedRoomId)
                    put("relatedRoomName", n.relatedRoomName)
                    put("requesterEmail", n.requesterEmail)
                }
            )
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply()
    }

    private fun parseType(value: String): NotificationType {
        return when (value) {
            NotificationType.ROOM_CREATED.name -> NotificationType.ROOM_CREATED
            NotificationType.ROOM_UNLOCKED.name -> NotificationType.ROOM_UNLOCKED
            NotificationType.INVITATION.name -> NotificationType.INVITATION
            NotificationType.JOIN_REQUEST.name -> NotificationType.JOIN_REQUEST
            else -> NotificationType.GENERAL
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
    }
}
