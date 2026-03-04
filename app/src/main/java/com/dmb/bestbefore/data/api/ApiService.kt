package com.dmb.bestbefore.data.api

import com.dmb.bestbefore.data.api.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────────────
    // POST /auth/sync — verify Firebase token, find-or-create MongoDB user
    @POST("auth/sync")
    suspend fun syncAuth(
        @Header("Authorization") token: String
    ): Response<SyncAuthResponse>

    // GET /auth/me — fetch current user profile
    @GET("auth/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<SyncAuthResponse>

    // PATCH /auth/me — update user profile fields
    @PATCH("auth/me")
    suspend fun updateMe(
        @Header("Authorization") token: String,
        @Body request: UpdateMeRequest
    ): Response<SyncAuthResponse>

    // ── Rooms ─────────────────────────────────────────────────────────────────
    @GET("rooms")
    suspend fun getRooms(
        @Header("Authorization") token: String
    ): Response<List<RoomDto>>

    @GET("rooms/discover")
    suspend fun getDiscoverRooms(
        @Header("Authorization") token: String
    ): Response<List<RoomDto>>

    @POST("rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body request: CreateRoomRequest
    ): Response<CreateRoomResponse>

    @PATCH("rooms/{id}")
    suspend fun updateRoom(
        @Header("Authorization") token: String,
        @Path("id") roomId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(
        @Header("Authorization") token: String,
        @Path("id") roomId: String
    ): Response<Unit>

    @POST("rooms/{id}/accept-invite")
    suspend fun acceptInvite(
        @Header("Authorization") token: String,
        @Path("id") roomId: String
    ): Response<Unit>

    @POST("rooms/{id}/decline-invite")
    suspend fun declineInvite(
        @Header("Authorization") token: String,
        @Path("id") roomId: String
    ): Response<Unit>

    // ── Memories ──────────────────────────────────────────────────────────────
    @GET("rooms/{roomId}/memories")
    suspend fun getMemoriesByRoom(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String
    ): Response<List<Map<String, @JvmSuppressWildcards Any>>>

    @GET("rooms/{roomId}/memories/archived")
    suspend fun getArchivedMemoriesByRoom(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String
    ): Response<List<Map<String, @JvmSuppressWildcards Any>>>

    @POST("rooms/{roomId}/memories")
    suspend fun addMemoryToRoom(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @POST("rooms/{roomId}/dump")
    suspend fun dumpMemories(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String
    ): Response<Unit>

    @GET("memories/trending")
    suspend fun getTrendingMemories(
        @Header("Authorization") token: String
    ): Response<List<Map<String, @JvmSuppressWildcards Any>>>

    @GET("memories/count")
    suspend fun getMemoryCount(
        @Header("Authorization") token: String
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    // ── Upload ────────────────────────────────────────────────────────────────
    @Multipart
    @POST("upload/room-photo")
    suspend fun uploadRoomPhoto(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("roomId") roomId: RequestBody
    ): UploadResponse
}
