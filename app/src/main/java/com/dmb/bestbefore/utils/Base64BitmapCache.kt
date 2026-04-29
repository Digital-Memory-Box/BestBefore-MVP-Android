package com.dmb.bestbefore.utils

import android.graphics.Bitmap
import android.util.LruCache
import java.security.MessageDigest

/**
 * In-process LRU cache for bitmaps decoded from base64 strings.
 * Coil cannot cache these by URL because there is no URL — the full data is the key.
 * Hash the full data URI because base64 JPEGs often share long identical prefixes.
 */
object Base64BitmapCache {
    private val maxBytes = (Runtime.getRuntime().maxMemory() / 8).toInt()

    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun get(dataUri: String): Bitmap? = cache.get(cacheKey(dataUri))

    fun put(dataUri: String, bitmap: Bitmap) {
        cache.put(cacheKey(dataUri), bitmap)
    }

    private fun cacheKey(dataUri: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(dataUri.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
