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

    fun get(dataUri: String, targetSize: Int? = null): Bitmap? = cache.get(cacheKey(dataUri, targetSize))

    fun put(dataUri: String, bitmap: Bitmap, targetSize: Int? = null) {
        cache.put(cacheKey(dataUri, targetSize), bitmap)
    }

    private fun cacheKey(dataUri: String, targetSize: Int? = null): String {
        val len = dataUri.length
        val prefix = if (targetSize != null) "s${targetSize}_" else ""
        if (len <= 128) return prefix + dataUri
        val first = dataUri.substring(0, 64)
        val last = dataUri.substring(len - 64)
        return prefix + "${len}_${first}_${last}".hashCode().toString()
    }
}
