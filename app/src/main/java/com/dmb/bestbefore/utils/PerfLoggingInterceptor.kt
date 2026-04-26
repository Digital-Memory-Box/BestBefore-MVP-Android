package com.dmb.bestbefore.utils

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * OkHttp interceptor that measures wall-clock time and actual response size for
 * every network call.  Tag = "BB_PERF".
 *
 * Output format (one line per request, one per response):
 *
 *   BB_PERF →  GET /rooms
 *   BB_PERF ←  GET /rooms  🔴 SLOW   200 | 12 450 ms | 1 247 KB  ⚠ LARGE PAYLOAD
 *              ↑ flag:  🟢 <500ms  🟡 500–2000ms  🔴 >2000ms
 *
 * Additionally, if a response body contains "data:image" strings (base64 photos
 * embedded directly in the JSON), a ⚠ BASE64 PHOTOS warning is printed so you can
 * immediately spot the root cause of large payloads.
 *
 * NOTE: The body is fully buffered to measure its size — this is intentional for
 * diagnosis. Remove or gate behind BuildConfig.DEBUG in production once the
 * bottleneck is confirmed.
 */
class PerfLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val label = "${request.method} ${request.url.encodedPath}"

        Log.i(TAG, "→  $label")
        val t0 = System.currentTimeMillis()

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val ms = System.currentTimeMillis() - t0
            Log.e(TAG, "✗  $label  |  ${ms} ms  |  ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }

        val ms = System.currentTimeMillis() - t0

        // ── Buffer the full body so we can (a) measure size and (b) inspect content ──
        val contentType = response.body?.contentType()
        val rawBytes: ByteArray = try {
            response.body?.bytes() ?: ByteArray(0)
        } catch (e: Exception) {
            Log.e(TAG, "←  $label  BODY-READ-ERROR after ${ms} ms: ${e.message}")
            // Return original response unchanged so Retrofit still works
            return response
        }

        val sizeBytes = rawBytes.size
        val sizeLabel = when {
            sizeBytes >= 1_048_576 -> "%.2f MB".format(sizeBytes / 1_048_576.0)
            sizeBytes >= 1_024    -> "%.1f KB".format(sizeBytes / 1_024.0)
            else                  -> "$sizeBytes B"
        }

        val speedFlag = when {
            ms > 2_000 -> "🔴 SLOW  "
            ms > 500   -> "🟡       "
            else       -> "🟢       "
        }

        // ── Detect embedded base64 image data ────────────────────────────────────
        // If photos are stored as base64 and returned inline, responses can be 1 MB+.
        val bodyPreview = rawBytes.decodeToString().take(8_192)  // first 8 KB is enough to detect
        val base64PhotoCount = countOccurrences(bodyPreview, "data:image")
        val base64Warn = if (base64PhotoCount > 0) "  ⚠ BASE64 PHOTOS ×$base64PhotoCount (LIKELY CAUSE OF LARGE PAYLOAD)" else ""

        val largePayloadWarn = if (sizeBytes > 200_000) "  ⚠ LARGE PAYLOAD" else ""

        Log.i(TAG, "←  $label  $speedFlag ${response.code}  |  ${ms} ms  |  $sizeLabel$largePayloadWarn$base64Warn")

        // ── Extra breakdown for large responses ──────────────────────────────────
        if (sizeBytes > 100_000) {
            // Count top-level JSON array size if this is a list response
            val arrayItemCount = countOccurrences(bodyPreview.take(2_048), "\"_id\"")
            if (arrayItemCount > 0) {
                Log.i(TAG, "    ↳ array-item estimate: ~$arrayItemCount items in first 2 KB of body")
            }
            Log.i(TAG, "    ↳ payload breakdown: Content-Type=${contentType?.type}/${contentType?.subtype}")
        }

        // ── Rebuild the response so Retrofit can still parse the body ─────────────
        val rebuiltBody = rawBytes.toResponseBody(contentType)
        return response.newBuilder().body(rebuiltBody).build()
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
