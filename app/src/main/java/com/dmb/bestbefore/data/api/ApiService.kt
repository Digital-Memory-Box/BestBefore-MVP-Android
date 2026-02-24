package com.dmb.bestbefore.data.api

import com.dmb.bestbefore.data.api.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ApiService {
    @POST("signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @Multipart
    @POST("upload/room-photo")
    suspend fun uploadRoomPhoto(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("roomId") roomId: RequestBody
    ): UploadResponse

    @GET("rooms")
    suspend fun getRooms(@Header("Authorization") token: String): Response<List<RoomDto>>

    @POST("rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body request: CreateRoomRequest
    ): Response<CreateRoomResponse>

    @GET("calendar/auth")
    suspend fun getCalendarAuthUrl(@Header("Authorization") token: String): Response<CalendarAuthResponse>

    @GET("calendar/events")
    suspend fun getCalendarEvents(@Header("Authorization") token: String): Response<Map<String, List<CalendarEventDto>>>

    @POST("rooms/{roomId}/keep")
    suspend fun keepRoom(
        @Header("Authorization") token: String,
        @retrofit2.http.Path("roomId") roomId: String
    ): Response<Unit>

    @retrofit2.http.PATCH("user/email")
    suspend fun updateUserEmail(
        @Body request: Map<String, String>
    ): Response<Unit>

    @retrofit2.http.PATCH("user/password")
    suspend fun updateUserPassword(
        @Body request: Map<String, String>
    ): Response<Unit>

    @GET("rooms/saved")
    suspend fun getSavedRooms(@Header("Authorization") token: String): Response<List<RoomDto>>
}
