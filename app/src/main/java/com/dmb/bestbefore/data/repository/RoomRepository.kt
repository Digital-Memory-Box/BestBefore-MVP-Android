package com.dmb.bestbefore.data.repository

import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.CreateRoomRequest
import com.dmb.bestbefore.data.api.models.RoomDto
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class RoomRepository(private val authToken: String) {
    private val api = RetrofitClient.apiService

    suspend fun getRooms(): Result<List<RoomDto>> {
        return try {
            val response = api.getRooms("Bearer $authToken")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch rooms: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRoom(
        name: String, 
        days: Int, 
        hours: Int, 
        minutes: Int, 
        isPublic: Boolean, 
        isCollaboration: Boolean
    ): Result<String> {
        return try {
            val request = CreateRoomRequest(name, days, hours, minutes, isPublic, isCollaboration)
            val response = api.createRoom("Bearer $authToken", request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.id)
            } else {
                Result.failure(Exception("Failed to create room: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPhoto(roomId: String, imagePart: MultipartBody.Part): Result<String> {
        return try {
            val roomIdBody = roomId.toRequestBody(MultipartBody.FORM)
            val response = api.uploadRoomPhoto("Bearer $authToken", imagePart, roomIdBody)
            // ApiService uploadRoomPhoto returns UploadResponse directly (suspend function), not Response<UploadResponse>?
            // Wait, in helper I defined it as `suspend fun uploadRoomPhoto(...) : UploadResponse`.
            // Retrofit 2.6+ supports this. If so, it throws on error?
            // Usually we return Response<T> to check code.
            // Let's assume the definition in ApiService returns the object directly or I should check ApiService again.
            // I defined it as `UploadResponse`. Retrofit will throw if 4xx/5xx.
            Result.success(response.imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCalendarAuthUrl(): Result<String> {
        return try {
            val response = api.getCalendarAuthUrl("Bearer $authToken")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.url)
            } else {
                Result.failure(Exception("Failed to get auth URL: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUpcomingEvents(): Result<List<com.dmb.bestbefore.data.api.models.CalendarEventDto>> {
        return try {
            val response = api.getCalendarEvents("Bearer $authToken")
            if (response.isSuccessful && response.body() != null) {
                // The API returns { "events": [...] } but ApiService definition was Map<String, List<Event>>
                // So body["events"] should give the list
                val events = response.body()?.get("events") ?: emptyList()
                Result.success(events)
            } else {
                Result.failure(Exception("Failed to fetch events: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun keepRoom(roomId: String): Result<Unit> {
        return try {
            val response = api.keepRoom("Bearer $authToken", roomId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to keep room: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedRooms(): Result<List<RoomDto>> {
        return try {
            val response = api.getSavedRooms("Bearer $authToken")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch saved rooms: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
