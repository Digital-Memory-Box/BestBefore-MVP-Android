package com.dmb.bestbefore.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class LoginState {
    INITIAL,
    EMAIL_INPUT,
    PASSWORD_INPUT
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {


    private val _loginState = MutableStateFlow(LoginState.INITIAL)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginSuccess = MutableSharedFlow<Pair<String, String>>()
    val loginSuccess: SharedFlow<Pair<String, String>> = _loginSuccess.asSharedFlow()

    fun updateEmail(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null
    }

    fun updatePassword(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    fun transitionToEmailInput() {
        _loginState.value = LoginState.EMAIL_INPUT
    }

    fun transitionToPasswordInput() {
         // Simplified logic: Just check if empty
        if (_email.value.isEmpty()) {
            _errorMessage.value = "Please enter your email or nickname"
            return
        }
        _loginState.value = LoginState.PASSWORD_INPUT
    }

    private val repository = com.dmb.bestbefore.data.repository.AuthRepository(application)
    
    // Login function called from UI
    fun login(emailParam: String, passwordParam: String, onSuccess: () -> Unit) {
         if (emailParam.isEmpty() || passwordParam.isEmpty()) {
            _errorMessage.value = "Please enter both email/nickname and password"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // Update internal state just in case
            _email.value = emailParam
            _password.value = passwordParam

            val result = repository.login(emailParam, passwordParam)
            _isLoading.value = false
            
            result.onSuccess { auth ->
                 // Save session
                 val sessionManager = com.dmb.bestbefore.data.local.SessionManager(getApplication())
                 sessionManager.saveAuthToken(auth.token)
                 sessionManager.saveUser(auth.user.id, auth.user.name ?: "User", auth.user.email)
                 
                 _loginSuccess.emit(Pair(auth.user.id, auth.user.name ?: "User"))
                 onSuccess()
            }.onFailure { e ->
                _errorMessage.value = e.message ?: "Login failed"
            }
        }
    }

    fun attemptLogin() {
        val emailValue = _email.value
        val passwordValue = _password.value
        // call the main login function with empty callback or handle generic
        login(emailValue, passwordValue) {}
    }
}