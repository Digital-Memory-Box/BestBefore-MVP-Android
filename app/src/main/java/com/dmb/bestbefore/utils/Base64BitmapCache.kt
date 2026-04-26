package com.dmb.bestbefore.utils

import android.graphics.Bitmap
import android.util.LruCache

/**
 * In-process LRU cache for bitmaps decoded from base64 strings.
 * Coil cannot cache these by URL because there is no URL — the full data is the key.
 * We key by the first 64 characters of the data URI (unique per image, avoids hashing huge strings).
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

    private fun cacheKey(dataUri: String): String =
        if (dataUri.length > 64) dataUri.substring(0, 64) else dataUri
}
