package com.dmb.bestbefore.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    init {
        // Optional: Call getMe() on init if token exists
        if (repository.getCachedToken() != null) {
            fetchCurrentUser()
        }
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getMe()
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            } else {
                // Ignore silent failure
            }
            _isLoading.value = false
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.login(email, password)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
            _isLoading.value = false
        }
    }

    fun signup(email: String, password: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.signup(email, password, name)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Signup failed"
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
    }
}

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(AuthRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
