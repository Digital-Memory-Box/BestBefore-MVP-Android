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
    val profileImageUrl: String? = null,
    val profileImageData: String? = null, // Raw Base64 data from iOS/Backend
    val ignoredRoomIds: List<String>? = emptyList(),
    val savedRoomIds: List<String>? = emptyList(),
    val preferredTags: List<String>? = emptyList(),
    // AI Preference fields (matches iOS + backend schema)
    val preferenceTagWeights: Map<String, Double>? = null,
    val preferenceRoomTypes: List<String>? = null,
    val preferenceEmbedding: List<Float>? = null,
    val preferenceInteractions: List<String>? = null,
    val preferenceUpdatedAt: String? = null,
    val lastLat: Double? = null,
    val lastLon: Double? = null
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
    val profileImageUrl: String? = null,
    val profileImageBase64: String? = null,
    val profileImageData: String? = null,
    val ignoredRoomIds: List<String>? = null,
    val savedRoomIds: List<String>? = null,
    val preferredTags: List<String>? = null,
    // AI Preference fields — mirrors iOS user document schema
    val preferenceTagWeights: Map<String, Double>? = null,
    val preferenceRoomTypes: List<String>? = null,
    val preferenceEmbedding: List<Float>? = null,
    val preferenceInteractions: List<String>? = null,
    val preferenceUpdatedAt: String? = null,
    val lastLat: Double? = null,
    val lastLon: Double? = null
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

data class MemoryPreview(
    val id: String,
    val url: String
)

data class RoomDto(
    @SerializedName(value = "_id", alternate = ["id"]) val id: String,
    val name: String,
    val ownerId: String = "",
    val ownerName: String? = null,
    val ownerEmail: String?,
    val ownerUserType: String? = null,
    val ownerProfilePic: String? = null,
    val createdAt: String?,
    val photos: List<MemoryPreview>? = null,
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
    val generatedDescription: String? = null,
    val tags: List<String>? = null,
    val backgroundMusic: String? = null,
    val connectedRooms: List<String>? = null
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

// Matches backend suggestionController.js response
data class RoomSuggestionDto(
    val targetRoomId: String,
    val targetRoomName: String,
    val score: Int,
    val category: String,
    val reasoning: String? = null,
    val similarity: Double? = null
)

data class RoomSuggestionsResponse(
    val sourceRoomId: String,
    val suggestions: List<RoomSuggestionDto>,
    val count: Int,
    val strategy: String? = null
)

data class UploadResponse(
    val imageUrl: String
)

// ── AI-powered Initial Discovery (POST /rooms/discover/initial) ───────────────

data class InitialDiscoveryRequest(
    val preferredTags: List<String> = emptyList(),
    val userLat: Double? = null,
    val userLon: Double? = null,
    val userName: String = "",
    val bio: String = ""
)

data class DiscoveryRoomResult(
    val roomId: String,
    val name: String,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val isTimeCapsule: Boolean = false,
    val similarity: Double = 0.0,
    val distance: Double? = null
)

data class InitialDiscoveryResponse(
    val results: List<DiscoveryRoomResult>,
    val count: Int,
    val strategy: String? = null,
    val queryUsed: String? = null
)

// ── Backend Semantic Search (POST /search/search) ─────────────────────────────

data class BackendSearchRequest(
    val query: String,
    val userLat: Double? = null,
    val userLon: Double? = null,
    val maxDistance: Double? = null
)

data class BackendSearchResult(
    val roomId: String,
    val name: String,
    val similarity: Double,
    val description: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val tags: List<String>? = null,
    val distance: Double? = null
)

data class BackendSearchResponse(
    val query: String,
    val results: List<BackendSearchResult>,
    val count: Int
)
