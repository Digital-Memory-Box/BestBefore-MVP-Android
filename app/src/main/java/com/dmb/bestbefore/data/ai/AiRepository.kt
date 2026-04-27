package com.dmb.bestbefore.data.ai

import android.util.Log
import com.dmb.bestbefore.data.api.models.RoomDto
import com.dmb.bestbefore.data.api.models.UserDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val TAG = "AiRepository"

/**
 * Repository wrapping the BestBefore AI Service.
 *
 * Key responsibilities:
 * 1. [trackRoomInteraction]     — Fire-and-forget: report a VIEW/LIKE/IGNORE for preference learning.
 * 2. [getPersonalisedSuggestions] — Build a candidate list from the user's rooms and ask the AI
 * service for ranked suggestions using the hybrid score + embeddings.
 * 3. [scoreRoomPair]            — Raw hybrid score between two rooms.
 * 4. [semanticSearch]           — Full-text semantic search over a list of rooms.
 * 5. [generateRoomDescription]  — GPT-generated description for a room being created.
 * 6. [updateUserPreference]     — Explicitly update the stored preference model after an interaction.
 */
class AiRepository {

    private val api = AiServiceClient.api

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Track Interaction → update preference model
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun trackRoomInteraction(
        user: UserDto,
        room: RoomDto,
        interactionType: String,
        userLat: Double? = null,
        userLon: Double? = null
    ): Result<UpdatePreferenceResponse> {
        return try {
            val profile = PreferenceProfileSnapshot(
                preferredTags = user.preferredTags ?: emptyList(),
                preferenceTagWeights = user.preferenceTagWeights ?: emptyMap(),
                preferenceRoomTypes = user.preferenceRoomTypes ?: emptyList(),
                preferenceEmbedding = emptyList(), // embeddings stay server-side
                lastLat = user.lastLat,
                lastLon = user.lastLon
            )
            val event = PreferenceRoomEvent(
                tags = room.tags ?: emptyList(),
                isPrivate = room.isPrivate,
                isTimeCapsule = room.isTimeCapsule,
                lat = userLat,
                lon = userLon,
                interactionType = interactionType
            )
            val response = api.updateUserPreference(UpdatePreferenceRequest(profile, event, topKTags = 30))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Log.w(TAG, "trackRoomInteraction failed: $err")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "trackRoomInteraction exception", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Personalised Suggestions
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getPersonalisedSuggestions(
        user: UserDto,
        candidateRooms: List<RoomDto>,
        userLat: Double? = null,
        userLon: Double? = null,
        sourceRoomId: String? = null
    ): Result<GenerateSuggestionsResponse> {
        return try {
            if (candidateRooms.isEmpty()) {
                return Result.success(GenerateSuggestionsResponse("discovery", emptyList(), 0))
            }

            val userProfile = UserPreferenceSchema(
                preferredTags = user.preferredTags ?: emptyList(),
                lastLat = userLat ?: user.lastLat,
                lastLon = userLon ?: user.lastLon,
                interactionRoomTypes = user.preferenceRoomTypes ?: emptyList(),
                preferenceEmbedding = emptyList()
            )

            val aiCandidates = candidateRooms.map { it.toAiRoomDto() }

            val request = GenerateSuggestionsRequest(
                sourceRoomId = sourceRoomId,
                userProfile = userProfile,
                candidateRooms = aiCandidates,
                userLat = userLat ?: user.lastLat,
                userLon = userLon ?: user.lastLon
            )

            val response = api.getSuggestions(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Log.w(TAG, "getSuggestions failed: $err")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPersonalisedSuggestions exception", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Raw Hybrid Scoring
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun scoreRoomPair(
        sourceRoom: RoomDto,
        targetRoom: RoomDto,
        sourceInteraction: AiInteractionDto? = null,
        targetInteraction: AiInteractionDto? = null
    ): Result<HybridScoreResponse> {
        return try {
            val request = HybridScoreRequest(
                sourceRoom = sourceRoom.toAiRoomDto(),
                targetRoom = targetRoom.toAiRoomDto(),
                sourceInteraction = sourceInteraction,
                targetInteraction = targetInteraction
            )
            val response = api.scoreRooms(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Score failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "scoreRoomPair exception", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Semantic Search
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun semanticSearch(
        query: String,
        candidateRooms: List<RoomDto>,
        topK: Int = 5
    ): Result<SemanticSearchResponse> {
        return try {
            // coroutineScope ile işlemleri paralel (aynı anda) yaparak hızı artırıyoruz
            coroutineScope {
                // 1. ADIM: Her oda için /v1/embed endpoint'inden "embedding" değerlerini al
                val candidatesWithEmbeddings = candidateRooms.map { room ->
                    async {
                        // Python'un arka planda beklediği formatta metni hazırlıyoruz
                        val tagsText = room.tags?.joinToString(" ") ?: ""
                        val weightedTags = "$tagsText $tagsText".trim()
                        val descText = room.description?.trim() ?: ""
                        val combinedText = "Room name: ${room.name}. Description: $descText. Core tags: $weightedTags."

                        // Embedding'i (1536'lık sayıyı) Python'dan istiyoruz
                        val embedResponse = api.embed(EmbeddingRequest(combinedText))
                        val embeddingList = if (embedResponse.isSuccessful) {
                            embedResponse.body()?.embedding ?: emptyList()
                        } else {
                            emptyList()
                        }

                        // Sunucuya göndereceğimiz tam ve eksiksiz sözlük (dictionary)
                        mapOf<String, Any>(
                            "id" to room.id,
                            "roomId" to room.id,
                            "name" to room.name,
                            "embedding" to embeddingList
                        )
                    }
                }.awaitAll().filter {
                    // Sadece embedding'i başarıyla alınmış odaları listeye koy (Boşları at)
                    @Suppress("UNCHECKED_CAST")
                    (it["embedding"] as List<Float>).isNotEmpty()
                }

                if (candidatesWithEmbeddings.isEmpty()) {
                    return@coroutineScope Result.failure(Exception("Hiçbir oda için embedding oluşturulamadı."))
                }

                // 2. ADIM: Artık elimizde "dolu" embeddingler var! Aramayı başlatabiliriz.
                val request = SemanticSearchRequest(
                    queryText = query,
                    candidateEmbeddings = candidatesWithEmbeddings,
                    topK = topK
                )

                val response = api.semanticSearch(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Semantic search failed: HTTP ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "semanticSearch exception", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Generate Room Description
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun generateRoomDescription(
        roomName: String,
        tags: List<String> = emptyList(),
        isPrivate: Boolean = false,
        isTimeCapsule: Boolean = false
    ): Result<String> {
        return try {
            val request = GenerateDescriptionRequest(roomName, tags, isPrivate, isTimeCapsule)
            val response = api.generateDescription(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.description)
            } else {
                Result.failure(Exception("Description generation failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateRoomDescription exception", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Explicit Preference Update
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun updateUserPreference(
        request: UpdatePreferenceRequest
    ): Result<UpdatePreferenceResponse> {
        return try {
            val response = api.updateUserPreference(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Preference update failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateUserPreference exception", e)
            Result.failure(e)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension: Convert backend RoomDto → AiRoomDto
// ─────────────────────────────────────────────────────────────────────────────

fun RoomDto.toAiRoomDto(): AiRoomDto = AiRoomDto(
    id = this.id,
    name = this.name,
    tags = this.tags ?: emptyList(),
    isPrivate = this.isPrivate,
    isTimeCapsule = this.isTimeCapsule,
    lat = 0.0,
    lon = 0.0,
    description = this.description,
    embedding = null,
    dwellTime = 0
)