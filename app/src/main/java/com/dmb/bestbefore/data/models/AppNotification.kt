package com.dmb.bestbefore.data.models

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: NotificationType = NotificationType.GENERAL,
    val relatedRoomId: String? = null,
    val relatedRoomName: String? = null,
    // For JOIN_REQUEST notifications: the email of the person who wants to join
    val requesterEmail: String? = null
)

enum class NotificationType {
    ROOM_CREATED,
    ROOM_UNLOCKED,
    GENERAL,
    INVITATION,
    JOIN_REQUEST,  // Owner receives this when someone requests to join via QR/link
    MEMORY_ADDED
}
