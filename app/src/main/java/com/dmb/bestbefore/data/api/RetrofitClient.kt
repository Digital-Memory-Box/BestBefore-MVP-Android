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
    internal const val SECONDARY_BASE_URL = "https://bestbefore-ai.up.railway.app/"


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE // Increase logging to see payloads
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

    private fun createApiService(baseUrl: String): ApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    val apiService: ApiService by lazy {
        createApiService(BASE_URL)
    }

    fun apiServiceForBaseUrl(baseUrl: String): ApiService {
        return createApiService(baseUrl)
    }
}
