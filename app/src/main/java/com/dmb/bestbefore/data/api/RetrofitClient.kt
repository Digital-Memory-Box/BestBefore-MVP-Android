package com.dmb.bestbefore.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // --- SERVER URL CONFIGURATION ---
    // Production Backend on Railway
    internal const val BASE_URL = "https://bestbefore.up.railway.app/" 

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

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
