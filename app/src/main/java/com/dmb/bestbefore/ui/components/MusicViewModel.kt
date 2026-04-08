package com.dmb.bestbefore.ui.components

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.SoundCloudTrack
import com.dmb.bestbefore.notifications.MusicPlayerManager
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
                    _error.value = "Failed to load playlist: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error loading playlist: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playTrack(context: Context, track: SoundCloudTrack) {
        MusicPlayerManager.playTrack(context, track)
    }
}
