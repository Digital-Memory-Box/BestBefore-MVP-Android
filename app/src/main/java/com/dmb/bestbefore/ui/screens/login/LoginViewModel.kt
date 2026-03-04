package com.dmb.bestbefore.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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

    private val repository = AuthRepository(application)
    private val sessionManager = SessionManager(application)

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
            _errorMessage.value = "Please enter your email"
            return
        }
        _loginState.value = LoginState.PASSWORD_INPUT
    }

    /**
     * Authenticates the user:
     * 1. Firebase signInWithEmailAndPassword
     * 2. GET Firebase ID token
     * 3. POST /auth/sync to register/fetch MongoDB user
     * 4. Cache session locally
     */
    fun login(emailParam: String, passwordParam: String, onSuccess: () -> Unit) {
        if (emailParam.isEmpty() || passwordParam.isEmpty()) {
            _errorMessage.value = "Please enter both email and password"
            return
        }

        _email.value = emailParam
        _password.value = passwordParam

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.login(emailParam, passwordParam)

            _isLoading.value = false

            result.onSuccess { user ->
                sessionManager.saveUser(user.id, user.name ?: "User", user.email)
                onSuccess()
            }.onFailure { e ->
                _errorMessage.value = when (e) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "Incorrect email or password. Please try again."
                    is FirebaseAuthInvalidUserException ->
                        "No account found with this email."
                    else -> e.message ?: "Login failed. Please try again."
                }
            }
        }
    }

    fun attemptLogin() {
        login(_email.value, _password.value) {}
    }
}