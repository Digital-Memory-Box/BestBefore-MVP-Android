package com.dmb.bestbefore.ui.screens.profile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.PublicProfileDto
import com.dmb.bestbefore.data.local.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreatorProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _profileState = MutableStateFlow<PublicProfileDto?>(null)
    val profileState: StateFlow<PublicProfileDto?> = _profileState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadProfile(context: Context, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = SessionManager(context).getAuthToken() ?: ""
                val response = RetrofitClient.apiService.getPublicProfile("Bearer $token", userId)
                if (response.isSuccessful) {
                    _profileState.value = response.body()
                } else {
                    _error.value = "Failed to load profile"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
