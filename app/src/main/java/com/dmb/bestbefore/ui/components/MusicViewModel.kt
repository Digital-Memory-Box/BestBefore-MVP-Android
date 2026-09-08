package com.dmb.bestbefore.ui.components

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.api.ApiService
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.SoundCloudTrack
import com.dmb.bestbefore.notifications.MusicPlayerManager
import com.dmb.bestbefore.utils.AppErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel @JvmOverloads constructor(
    private val api: ApiService = RetrofitClient.apiService
) : ViewModel() {

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
                val response = api.getSoundCloudPlaylist("Bearer $token")
                if (response.isSuccessful) {
                    val tracksList = response.body()?.tracks ?: emptyList()
                    val baseUrl = com.dmb.bestbefore.BuildConfig.API_BASE_URL.removeSuffix("/")
                    val mappedTracks = tracksList.map { track ->
                        if (track.streamUrl.startsWith("/")) {
                            track.copy(streamUrl = baseUrl + track.streamUrl)
                        } else {
                            track
                        }
                    }
                    _tracks.value = mappedTracks
                    MusicPlayerManager.setPlaylist(mappedTracks)
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

    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchTracks(token: String, query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300L)
            _isLoading.value = true
            _error.value = null
            try {
                if (query.isBlank()) {
                    _tracks.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                val response = api.searchSoundCloudTracks("Bearer $token", query)
                if (response.isSuccessful) {
                    val tracksList = response.body()?.tracks ?: emptyList()
                    val baseUrl = com.dmb.bestbefore.BuildConfig.API_BASE_URL.removeSuffix("/")
                    val mappedTracks = tracksList.map { track ->
                        if (track.streamUrl.startsWith("/")) {
                            track.copy(streamUrl = baseUrl + track.streamUrl)
                        } else {
                            track
                        }
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
