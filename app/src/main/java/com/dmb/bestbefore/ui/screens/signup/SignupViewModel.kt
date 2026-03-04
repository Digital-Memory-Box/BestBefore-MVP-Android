package com.dmb.bestbefore.ui.screens.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignupViewModel(application: Application) : AndroidViewModel(application) {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _signupSuccess = MutableSharedFlow<String>()
    val signupSuccess: SharedFlow<String> = _signupSuccess.asSharedFlow()

    private val _isVerificationSent = MutableStateFlow(false)
    val isVerificationSent: StateFlow<Boolean> = _isVerificationSent.asStateFlow()

    private val repository = AuthRepository(application)
    private val sessionManager = SessionManager(application)
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun updateName(newName: String) {
        _name.value = newName
        _errorMessage.value = null
    }

    fun updateEmail(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null
    }

    fun updatePassword(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    /**
     * Step 1: Create Firebase account + send verification email.
     * Does NOT call the backend yet — backend auto-creates the user on first sync.
     */
    fun attemptSignup() {
        val nameValue = _name.value.trim()
        val emailValue = _email.value.trim()
        val passwordValue = _password.value

        if (!isValidEmail(emailValue)) {
            _errorMessage.value = "Please enter a valid email"
            return
        }

        if (passwordValue.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Create or recover Firebase account
                var user = firebaseAuth.currentUser
                if (user == null || user.email != emailValue) {
                    val result = firebaseAuth
                        .createUserWithEmailAndPassword(emailValue, passwordValue)
                        .await()
                    user = result.user
                }

                // Send verification email
                user?.sendEmailVerification()?.await()
                _isLoading.value = false
                _isVerificationSent.value = true

            } catch (e: FirebaseAuthUserCollisionException) {
                // Account already exists in Firebase — try silent sign-in to recover
                try {
                    val signInResult = firebaseAuth
                        .signInWithEmailAndPassword(emailValue, passwordValue)
                        .await()
                    val user = signInResult.user
                    if (user != null) {
                        if (!user.isEmailVerified) {
                            user.sendEmailVerification().await()
                            _isLoading.value = false
                            _isVerificationSent.value = true
                        } else {
                            // Already verified — sync directly
                            syncAndComplete()
                        }
                        return@launch
                    }
                } catch (signInEx: Exception) {
                    // Password wrong or other sign-in issue
                }
                _isLoading.value = false
                _errorMessage.value = "Account already exists. Please Log In instead."

            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = when (e) {
                    is FirebaseAuthWeakPasswordException ->
                        "Password is too weak. Please choose a stronger password."
                    is FirebaseAuthInvalidCredentialsException ->
                        "Invalid email format."
                    else -> e.message ?: "Signup failed. Please try again."
                }
            }
        }
    }

    /**
     * Step 2: User taps "I've verified my email".
     * Reload Firebase user to check verification, then sync to MongoDB backend
     * via POST /auth/sync (the backend will find-or-create the user automatically).
     */
    fun checkVerificationStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val user = firebaseAuth.currentUser
                    ?: run {
                        _isLoading.value = false
                        _errorMessage.value = "Session expired. Please start again."
                        return@launch
                    }

                // Reload to get the latest email verification state
                user.reload().await()

                if (user.isEmailVerified) {
                    syncAndComplete()
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Email not yet verified. Please check your inbox."
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Failed to check status: ${e.message}"
            }
        }
    }

    /** Obtain Firebase ID token → call POST /auth/sync → cache session → emit success. */
    private suspend fun syncAndComplete() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            _isLoading.value = false
            _errorMessage.value = "Session expired. Please start again."
            return
        }

        val idToken = user.getIdToken(false).await()?.token
        if (idToken == null) {
            _isLoading.value = false
            _errorMessage.value = "Failed to get auth token. Please try again."
            return
        }

        val result = repository.syncWithBackend(idToken)
        _isLoading.value = false

        result.onSuccess { userDto ->
            sessionManager.saveUser(userDto.id, userDto.name ?: "User", userDto.email)
            _signupSuccess.emit(userDto.email)
        }.onFailure { e ->
            _errorMessage.value = e.message ?: "Server sync failed. Please try again."
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
