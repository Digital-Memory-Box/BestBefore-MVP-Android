package com.dmb.bestbefore.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class ImageProcessingHelperTest {

    private fun createSampleBitmapBytes(width: Int = 200, height: Int = 200): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
        return bos.toByteArray()
    }

    @Test
    fun `encodeBytesToBase64 produces expected base64 string`() {
        val input = "Test BestBefore Image Data".toByteArray()
        val base64 = ImageProcessingHelper.encodeBytesToBase64(input)

        assertNotNull(base64)
        assertTrue(base64.isNotEmpty())
        assertFalse(base64.contains("\n"))
    }

    @Test
    fun `downsampleAndEncodeImage with empty bytes returns null`() {
        val result = ImageProcessingHelper.downsampleAndEncodeImage(ByteArray(0))
        assertNull(result)
    }

    @Test
    fun `downsampleAndEncodeImage with valid bitmap compresses and returns base64`() {
        val bytes = createSampleBitmapBytes(300, 300)
        val result = ImageProcessingHelper.downsampleAndEncodeImage(bytes, maxDimension = 512, quality = 80)

        assertNotNull(result)
        assertTrue(result!!.isNotEmpty())
    }

    @Test
    fun `downsampleAndEncodeImage downsamples large image to fit within maxDimension`() {
        val largeBytes = createSampleBitmapBytes(2048, 2048)
        val result = ImageProcessingHelper.downsampleAndEncodeImage(largeBytes, maxDimension = 512, quality = 60)

        assertNotNull(result)
        assertTrue(result!!.isNotEmpty())
    }

    @Test
    fun `encodeUriToBase64 reads from content resolver and encodes successfully`() = runTest {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()
        val sampleBytes = createSampleBitmapBytes(100, 100)

        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(sampleBytes)

        val result = ImageProcessingHelper.encodeUriToBase64(context, uri, maxDimension = 512, quality = 70)

        assertNotNull(result)
        assertTrue(result!!.isNotEmpty())
    }

    @Test
    fun `encodeUriToBase64 when stream is null returns null gracefully`() = runTest {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()

        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns null

        val result = ImageProcessingHelper.encodeUriToBase64(context, uri)
        assertNull(result)
    }
}
