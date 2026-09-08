package com.dmb.bestbefore.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.File

class AudioRecordingHelper(
    private var recorderHelper: AudioRecorderHelper? = null
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingTempFile: File? = null

    fun startRecording(context: Context) {
        if (recorderHelper == null) {
            recorderHelper = AudioRecorderHelper(context)
        }
        recorderHelper?.startRecording()
    }

    fun stopRecording(): File? {
        val file = recorderHelper?.stopRecording()
        recorderHelper = null
        return file
    }

    fun cancelRecording() {
        recorderHelper?.cancelRecording()
        recorderHelper = null
    }

    fun encodeAudioFileToBase64(file: File): String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun buildVoiceMemoryPayload(base64Audio: String): Map<String, Any> {
        return mapOf(
            "type" to "audio",
            "title" to "Voice Memory",
            "content" to base64Audio,
            "metadata" to emptyMap<String, Any>()
        )
    }

    fun playAudio(
        context: Context,
        source: String,
        onError: ((Throwable) -> Unit)? = null
    ) {
        if (source.startsWith("data:audio")) {
            playBase64Audio(context, source, onError)
            return
        }
        try {
            stopPlayback()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(source))
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play uri audio", e)
            onError?.invoke(e)
        }
    }

    fun playBase64Audio(
        context: Context,
        dataUri: String,
        onError: ((Throwable) -> Unit)? = null
    ) {
        try {
            stopPlayback()

            val base64String = dataUri.substringAfter("base64,")
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)

            val extension = when {
                dataUri.startsWith("data:audio/mpeg") -> ".mp3"
                dataUri.startsWith("data:audio/wav") -> ".wav"
                dataUri.startsWith("data:audio/ogg") -> ".ogg"
                else -> ".m4a"
            }
            currentPlayingTempFile?.delete()
            val tempFile = File.createTempFile("playing_audio", extension, context.cacheDir)
            tempFile.writeBytes(decodedBytes)
            currentPlayingTempFile = tempFile

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    tempFile.delete()
                    if (currentPlayingTempFile == tempFile) currentPlayingTempFile = null
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    tempFile.delete()
                    if (currentPlayingTempFile == tempFile) currentPlayingTempFile = null
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play base64 audio", e)
            currentPlayingTempFile?.delete()
            currentPlayingTempFile = null
            onError?.invoke(e)
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mediaPlayer", e)
        } finally {
            mediaPlayer = null
            currentPlayingTempFile?.delete()
            currentPlayingTempFile = null
        }
    }

    fun release() {
        stopPlayback()
        cancelRecording()
    }

    companion object {
        private const val TAG = "AudioRecordingHelper"
    }
}
