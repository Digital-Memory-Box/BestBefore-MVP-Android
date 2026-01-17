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
    val name: String
)

data class CreateRoomResponse(
    val id: String
)

data class RoomDto(
    @SerializedName("_id") val id: String,
    val name: String,
    val ownerId: String,
    val ownerEmail: String?,
    val createdAt: String?
)
