package com.dmb.bestbefore.data.repository

import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.models.AppNotification
import com.dmb.bestbefore.data.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val api = RetrofitClient.apiService

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
}
