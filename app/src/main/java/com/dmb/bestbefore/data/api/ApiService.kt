package com.dmb.bestbefore.data.api

import com.dmb.bestbefore.data.api.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("me")
    suspend fun getMe(@Header("Authorization") token: String): Response<Map<String, UserDto>>

    @GET("rooms")
    suspend fun getRooms(@Header("Authorization") token: String): Response<List<RoomDto>>

    @POST("rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body request: CreateRoomRequest
    ): Response<CreateRoomResponse>
}
