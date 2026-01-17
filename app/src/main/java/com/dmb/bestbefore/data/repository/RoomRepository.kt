package com.dmb.bestbefore.data.repository

import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.CreateRoomRequest
import com.dmb.bestbefore.data.api.models.RoomDto

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

    suspend fun createRoom(name: String): Result<String> {
        return try {
            val response = api.createRoom("Bearer $authToken", CreateRoomRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.id)
            } else {
                Result.failure(Exception("Failed to create room: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
