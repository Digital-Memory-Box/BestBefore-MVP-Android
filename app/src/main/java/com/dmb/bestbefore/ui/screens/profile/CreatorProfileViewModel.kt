package com.dmb.bestbefore.ui.screens.profile

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.PublicProfileDto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CreatorProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _profileState = MutableStateFlow<PublicProfileDto?>(null)
    val profileState: StateFlow<PublicProfileDto?> = _profileState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadProfile(context: Context, userId: String) {
        if (userId.isBlank()) {
            _error.value = "No user ID provided"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _profileState.value = null
            try {
                // Always use a fresh Firebase ID token — the SessionManager token
                // may be stale or missing for other users' profile fetches.
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                val token = firebaseUser?.getIdToken(false)?.await()?.token ?: ""

                Log.d("CreatorProfileVM", "loadProfile userId=$userId, hasToken=${token.isNotEmpty()}")

                val response = RetrofitClient.apiService.getPublicProfile("Bearer $token", userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("CreatorProfileVM", "Profile loaded successfully for $userId: $body")
                    _profileState.value = body
                    if (body == null) {
                        _error.value = "Server returned empty profile body"
                    }
                } else {
                    val errBody = response.errorBody()?.string()
                    Log.e("CreatorProfileVM", "HTTP Error ${response.code()} for $userId: $errBody")
                    _error.value = "Error ${response.code()}: ${errBody ?: "Failed to load profile"}"
                }
            } catch (e: Exception) {
                Log.e("CreatorProfileVM", "Exception loading profile", e)
                _error.value = e.localizedMessage ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
