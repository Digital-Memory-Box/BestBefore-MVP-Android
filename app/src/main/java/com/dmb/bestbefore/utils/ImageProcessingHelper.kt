package com.dmb.bestbefore.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageProcessingHelper {

    /**
     * Encodes raw byte array to Base64 (NO_WRAP).
     */
    fun encodeBytesToBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Decodes, downsamples (to fit within [maxDimension] x [maxDimension]), compresses as JPEG,
     * and returns the Base64 representation.
     */
    fun downsampleAndEncodeImage(
        bytes: ByteArray,
        maxDimension: Int = 1024,
        quality: Int = 60
    ): String? {
        if (bytes.isEmpty()) return null
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            var inSampleSize = 1
            while (options.outWidth / inSampleSize > maxDimension || options.outHeight / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null

            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            encodeBytesToBase64(bos.toByteArray())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reads image from content URI, downsamples, compresses, and returns Base64 string on Dispatchers.IO.
     */
    suspend fun encodeUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 512,
        quality: Int = 70
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            val raw = stream.use { it.readBytes() }
            downsampleAndEncodeImage(raw, maxDimension, quality)
        }.getOrNull()
    }
}
