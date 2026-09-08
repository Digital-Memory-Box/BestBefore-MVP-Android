package com.dmb.bestbefore.utils

import android.util.Log
import com.dmb.bestbefore.data.api.ApiService
import com.dmb.bestbefore.data.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Handles cold-start /health warmup checks for the backend server.
 * Railway free tier cold-starts in 20–60s. Fires /health first, and after
 * 2s transitions status to WARMING_UP so the UI can notify the user.
 */
class ServerWarmupHelper(
    private val apiService: ApiService = RetrofitClient.apiService
) {
    companion object {
        private const val TAG_PERF = "PerfTimer"
        const val WARMUP_DELAY_MS = 2_000L
        const val PING_TIMEOUT_MS = 90_000L
    }

    suspend fun performWarmup(
        scope: CoroutineScope,
        isConnecting: () -> Boolean,
        onWarmingUp: () -> Unit,
        onReady: () -> Unit,
        onError: (Exception) -> Unit = {},
        elapsedMs: () -> Long = { 0L }
    ) {
        val warmupJob = scope.launch {
            delay(WARMUP_DELAY_MS)
            if (isConnecting()) {
                onWarmingUp()
                Log.i(TAG_PERF, "[${elapsedMs()}ms] server still cold — showing warm-up message")
            }
        }
        val pingT0 = System.currentTimeMillis()
        try {
            val pingResult = withTimeoutOrNull(PING_TIMEOUT_MS) {
                apiService.health()
            }
            val pingMs = System.currentTimeMillis() - pingT0
            if (pingResult?.isSuccessful == true) {
                Log.i(TAG_PERF, "[${elapsedMs()}ms] /health OK in ${pingMs}ms — server is warm ✅")
            } else {
                Log.w(TAG_PERF, "[${elapsedMs()}ms] /health ${pingResult?.code()} in ${pingMs}ms — proceeding anyway")
            }
        } catch (e: Exception) {
            Log.e(TAG_PERF, "[${elapsedMs()}ms] /health exception in ${System.currentTimeMillis() - pingT0}ms: ${e.message}")
            onError(e)
        } finally {
            warmupJob.cancel()
            onReady()
        }
    }
}
