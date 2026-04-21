package com.dmb.bestbefore.data.api.models

import com.google.gson.annotations.SerializedName

// Auth Models — backend uses Firebase, only /auth/sync and /auth/me exist
data class SyncAuthResponse(
    val user: UserDto
)

data class UserDto(
    val id: String,
    val name: String?,
    val email: String,
    val theme: String = "Default",
    val accentColor: String = "#007AFF",
    val profileMusic: String? = null,
    val createdAt: String? = null,
    val userType: String? = "normal",
    val bio: String? = null,
    val profileImageUrl: String? = null
)

data class UpdateMeRequest(
    val name: String? = null,
    val theme: String? = null,
    val accentColor: String? = null,
    val profileMusic: String? = null,
    val email: String? = null,
    val fcmToken: String? = null,
    val userType: String? = null,
    val bio: String? = null,
    val profileImageUrl: String? = null
)

// Room Models — field names aligned to backend roomController.js
data class CreateRoomRequest(
    val name: String,
    val isPrivate: Boolean,
    val isTimeCapsule: Boolean,
    val capsuleDurationDays: Int,
    val capsuleDurationHours: Int,
    val capsuleDurationMinutes: Int,
    val theme: String = "default",
    val collaborators: List<String> = emptyList(),
    val viewers: List<String> = emptyList(),
    val unlockDate: String? = null,
    val expirationDate: String? = null,
    val rollingExpiryDays: Int = 0,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val backgroundMusic: String? = null
)

data class CreateRoomResponse(
    val id: String
)

data class RoomDto(
    @SerializedName("_id") val id: String,
    val name: String,
    val ownerId: String,
    val ownerName: String? = null,
    val ownerEmail: String?,
    val ownerUserType: String? = null,
    val ownerProfilePic: String? = null,
    val createdAt: String?,
    val photos: List<String>? = null,
    val capsuleDurationDays: Int = 0,
    val capsuleDurationHours: Int = 0,
    val capsuleDurationMinutes: Int = 0,
    val isPrivate: Boolean = false,
    val isPublic: Boolean? = null,
    val isTimeCapsule: Boolean = false,
    val theme: String? = null,
    val unlockDate: String? = null,
    val expirationDate: String? = null,
    val collaborators: List<com.google.gson.JsonElement>? = null,
    val pendingCollaborators: List<String>? = null,
    val viewers: List<com.google.gson.JsonElement>? = null,
    val pendingViewers: List<String>? = null,
    val rollingExpiryDays: Int = 0,
    val description: String? = null,
    val tags: List<String>? = null,
    val backgroundMusic: String? = null
)

// --- Dynamic Recommendation Models ---

data class RoomInteractionRequest(
    val roomId: String,
    val dwellTimeSeconds: Long,
    val interactionType: String? = null, // "LIKE", "COMMENT", "FUTURE_LOCK"
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class RoomRecommendationDto(
    val roomId: String,
    val roomName: String,
    val relevanceScore: Double,
    val connectionType: String, // "DYNAMIC" (80+ score) or "AI_CONFIRMED" (OpenAI decision)
    val tagsMatch: List<String>,
    val description: String? = null
)

data class UploadResponse(
    val imageUrl: String
)
