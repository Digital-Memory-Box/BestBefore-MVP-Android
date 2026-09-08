package com.dmb.bestbefore.data.api

import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Automatically injects Firebase Bearer token into requests
 * that don't already have an Authorization header.
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip if Authorization header is already set
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }
        
        // Skip health check and public endpoints
        val path = originalRequest.url.encodedPath
        if (path == "/health") {
            return chain.proceed(originalRequest)
        }
        
        val token = try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                com.google.android.gms.tasks.Tasks.await(
                    user.getIdToken(false),
                    5,
                    java.util.concurrent.TimeUnit.SECONDS
                )?.token
            } else null
        } catch (e: Exception) {
            null
        }
        
        if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            return chain.proceed(newRequest)
        }
        
        return chain.proceed(originalRequest)
    }
}
