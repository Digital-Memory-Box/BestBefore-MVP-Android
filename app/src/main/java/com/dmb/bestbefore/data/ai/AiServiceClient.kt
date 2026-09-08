package com.dmb.bestbefore.data.ai

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client targeting the BestBefore AI microservice.
 * Base URL: https://bestbefore-ai.up.railway.app/
 */
object AiServiceClient {

    private val AI_BASE_URL = com.dmb.bestbefore.BuildConfig.AI_BASE_URL

    private val httpClient = com.dmb.bestbefore.data.api.RetrofitClient.okHttpClient.newBuilder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val api: AiServiceApi by lazy {
        Retrofit.Builder()
            .baseUrl(AI_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiServiceApi::class.java)
    }
}
