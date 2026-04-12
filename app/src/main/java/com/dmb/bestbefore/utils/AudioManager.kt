package com.dmb.bestbefore.utils

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

class AudioManager private constructor() {

    companion object {
        val shared = AudioManager()
        private const val TAG = "AudioManager"
    }

    private var player: MediaPlayer? = null
    private var currentTrack: String? = null

    private val streamPresets = mapOf(
        "Lofi Beats" to "https://p.scdn.co/mp3-preview/766c5968f9a3f2560388a1e843f88be968746c0d?cid=774b29d4f13844c495f206cafdad9c86",
        "Nature Ambience" to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        "Chill Cafe" to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
        "Minimal Piano" to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
        "Vaporwave" to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
        "Dreamy Synth" to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
    )

    fun playBackgroundMusic(preset: String?) {
        if (preset == null || preset == "None") {
            stopMusic()
            return
        }

        if (currentTrack == preset && player?.isPlaying == true) {
            return
        }

        currentTrack = preset

        // If it's a known preset, use the stream URL, otherwise treat as a direct URL string
        val streamURLString = streamPresets[preset] ?: preset
        Log.d(TAG, "Streaming from: $streamURLString")

        try {
            stopMusic() // We make sure to stop and release before recreating

            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(streamURLString)
                setVolume(0.5f, 0.5f)
                isLooping = true // Handles the notification loop natively in Android
                
                setOnPreparedListener {
                    it.start()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    true
                }
                
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio: ${e.message}")
            stopMusic()
        }
    }

    fun stopMusic() {
        Log.d(TAG, "Stopping music")
        try {
            player?.stop()
            player?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player: ${e.message}")
        } finally {
            player = null
            currentTrack = null
        }
    }
}
