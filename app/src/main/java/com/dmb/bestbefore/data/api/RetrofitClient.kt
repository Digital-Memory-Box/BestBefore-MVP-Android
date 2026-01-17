package com.dmb.bestbefore.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // --- SERVER URL CONFIGURATION ---
    
    // 1. Production (Render) - Active
    private const val BASE_URL = "https://backend-test-1p44.onrender.com/" 

    // 2. Local Emulator (Uncomment to use for local testing)
    // private const val BASE_URL = "http://10.0.2.2:3000/"
    
    // 3. Local Physical Device (Uncomment and set your IP)
    // private const val BASE_URL = "http://192.168.1.35:3000/" 

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
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
