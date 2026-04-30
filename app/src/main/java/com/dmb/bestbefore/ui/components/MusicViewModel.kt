package com.dmb.bestbefore.ui.components

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.SoundCloudTrack
import com.dmb.bestbefore.notifications.MusicPlayerManager
import com.dmb.bestbefore.utils.AppErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {

    private val _tracks = MutableStateFlow<List<SoundCloudTrack>>(emptyList())
    val tracks: StateFlow<List<SoundCloudTrack>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadPlaylist(token: String) {
        if (_tracks.value.isNotEmpty()) return // Already loaded

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.apiService.getSoundCloudPlaylist("Bearer $token")
                if (response.isSuccessful) {
                    val tracksList = response.body()?.tracks ?: emptyList()
                    _tracks.value = tracksList
                    MusicPlayerManager.setPlaylist(tracksList)
                } else {
                    _error.value = if (response.code() in 500..599) AppErrorUtils.LOADING_ERROR else "Failed to load playlist"
                }
            } catch (e: Exception) {
                _error.value = AppErrorUtils.userMessage(e, "Error loading playlist")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playTrack(context: Context, track: SoundCloudTrack) {
        MusicPlayerManager.playTrack(context, track)
    }

    fun searchTracks(query: String) {
        val clientId = "ALkAMYHptiNuZ5wq0viSlcF0BfWrSTD2"
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (query.isBlank()) {
                    _tracks.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://api.soundcloud.com/tracks?q=$encodedQuery&client_id=$clientId&limit=30"
                val response = RetrofitClient.apiService.searchSoundCloudDirect(url)
                if (response.isSuccessful) {
                    val rawTracks = response.body() ?: emptyList()
                    val mappedTracks = rawTracks.mapNotNull { item ->
                        try {
                            val id = (item["id"] as? Number)?.toLong() ?: return@mapNotNull null
                            val title = item["title"] as? String ?: return@mapNotNull null
                            val userObj = item["user"] as? Map<*, *>
                            val artist = userObj?.get("username") as? String ?: "Unknown Artist"
                            val duration = (item["duration"] as? Number)?.toLong() ?: 0L
                            val streamUrl = item["stream_url"] as? String ?: return@mapNotNull null
                            val artworkUrl = item["artwork_url"] as? String
                            SoundCloudTrack(id, title, artist, duration, "$streamUrl?client_id=$clientId", artworkUrl)
                        } catch (e: Exception) { null }
                    }
                    _tracks.value = mappedTracks
                    MusicPlayerManager.setPlaylist(mappedTracks)
                } else {
                    _error.value = if (response.code() in 500..599) AppErrorUtils.LOADING_ERROR else "Failed to search tracks"
                }
            } catch (e: Exception) {
                _error.value = AppErrorUtils.userMessage(e, "Error searching tracks")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
