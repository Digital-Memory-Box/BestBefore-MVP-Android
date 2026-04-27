package com.dmb.bestbefore.data.ai

import com.google.gson.annotations.SerializedName

// ── Shared ─────────────────────────────────────────────────────────────────

/** Mirrors Python InteractionType enum */
enum class AiInteractionType {
    VIEW, LIKE, IGNORE, MEMORY_ADD, SUGGESTION_ACCEPT, SUGGESTION_REJECT, OTHER
}

// ── Hybrid Scoring (schemas.py) ─────────────────────────────────────────────

data class AiRoomDto(
    val id: String,
    val name: String,
    val tags: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val isTimeCapsule: Boolean = false,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val description: String? = null,
    val embedding: List<Float>? = null,
    val dwellTime: Int = 0
)

data class AiInteractionDto(
    val roomId: String,
    val dwellTimeSeconds: Int,
    val interactionType: String,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timestamp: String? = null
)

data class HybridScoreRequest(
    val sourceRoom: AiRoomDto,
    val targetRoom: AiRoomDto,
    val sourceInteraction: AiInteractionDto? = null,
    val targetInteraction: AiInteractionDto? = null
)

data class HybridScoreDetails(
    val tagScore: Int = 0,
    val dwellScore: Int = 0,
    val interactionScore: Int = 0,
    val locationScore: Int = 0,
    val timeScore: Int = 0
)

data class HybridScoreResponse(
    val score: Int,
    val category: String,       // "direct_match" | "critical_zone" | "reject"
    val needsReasoning: Boolean,
    val reasoning: String? = null,
    val details: HybridScoreDetails
)

// ── User Preference (preference_schemas.py) ──────────────────────────────────

data class PreferenceProfileSnapshot(
    val preferredTags: List<String> = emptyList(),
    val preferenceTagWeights: Map<String, Double> = emptyMap(),
    val preferenceRoomTypes: List<String> = emptyList(),
    val preferenceEmbedding: List<Float> = emptyList(),
    val lastLat: Double? = null,
    val lastLon: Double? = null
)

data class PreferenceRoomEvent(
    val tags: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val isTimeCapsule: Boolean = false,
    val lat: Double? = null,
    val lon: Double? = null,
    val interactionType: String   // VIEW | LIKE | IGNORE | MEMORY_ADD | SUGGESTION_ACCEPT | SUGGESTION_REJECT
)

data class UpdatePreferenceRequest(
    val profile: PreferenceProfileSnapshot,
    val event: PreferenceRoomEvent,
    val topKTags: Int = 30
)

data class UpdatePreferenceResponse(
    val preferredTags: List<String>,
    val preferenceTagWeights: Map<String, Double>,
    val preferenceRoomTypes: List<String>,
    val preferenceEmbedding: List<Float>,
    val lastLat: Double? = null,
    val lastLon: Double? = null
)

// ── Suggestions (suggestion_schemas.py) ──────────────────────────────────────

data class UserPreferenceSchema(
    val preferredTags: List<String> = emptyList(),
    val lastLat: Double? = null,
    val lastLon: Double? = null,
    val interactionRoomTypes: List<String> = emptyList(),
    val preferenceEmbedding: List<Float> = emptyList()
)

data class GenerateSuggestionsRequest(
    val sourceRoomId: String? = null,
    val sourceRoom: AiRoomDto? = null,
    val userProfile: UserPreferenceSchema? = null,
    val candidateRooms: List<AiRoomDto>,
    val userLat: Double? = null,
    val userLon: Double? = null
)

data class AiRoomSuggestion(
    @SerializedName(value = "targetRoomId", alternate = ["roomId", "id", "target_room_id"])
    val targetRoomId: String,
    @SerializedName(value = "targetRoomName", alternate = ["name", "roomName"])
    val targetRoomName: String,
    val score: Int,
    val category: String,
    val reasoning: String? = null,
    val similarity: Double
)

data class GenerateSuggestionsResponse(
    val sourceRoomId: String,
    val suggestions: List<AiRoomSuggestion>,
    val count: Int
)

// ── Embedding & Semantic Search (embedding_schemas.py) ───────────────────────

data class EmbeddingRequest(val text: String)

data class EmbeddingResponse(
    val embedding: List<Float>,
    val text: String,
    val dimension: Int
)

data class SemanticSearchRequest(
    val queryText: String,
    val candidateEmbeddings: List<Map<String, @JvmSuppressWildcards Any>>,
    val topK: Int = 5
)

data class SemanticSearchResult(
    val roomId: String,
    val name: String,
    val similarity: Double
)

data class SemanticSearchResponse(
    val query: String,
    val results: List<SemanticSearchResult>,
    val count: Int
)

// ── Description Generation (generative_schemas.py) ───────────────────────────

data class GenerateDescriptionRequest(
    val roomName: String,
    val tags: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val isTimeCapsule: Boolean = false
)

data class GenerateDescriptionResponse(
    val description: String,
    val roomName: String
)
