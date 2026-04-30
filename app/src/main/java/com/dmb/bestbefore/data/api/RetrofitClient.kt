package com.dmb.bestbefore.data.api

import com.dmb.bestbefore.BuildConfig
import com.dmb.bestbefore.data.api.models.RoomDto
import com.dmb.bestbefore.data.api.models.RoomDtoJsonDeserializer
import com.dmb.bestbefore.utils.PerfLoggingInterceptor
import com.google.gson.GsonBuilder
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    internal const val BASE_URL = BuildConfig.API_BASE_URL
    internal const val SECONDARY_BASE_URL = "https://bestbefore-ai.up.railway.app/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Only log in debug builds — logging headers on every request adds ~1ms per call in release.
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
    }

    // PerfLoggingInterceptor goes first so it sees the raw response bytes before
    // any other interceptor transforms them.  It buffers + re-emits the body so
    // Retrofit can still parse it normally.
    private val perfInterceptor = PerfLoggingInterceptor()
    private val appErrorInterceptor = AppErrorInterceptor()

    private val client = OkHttpClient.Builder()
        .addInterceptor(appErrorInterceptor)
        .addInterceptor(perfInterceptor)
        .addInterceptor(loggingInterceptor)
        // Sensible timeouts: connect fast, allow time for large responses, no global call cap
        // that blocks the dispatcher thread pool.
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // Keep 5 connections alive for up to 60 s — reuses TCP/TLS for repeat requests to
        // the same host (avoids 3-way handshake + TLS on every room fetch).
        .connectionPool(ConnectionPool(5, 60, TimeUnit.SECONDS))
        // HTTP/1.1 keep-alive is the default; this also works with HTTP/2 multiplexing if
        // the Railway backend supports it.
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
