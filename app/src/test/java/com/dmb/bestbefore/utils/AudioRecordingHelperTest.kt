package com.dmb.bestbefore.utils

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class AudioRecordingHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `buildVoiceMemoryPayload returns expected structure`() {
        val helper = AudioRecordingHelper()
        val payload = helper.buildVoiceMemoryPayload("base64-audio-content")

        assertEquals("audio", payload["type"])
        assertEquals("Voice Memory", payload["title"])
        assertEquals("base64-audio-content", payload["content"])
        assertTrue((payload["metadata"] as Map<*, *>).isEmpty())
    }

    @Test
    fun `encodeAudioFileToBase64 encodes raw file bytes to NO_WRAP base64`() {
        val helper = AudioRecordingHelper()
        val file = tempFolder.newFile("sample_voice.m4a")
        file.writeBytes("voice-data-bytes-test".toByteArray())

        val encoded = helper.encodeAudioFileToBase64(file)
        assertNotNull(encoded)
        assertTrue(encoded.isNotEmpty())
        assertFalse(encoded.contains("\n"))
    }

    @Test
    fun `startRecording delegates to AudioRecorderHelper`() {
        val mockRecorder = mockk<AudioRecorderHelper>(relaxed = true)
        val helper = AudioRecordingHelper(recorderHelper = mockRecorder)
        val context = mockk<Context>(relaxed = true)

        helper.startRecording(context)

        verify(exactly = 1) { mockRecorder.startRecording() }
    }

    @Test
    fun `stopRecording delegates to AudioRecorderHelper and returns recorded file`() {
        val mockRecorder = mockk<AudioRecorderHelper>(relaxed = true)
        val expectedFile = File("test.m4a")
        every { mockRecorder.stopRecording() } returns expectedFile

        val helper = AudioRecordingHelper(recorderHelper = mockRecorder)
        val result = helper.stopRecording()

        assertEquals(expectedFile, result)
        verify(exactly = 1) { mockRecorder.stopRecording() }
    }

    @Test
    fun `cancelRecording delegates to AudioRecorderHelper`() {
        val mockRecorder = mockk<AudioRecorderHelper>(relaxed = true)
        val helper = AudioRecordingHelper(recorderHelper = mockRecorder)

        helper.cancelRecording()

        verify(exactly = 1) { mockRecorder.cancelRecording() }
    }

    @Test
    fun `release cancels recording and stops playback cleanly`() {
        val mockRecorder = mockk<AudioRecorderHelper>(relaxed = true)
        val helper = AudioRecordingHelper(recorderHelper = mockRecorder)

        helper.release()

        verify(exactly = 1) { mockRecorder.cancelRecording() }
    }
}
