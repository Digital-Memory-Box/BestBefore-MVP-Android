package com.dmb.bestbefore.utils

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * OkHttp interceptor that logs request duration and response size.
 *
 * Memory endpoints can return large inline media payloads, so they are logged
 * without buffering the body. This keeps the raw ResponseBody stream intact for
 * Retrofit and avoids adding client-side delay to an already heavy request.
 */
class PerfLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val label = "${request.method} ${request.url.encodedPath}"

        Log.i(TAG, "->  $label")
        val t0 = System.currentTimeMillis()

        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            val ms = System.currentTimeMillis() - t0
            val message = "x  $label  |  ${ms} ms  |  ${e.javaClass.simpleName}: ${e.message}"
            when {
                e is IOException && e.message.equals("Canceled", ignoreCase = true) -> Log.d(TAG, message)
                e is IOException -> Log.w(TAG, message)
                else -> Log.e(TAG, message)
            }
            throw e
        }

        val ms = System.currentTimeMillis() - t0
        val speedFlag = speedFlag(ms)

        if (shouldSkipBodyInspection(request.url.encodedPath)) {
            val contentLength = response.body?.contentLength() ?: -1L
            val sizeLabel = if (contentLength >= 0L) formatSize(contentLength) else "streaming"
            Log.i(TAG, "<-  $label  $speedFlag ${response.code}  |  ${ms} ms  |  $sizeLabel")
            return response
        }

        val contentType = response.body?.contentType()
        val rawBytes = try {
            response.body?.bytes() ?: ByteArray(0)
        } catch (e: Exception) {
            Log.w(TAG, "<-  $label  body read failed after ${ms} ms: ${e.message}")
            return response
        }

        val sizeBytes = rawBytes.size
        val sizeLabel = formatSize(sizeBytes.toLong())
        val bodyPreview = rawBytes.decodeToString().take(8_192)
        val base64PhotoCount = countOccurrences(bodyPreview, "data:image")
        val base64Warn = if (base64PhotoCount > 0) {
            "  BASE64 PHOTOS x$base64PhotoCount (likely large payload)"
        } else {
            ""
        }
        val largePayloadWarn = if (sizeBytes > 200_000) "  LARGE PAYLOAD" else ""

        Log.i(TAG, "<-  $label  $speedFlag ${response.code}  |  ${ms} ms  |  $sizeLabel$largePayloadWarn$base64Warn")

        if (sizeBytes > 100_000) {
            val arrayItemCount = countOccurrences(bodyPreview.take(2_048), "\"_id\"")
            if (arrayItemCount > 0) {
                Log.i(TAG, "    array-item estimate: ~$arrayItemCount items in first 2 KB of body")
            }
            Log.i(TAG, "    payload breakdown: Content-Type=${contentType?.type}/${contentType?.subtype}")
        }

        val rebuiltBody = rawBytes.toResponseBody(contentType)
        return response.newBuilder().body(rebuiltBody).build()
    }

    private fun shouldSkipBodyInspection(path: String): Boolean {
        return path.contains("/memories")
    }

    private fun formatSize(sizeBytes: Long): String {
        return when {
            sizeBytes >= 1_048_576 -> "%.2f MB".format(sizeBytes / 1_048_576.0)
            sizeBytes >= 1_024 -> "%.1f KB".format(sizeBytes / 1_024.0)
            else -> "$sizeBytes B"
        }
    }

    private fun speedFlag(ms: Long): String {
        return when {
            ms > 2_000 -> "SLOW"
            ms > 500 -> "OK"
            else -> "FAST"
        }
    }

    private fun countOccurrences(text: String, target: String): Int {
        if (target.isEmpty()) return 0
        var count = 0
        var idx = 0
        while (true) {
            idx = text.indexOf(target, idx)
            if (idx == -1) break
            count++
            idx += target.length
        }
        return count
    }

    companion object {
        const val TAG = "BB_PERF"
    }
}
