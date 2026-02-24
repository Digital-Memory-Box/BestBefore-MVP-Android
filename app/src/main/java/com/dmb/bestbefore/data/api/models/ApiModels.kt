package com.dmb.bestbefore.data.api.models

import com.google.gson.annotations.SerializedName

// Auth Models
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val user: UserDto,
    val token: String
)

data class UserDto(
    val id: String,
    val name: String?,
    val email: String
)

// Room Models
data class CreateRoomRequest(
    val name: String,
    val capsuleDays: Int,
    val capsuleHours: Int,
    val capsuleMinutes: Int,
    val isPublic: Boolean,
    val isCollaboration: Boolean
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
    val capsuleDays: Int = 0,
    val capsuleHours: Int = 0,
    val capsuleMinutes: Int = 0,
    val isPublic: Boolean = true,
    val isCollaboration: Boolean = false
)

data class UploadResponse(
    val imageUrl: String
)

// Calendar Models
data class CalendarAuthResponse(
    val url: String
)

data class CalendarEventDto(
    val id: String,
    val title: String,
    val start: String,
    val end: String,
    val description: String?,
    val location: String?
)
