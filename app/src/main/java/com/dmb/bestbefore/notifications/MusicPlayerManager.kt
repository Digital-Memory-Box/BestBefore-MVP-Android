package com.dmb.bestbefore.notifications

import com.dmb.bestbefore.data.api.models.SoundCloudTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Context
import android.content.Intent

object MusicPlayerManager {
    private val _currentTrack = MutableStateFlow<SoundCloudTrack?>(null)
    val currentTrack: StateFlow<SoundCloudTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var playlist: List<SoundCloudTrack> = emptyList()

    fun setPlaylist(tracks: List<SoundCloudTrack>) {
        playlist = tracks
    }

    fun getPlaylist(): List<SoundCloudTrack> = playlist

    fun playTrack(context: Context, track: SoundCloudTrack) {
        _currentTrack.value = track
        _isPlaying.value = true
        
        val intent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_PLAY
            putExtra(MusicPlayerService.EXTRA_TRACK_ID, track.id)
            putExtra(MusicPlayerService.EXTRA_TRACK_TITLE, track.title)
            putExtra(MusicPlayerService.EXTRA_TRACK_ARTIST, track.artist)
            putExtra(MusicPlayerService.EXTRA_TRACK_STREAM_URL, track.streamUrl)
            putExtra(MusicPlayerService.EXTRA_TRACK_ARTWORK, track.artworkUrl)
        }
        context.startForegroundService(intent)
    }

    fun togglePlayPause(context: Context) {
        val intent = Intent(context, MusicPlayerService::class.java).apply {
            action = if (_isPlaying.value) MusicPlayerService.ACTION_PAUSE else MusicPlayerService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun stop(context: Context) {
        _currentTrack.value = null
        _isPlaying.value = false
        val intent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_STOP
        }
        context.startService(intent)
    }

    // Callbacks from Service
    internal fun onPlaybackStarted() {
        _isPlaying.value = true
    }

    internal fun onPlaybackPaused() {
        _isPlaying.value = false
    }

    internal fun onPlaybackStopped() {
        _isPlaying.value = false
        _currentTrack.value = null
    }

    fun playNext(context: Context) {
        val current = _currentTrack.value
        if (current != null && playlist.isNotEmpty()) {
            val currentIndex = playlist.indexOfFirst { it.id == current.id }
            if (currentIndex != -1) {
                val nextIndex = (currentIndex + 1) % playlist.size
                playTrack(context, playlist[nextIndex])
            }
        }
    }

    fun playPrevious(context: Context) {
        val current = _currentTrack.value
        if (current != null && playlist.isNotEmpty()) {
            val currentIndex = playlist.indexOfFirst { it.id == current.id }
            if (currentIndex != -1) {
                val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
                playTrack(context, playlist[prevIndex])
            }
        }
    }

    internal fun onTrackCompleted(context: Context) {
        playNext(context)
    }
}
