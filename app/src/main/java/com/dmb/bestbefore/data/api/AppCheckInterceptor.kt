package com.dmb.bestbefore.data.api

import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log

class AppCheckInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 1. Get the App Check token
        // usage of runBlocking is necessary here because Interceptor is synchronous
        // and getAppCheckToken is async.
        // We use 'false' to avoid forcing a refresh unless necessary.
        val token = try {
            runBlocking {
                val appCheck = FirebaseAppCheck.getInstance()
                val tokenResult = appCheck.getAppCheckToken(false).await()
                tokenResult.token
            }
        } catch (e: Exception) {
            // Log error but proceed without token - let the backend decide
            Log.e("AppCheckInterceptor", "Error getting App Check token", e)
             ""
        }

        // 2. Add header if token exists
        val newRequest = if (token.isNotEmpty()) {
            originalRequest.newBuilder()
                .header("X-Firebase-AppCheck", token)
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
