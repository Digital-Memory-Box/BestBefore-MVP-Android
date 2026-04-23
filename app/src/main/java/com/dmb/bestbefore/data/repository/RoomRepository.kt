package com.dmb.bestbefore.data.repository

import android.util.Log
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * RoomRepository — handles Room data and dynamic analytics/recommendations.
 * Restored with all original functionality plus new recommendation features.
 */
class RoomRepository {

    private val api = RetrofitClient.apiService

    /** Always returns "Bearer <fresh-firebase-id-token>" — throws if not signed in. */
    private suspend fun freshBearer(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User not signed in")
        val token = user.getIdToken(false).await().token
            ?: throw IllegalStateException("Could not obtain Firebase ID token")
        return "Bearer $token"
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────

    suspend fun getRooms(): Result<List<RoomDto>> {
        return try {
            val response = api.getRooms(freshBearer())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception("Failed to fetch rooms: $err"))
            }
        } catch (e: IOException) {
            tryFallbackRoomsRequest(
                endpointName = "getRooms",
                request = { service, bearer -> service.getRooms(bearer) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDiscoverRooms(): Result<List<RoomDto>> {
        return try {
            val response = api.getDiscoverRooms(freshBearer())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to discover rooms: ${response.code()}"))
            }
        } catch (e: IOException) {
            tryFallbackRoomsRequest(
                endpointName = "getDiscoverRooms",
                request = { service, bearer -> service.getDiscoverRooms(bearer) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun tryFallbackRoomsRequest(
        endpointName: String,
        request: suspend (com.dmb.bestbefore.data.api.ApiService, String) -> retrofit2.Response<List<RoomDto>>
    ): Result<List<RoomDto>> {
        return try {
            val bearer = freshBearer()
            val fallbackService = RetrofitClient.apiServiceForBaseUrl(RetrofitClient.SECONDARY_BASE_URL)
            val fallbackResponse = request(fallbackService, bearer)
            if (fallbackResponse.isSuccessful && fallbackResponse.body() != null) {
                Log.w("RoomRepository", "$endpointName primary host failed, fallback host succeeded")
                Result.success(fallbackResponse.body()!!)
            } else {
                val err = fallbackResponse.errorBody()?.string() ?: "HTTP ${fallbackResponse.code()}"
                Result.failure(Exception("$endpointName failed on fallback host: $err"))
            }
        } catch (fallbackError: Exception) {
            Result.failure(fallbackError)
        }
    }

    suspend fun createRoom(
        name: String,
        days: Int,
        hours: Int,
        minutes: Int,
        isPublic: Boolean,
        isTimeCapsule: Boolean,
        theme: String,
        collaborators: List<String> = emptyList(),
        viewers: List<String> = emptyList(),
        scheduledClosureIso: String? = null,
        unlockDateIso: String? = null,
        rollingExpiryDays: Int = 0,
        description: String? = null,
        tags: List<String> = emptyList(),
        music: String = "None"
    ): Result<String> {
        return try {
            val request = CreateRoomRequest(
                name = name,
                isPrivate = !isPublic,
                isTimeCapsule = isTimeCapsule,
                capsuleDurationDays = days,
                capsuleDurationHours = hours,
                capsuleDurationMinutes = minutes,
                theme = theme,
                collaborators = collaborators,
                viewers = viewers,
                unlockDate = unlockDateIso,
                expirationDate = scheduledClosureIso,
                rollingExpiryDays = rollingExpiryDays,
                description = description,
                tags = tags,
                backgroundMusic = music
            )
            val response = api.createRoom(freshBearer(), request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.id)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception("Failed to create room: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRoom(roomId: String, fields: Map<String, Any>): Result<Unit> {
        return try {
            val response = api.updateRoom(freshBearer(), roomId, fields)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to update room: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(roomId: String): Result<Unit> {
        return try {
            val response = api.deleteRoom(freshBearer(), roomId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to delete room: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvite(roomId: String): Result<Unit> {
        return try {
            val response = api.acceptInvite(freshBearer(), roomId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to accept invite: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineInvite(roomId: String): Result<Unit> {
        return try {
            val response = api.declineInvite(freshBearer(), roomId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to decline invite: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveJoinRequest(roomId: String, requesterEmail: String): Result<Unit> {
        return try {
            val body = mapOf<String, Any>("requesterEmail" to requesterEmail)
            val response = api.approveJoinRequest(freshBearer(), roomId, body)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to approve join request: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun denyJoinRequest(roomId: String, requesterEmail: String): Result<Unit> {
        return try {
            val body = mapOf<String, Any>("requesterEmail" to requesterEmail)
            val response = api.denyJoinRequest(freshBearer(), roomId, body)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to deny join request: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Memories ──────────────────────────────────────────────────────────────

    suspend fun getMemoriesByRoom(roomId: String): Result<List<Map<String, Any>>> {
        return try {
            val response = api.getMemoriesByRoomRaw(freshBearer(), roomId)
            if (response.isSuccessful && response.body() != null) {
                val reader = response.body()!!.charStream()
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val body: List<Map<String, Any>> = Gson().fromJson(reader, listType)
                
                android.util.Log.d("RoomRepository", "Successfully fetched ${body.size} memories via stream. Status: ${response.code()}")
                Result.success(body)
            } else {
                android.util.Log.e("RoomRepository", "Failed to fetch memories. Status: ${response.code()}, Error: ${response.errorBody()?.string()}")
                Result.failure(Exception("Failed to fetch memories: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("RoomRepository", "Exception fetching memories via stream", e)
            Result.failure(e)
        }
    }

    suspend fun addMemoryToRoom(roomId: String, memoryData: Map<String, Any>): Result<Unit> {
        return try {
            val response = api.addMemoryToRoom(freshBearer(), roomId, memoryData)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to add memory: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dumpMemories(roomId: String): Result<Unit> {
        return try {
            val response = api.dumpMemories(freshBearer(), roomId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to dump memories: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Recommendations & Analytics ──────────────────────────────────────────

    suspend fun trackInteraction(
        roomId: String,
        dwellTimeSeconds: Long,
        type: String? = null,
        lat: Double? = null,
        lon: Double? = null
    ): Result<Unit> {
        return try {
            val request = RoomInteractionRequest(
                roomId = roomId,
                dwellTimeSeconds = dwellTimeSeconds,
                interactionType = type,
                latitude = lat,
                longitude = lon
            )
            val response = api.trackInteraction(freshBearer(), request)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Analytics track failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPersonalizedRecommendations(): Result<List<RoomRecommendationDto>> {
        return try {
            val response = api.getPersonalizedRecommendations(freshBearer())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch recommendations: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoomSuggestions(roomId: String): Result<RoomSuggestionsResponse> {
        return try {
            val response = api.getRoomSuggestions(freshBearer(), roomId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch suggestions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptSuggestion(roomId: String, suggestionId: String): Result<Unit> {
        return try {
            val response = api.acceptSuggestion(freshBearer(), roomId, suggestionId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to accept suggestion: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Invite Tokens ────────────────────────────────────────────────────────
    suspend fun generateInviteToken(roomId: String, expiresInHours: Int? = null, maxUses: Int? = null): Result<Map<String, Any>> {
        return try {
            val body = mapOf<String, Any?>("expiresInHours" to expiresInHours, "maxUses" to maxUses)
            val response = api.generateInviteToken(freshBearer(), roomId, body)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Failed to generate invite token: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinViaInviteToken(inviteToken: String): Result<Map<String, Any>> {
        return try {
            val response = api.joinViaInviteToken(freshBearer(), inviteToken)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Join failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── QR Code Join ─────────────────────────────────────────────────────────
    suspend fun joinViaQR(roomId: String): Result<Map<String, Any>> {
        return try {
            val response = api.joinViaQR(freshBearer(), roomId)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception("QR join failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoomQRCode(roomId: String): Result<String> {
        return try {
            val response = api.getRoomQRCode(freshBearer(), roomId)
            if (response.isSuccessful && response.body() != null) {
                val qrCode = response.body()!!["qrCode"] as? String
                    ?: return Result.failure(Exception("No qrCode in response"))
                Result.success(qrCode)
            } else {
                Result.failure(Exception("Failed to get QR: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Handshake (Email) Invites ─────────────────────────────────────────────
    suspend fun createHandshakeInvite(roomId: String, inviteeEmail: String): Result<Map<String, Any>> {
        return try {
            val body = mapOf<String, Any>("inviteeEmail" to inviteeEmail)
            val response = api.createHandshakeInvite(freshBearer(), roomId, body)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception("Failed to create handshake invite: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<UserDto>> {
        return try {
            val response = api.searchUsers(freshBearer(), query)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to search users: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

