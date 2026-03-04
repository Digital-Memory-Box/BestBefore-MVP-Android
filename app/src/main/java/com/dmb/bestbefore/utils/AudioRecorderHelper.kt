package com.dmb.bestbefore.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var currentOutputFile: File? = null
        private set

    fun startRecording() {
        // Prepare output file in cache directory
        val fileName = "audio_record_${System.currentTimeMillis()}.m4a"
        currentOutputFile = File(context.cacheDir, fileName)

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(currentOutputFile?.absolutePath)

            try {
                prepare()
                start()
                Log.d("AudioRecorder", "Recording started: ${currentOutputFile?.absolutePath}")
            } catch (e: IOException) {
                Log.e("AudioRecorder", "prepare() failed", e)
            }
        }
    }

    fun stopRecording(): File? {
        try {
            recorder?.apply {
                stop()
                release()
            }
            Log.d("AudioRecorder", "Recording stopped")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "stop() failed", e)
        } finally {
            recorder = null
        }
        return currentOutputFile
    }

    fun cancelRecording() {
        stopRecording()
        currentOutputFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        currentOutputFile = null
    }
}
