package com.ozang.bestbefore_mvp.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
        if (_email.value.isEmpty()) {
            _errorMessage.value = "Please enter your email or nickname"
            return
        }
        _loginState.value = LoginState.PASSWORD_INPUT
    }

    fun attemptLogin() {
        val emailValue = _email.value
        val passwordValue = _password.value

        if (emailValue.isEmpty() || passwordValue.isEmpty()) {
            _errorMessage.value = "Please enter both email/nickname and password"
            return
        }

        if (!isValidEmail(emailValue) && !isValidNickname(emailValue)) {
            _errorMessage.value = "Invalid email or nickname format"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Mock success
                kotlinx.coroutines.delay(1000)
                // Mock a room ID and name
                _loginSuccess.emit(Pair("mock-room-id", "Mock Room"))
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Login failed. Please check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}".toRegex()
        return emailRegex.matches(email)
    }

    private fun isValidNickname(nickname: String): Boolean {
        val nicknameRegex = "^[A-Za-z0-9_]{3,20}$".toRegex()
        return nicknameRegex.matches(nickname)
    }
}