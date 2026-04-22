package com.dmb.bestbefore.data.api

import com.dmb.bestbefore.BuildConfig
import com.dmb.bestbefore.data.api.models.RoomDto
import com.dmb.bestbefore.data.api.models.RoomDtoJsonDeserializer
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // --- SERVER URL CONFIGURATION ---
    // Debug: local backend (10.0.2.2), Release: Railway
    internal const val BASE_URL = BuildConfig.API_BASE_URL


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Increase logging to see payloads
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // Temporarily bypassing local AppCheck to resolve attestation failures directly connecting to backend
        // .addInterceptor(AppCheckInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .registerTypeAdapter(RoomDto::class.java, RoomDtoJsonDeserializer())
        .create()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
