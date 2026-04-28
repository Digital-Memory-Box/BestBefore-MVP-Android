package com.dmb.bestbefore.data.ai

import android.util.Log
import com.dmb.bestbefore.data.api.models.RoomDto
import com.dmb.bestbefore.data.api.models.UserDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private companion object {
        const val MAX_SUGGESTION_CANDIDATES = 40
    }

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
        sourceRoomId: String? = null,
        sourceRoom: RoomDto? = null
    ): Result<GenerateSuggestionsResponse> {
        return withContext(Dispatchers.IO) {
            try {
            if (candidateRooms.isEmpty()) {
                return@withContext Result.success(GenerateSuggestionsResponse("discovery", emptyList(), 0))
            }

            val resolvedSourceRoom = sourceRoom ?: sourceRoomId?.let { id ->
                candidateRooms.firstOrNull { it.id == id }
            }
            val rankedCandidateRooms = rankSuggestionCandidates(
                sourceRoom = resolvedSourceRoom,
                user = user,
                rooms = candidateRooms
                    .filter { it.id != sourceRoomId }
                    .distinctBy { it.id }
            ).take(MAX_SUGGESTION_CANDIDATES)

            if (rankedCandidateRooms.isEmpty()) {
                return@withContext Result.success(
                    GenerateSuggestionsResponse(sourceRoomId ?: "discovery", emptyList(), 0)
                )
            }

            // Tıpkı Arama (Semantic Search) fonksiyonunda yaptığımız gibi odaları hazırla
            coroutineScope {
                val aiSourceRoom = resolvedSourceRoom?.let { room ->
                    async { room.toAiRoomDtoWithEmbedding() }
                }
                val aiCandidates = rankedCandidateRooms.map { room ->
                    async { room.toAiRoomDtoWithEmbedding() }
                }.awaitAll()

                val userProfile = UserPreferenceSchema(
                    preferredTags = user.preferredTags ?: emptyList(),
                    lastLat = userLat ?: user.lastLat,
                    lastLon = userLon ?: user.lastLon,
                    interactionRoomTypes = user.preferenceRoomTypes ?: emptyList(),
                    preferenceEmbedding = emptyList()
                )

                val request = GenerateSuggestionsRequest(
                    sourceRoomId = sourceRoomId,
                    sourceRoom = aiSourceRoom?.await(),
                    userProfile = userProfile,
                    candidateRooms = aiCandidates,
                    userLat = userLat ?: user.lastLat,
                    userLon = userLon ?: user.lastLon
                )

                val response = api.getSuggestions(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.suggestions.isNotEmpty()) {
                        Result.success(body)
                    } else {
                        Result.success(
                            buildLocalSuggestionResponse(sourceRoomId, resolvedSourceRoom, rankedCandidateRooms, user)
                        )
                    }
                } else {
                    val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                    Log.w(TAG, "getPersonalisedSuggestions failed: $err")
                    Result.success(
                        buildLocalSuggestionResponse(sourceRoomId, resolvedSourceRoom, rankedCandidateRooms, user)
                    )
                }
            }
            } catch (e: Exception) {
                Log.e(TAG, "getPersonalisedSuggestions exception", e)
                Result.success(buildLocalSuggestionResponse(sourceRoomId, sourceRoom, candidateRooms, user))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Raw Hybrid Scoring
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun RoomDto.toAiRoomDtoWithEmbedding(): AiRoomDto {
        val embeddingList = try {
            val embedResponse = api.embed(EmbeddingRequest(buildRoomEmbeddingText(this)))
            if (embedResponse.isSuccessful) {
                embedResponse.body()?.embedding.orEmpty()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Embedding skipped for room $id: ${e.message}")
            emptyList()
        }

        return AiRoomDto(
            id = id,
            name = name,
            tags = tags ?: emptyList(),
            isPrivate = isPrivate,
            isTimeCapsule = isTimeCapsule,
            lat = 0.0,
            lon = 0.0,
            description = description ?: generatedDescription,
            embedding = embeddingList.takeIf { it.isNotEmpty() },
            dwellTime = 0
        )
    }

    private fun buildRoomEmbeddingText(room: RoomDto): String {
        val tagsText = room.tags?.joinToString(" ").orEmpty()
        val weightedTags = "$tagsText $tagsText".trim()
        val descText = (room.description ?: room.generatedDescription).orEmpty().trim()
        return "Room name: ${room.name}. Description: $descText. Core tags: $weightedTags."
    }

    private fun buildLocalSuggestionResponse(
        sourceRoomId: String?,
        sourceRoom: RoomDto?,
        candidateRooms: List<RoomDto>,
        user: UserDto
    ): GenerateSuggestionsResponse {
        val ranked = rankSuggestionCandidates(sourceRoom, user, candidateRooms)
            .filter { it.id != sourceRoomId }
            .take(12)
            .map { room ->
                val score = localSuggestionScore(sourceRoom, user, room).coerceIn(1, 100)
                AiRoomSuggestion(
                    targetRoomId = room.id,
                    targetRoomName = room.name,
                    score = score,
                    category = if (score >= 70) "direct_match" else "critical_zone",
                    reasoning = buildLocalReason(sourceRoom, user, room),
                    similarity = score / 100.0
                )
            }

        return GenerateSuggestionsResponse(
            sourceRoomId = sourceRoomId ?: sourceRoom?.id ?: "discovery",
            suggestions = ranked,
            count = ranked.size
        )
    }

    private fun rankSuggestionCandidates(
        sourceRoom: RoomDto?,
        user: UserDto,
        rooms: List<RoomDto>
    ): List<RoomDto> {
        return rooms
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .sortedByDescending { localSuggestionScore(sourceRoom, user, it) }
    }

    private fun localSuggestionScore(
        sourceRoom: RoomDto?,
        user: UserDto,
        candidate: RoomDto
    ): Int {
        val sourceTags = sourceRoom?.tags.orEmpty().map { it.normalizedToken() }.filter { it.isNotBlank() }.toSet()
        val candidateTags = candidate.tags.orEmpty().map { it.normalizedToken() }.filter { it.isNotBlank() }.toSet()
        val preferredTags = user.preferredTags.orEmpty().map { it.normalizedToken() }.filter { it.isNotBlank() }.toSet()
        val sourceWords = tokenizeRoom(sourceRoom)
        val candidateWords = tokenizeRoom(candidate)

        val sharedTagScore = sourceTags.intersect(candidateTags).size * 24
        val preferredTagScore = preferredTags.intersect(candidateTags).size * 10
        val sharedWordScore = sourceWords.intersect(candidateWords).size * 4
        val timeCapsuleScore = if (sourceRoom != null && sourceRoom.isTimeCapsule == candidate.isTimeCapsule) 8 else 0
        val privacyScore = if (sourceRoom != null && sourceRoom.isPrivate == candidate.isPrivate) 4 else 0

        return (18 + sharedTagScore + preferredTagScore + sharedWordScore + timeCapsuleScore + privacyScore)
            .coerceIn(1, 100)
    }

    private fun buildLocalReason(sourceRoom: RoomDto?, user: UserDto, candidate: RoomDto): String {
        val sourceTags = sourceRoom?.tags.orEmpty().map { it.normalizedToken() }.filter { it.isNotBlank() }.toSet()
        val candidateTags = candidate.tags.orEmpty().map { it.normalizedToken() }.filter { it.isNotBlank() }.toSet()
        val preferredTags = user.preferredTags.orEmpty().map { it.normalizedToken() }.filter { it.isNotBlank() }.toSet()
        val sharedTags = sourceTags.intersect(candidateTags).take(3)
        val preferenceMatches = preferredTags.intersect(candidateTags).take(2)

        return when {
            sharedTags.isNotEmpty() -> "Shared tags: ${sharedTags.joinToString(", ")}"
            preferenceMatches.isNotEmpty() -> "Matches your interests: ${preferenceMatches.joinToString(", ")}"
            else -> "Similar room details and activity context."
        }
    }

    private fun tokenizeRoom(room: RoomDto?): Set<String> {
        if (room == null) return emptySet()
        return "${room.name} ${room.description.orEmpty()} ${room.generatedDescription.orEmpty()} ${room.tags.orEmpty().joinToString(" ")}"
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .toSet()
    }

    private fun String.normalizedToken(): String = trim().lowercase()

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
            // Python'a gidecek paketi hazırlıyoruz
            val request = GenerateDescriptionRequest(
                roomName = roomName,
                tags = tags,
                isPrivate = isPrivate,
                isTimeCapsule = isTimeCapsule
            )

            Log.d(TAG, "AI Description request sending: $roomName")

            val response = api.generateDescription(request)

            if (response.isSuccessful && response.body() != null) {
                val description = response.body()!!.description
                Log.d(TAG, "AI Description successfully get: $description")
                Result.success(description)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "AI Description error (HTTP ${response.code()}): $errorMsg")
                Result.failure(Exception("Description generation failed: $errorMsg"))
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
