package com.dmb.bestbefore.data.repository

import android.util.Log
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.CreateRoomRequest
import com.dmb.bestbefore.data.api.models.RoomDto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * RoomRepository — mirrors the iOS BackendAPIClient pattern.
 *
 * KEY CHANGE: Instead of storing a token at construction time (which expires),
 * every public function calls [freshBearer] to get a brand-new Firebase ID token
 * before making its request. This matches exactly how the working iOS project
 * handles auth (see Database.swift / BackendAPIClient.authorizedRequest).
 */
class RoomRepository {

    private val api = RetrofitClient.apiService

    /** Always returns "Bearer <fresh-firebase-id-token>" — throws if not signed in. */
    private suspend fun freshBearer(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User not signed in")
        val token = user.getIdToken(false).await().token
            ?: throw IllegalStateException("Could not obtain Firebase ID token")
        Log.d("RoomRepository", "Token fetched (uid=${user.uid}, len=${token.length})")
        return "Bearer $token"
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────

    suspend fun getRooms(): Result<List<RoomDto>> {
        return try {
            val response = api.getRooms(freshBearer())
            Log.d("RoomRepository", "getRooms → ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Log.e("RoomRepository", "getRooms failed: $err")
                Result.failure(Exception("Failed to fetch rooms: $err"))
            }
        } catch (e: Exception) {
            Log.e("RoomRepository", "getRooms exception", e)
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a room. isPublic → isPrivate=false on backend (matches iOS createRoom).
     * Returns the new room's MongoDB _id on success.
     */
    suspend fun createRoom(
        name: String,
        days: Int,
        hours: Int,
        minutes: Int,
        isPublic: Boolean,
        isTimeCapsule: Boolean,
        theme: String,
        collaborators: List<String> = emptyList(),
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
                unlockDate = unlockDateIso,
                expirationDate = scheduledClosureIso,
                rollingExpiryDays = rollingExpiryDays,
                description = description,
                tags = tags,
                backgroundMusic = music
            )
            val response = api.createRoom(freshBearer(), request)
            Log.d("RoomRepository", "createRoom → ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.id)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Log.e("RoomRepository", "createRoom failed: $err")
                Result.failure(Exception("Failed to create room: $err"))
            }
        } catch (e: Exception) {
            Log.e("RoomRepository", "createRoom exception", e)
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

    // ── Memories ──────────────────────────────────────────────────────────────

    suspend fun getMemoriesByRoom(roomId: String): Result<List<Map<String, Any>>> {
        return try {
            val response = api.getMemoriesByRoom(freshBearer(), roomId)
            Log.d("RoomRepository", "getMemoriesByRoom($roomId) → ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                @Suppress("UNCHECKED_CAST")
                Result.success(response.body()!! as List<Map<String, Any>>)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Log.e("RoomRepository", "getMemoriesByRoom failed: $err")
                Result.failure(Exception("Failed to fetch memories: $err"))
            }
        } catch (e: Exception) {
            Log.e("RoomRepository", "getMemoriesByRoom exception", e)
            Result.failure(e)
        }
    }

    /** Mirrors iOS Database.addMemory — posts type/title/content to /rooms/{id}/memories */
    suspend fun addMemoryToRoom(roomId: String, memoryData: Map<String, Any>): Result<Unit> {
        return try {
            val response = api.addMemoryToRoom(freshBearer(), roomId, memoryData)
            Log.d("RoomRepository", "addMemoryToRoom($roomId) → ${response.code()}")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Log.e("RoomRepository", "addMemoryToRoom failed: $err")
                Result.failure(Exception("Failed to add memory: $err"))
            }
        } catch (e: Exception) {
            Log.e("RoomRepository", "addMemoryToRoom exception", e)
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
}
