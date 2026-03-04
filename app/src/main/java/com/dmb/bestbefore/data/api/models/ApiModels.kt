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
    val profileMusic: String? = null
)

data class UpdateMeRequest(
    val name: String? = null,
    val theme: String? = null,
    val accentColor: String? = null,
    val profileMusic: String? = null,
    val email: String? = null,
    val fcmToken: String? = null
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
    val ownerEmail: String?,
    val createdAt: String?,
    val photos: List<String>? = null,
    val capsuleDurationDays: Int = 0,
    val capsuleDurationHours: Int = 0,
    val capsuleDurationMinutes: Int = 0,
    val isPrivate: Boolean = false,
    val isTimeCapsule: Boolean = false,
    val theme: String? = null,
    val unlockDate: String? = null,
    val expirationDate: String? = null,
    val collaborators: List<Any>? = null,
    val pendingCollaborators: List<String>? = null,
    val rollingExpiryDays: Int = 0,
    val description: String? = null,
    val tags: List<String>? = null,
    val backgroundMusic: String? = null
)

data class UploadResponse(
    val imageUrl: String
)
