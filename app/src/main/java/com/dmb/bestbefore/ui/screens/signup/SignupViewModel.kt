package com.dmb.bestbefore.ui.screens.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.data.repository.AuthRepository
import com.dmb.bestbefore.data.api.models.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import android.util.Log

class SignupViewModel(application: Application) : AndroidViewModel(application) {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _userType = MutableStateFlow("normal")
    val userType: StateFlow<String> = _userType.asStateFlow()

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

    fun updateUserType(newType: String) {
        _userType.value = newType
    }

    fun cancelSignup() {
        _isLoading.value = false
        _errorMessage.value = "Signup cancelled. Please try again."
    }

    /**
     * Step 1: Create Firebase account + send verification email.
     * Does NOT call the backend yet — backend auto-creates the user on first sync.
     */
    fun attemptSignup() {
        val nameValue = _name.value.trim()
        val emailValue = _email.value.trim().lowercase()
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
            Log.d("SignupViewModel", "Starting signup for: $emailValue")

            try {
                var succeeded = false

                // Retry up to 3 times — handles intermittent WiFi data stalls
                for (attempt in 1..3) {
                    Log.d("SignupViewModel", "Signup attempt $attempt/3")

                    val result = withTimeoutOrNull(20_000L) {
                        try {
                            var user = firebaseAuth.currentUser
                            if (user == null || user.email != emailValue) {
                                Log.d("SignupViewModel", "Creating Firebase account...")
                                val created = firebaseAuth
                                    .createUserWithEmailAndPassword(emailValue, passwordValue)
                                    .await()
                                user = created.user
                                Log.d("SignupViewModel", "Firebase account created: ${user?.uid}")
                            }
                            user?.sendEmailVerification()?.await()
                            Log.d("SignupViewModel", "Verification email sent")
                            "ok"
                        } catch (e: FirebaseAuthUserCollisionException) {
                            Log.d("SignupViewModel", "Account collision — trying sign in")
                            handleCollision(emailValue, passwordValue)
                            "collision"
                        } catch (e: Exception) {
                            Log.e("SignupViewModel", "Firebase error: ${e.javaClass.simpleName}: ${e.message}")
                            _isLoading.value = false
                            _errorMessage.value = when (e) {
                                is FirebaseAuthWeakPasswordException -> "Password is too weak."
                                is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                                else -> e.message ?: "Signup failed. Please try again."
                            }
                            "error"
                        }
                    }

                    when (result) {
                        "ok" -> { succeeded = true; break }
                        "collision", "error" -> break  // Already handled inside
                        null -> {
                            // Timed out — retry if not last attempt
                            Log.e("SignupViewModel", "Attempt $attempt timed out")
                            if (attempt < 3) {
                                _errorMessage.value = "Retrying... ($attempt/3)"
                                kotlinx.coroutines.delay(2000L)
                            }
                        }
                    }
                }

                if (succeeded) {
                    _isLoading.value = false
                    _isVerificationSent.value = true
                } else if (_isLoading.value) {
                    Log.e("SignupViewModel", "All signup attempts failed")
                    _isLoading.value = false
                    _errorMessage.value = "Network is unstable. Please check your WiFi and try again."
                }

            } catch (e: Exception) {
                Log.e("SignupViewModel", "Unexpected error: ${e.message}")
                _isLoading.value = false
                _errorMessage.value = e.message ?: "Unexpected error. Please try again."
            }
        }
    }


    private suspend fun handleCollision(emailValue: String, passwordValue: String) {
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
                    syncAndComplete()
                }
                return
            }
        } catch (signInEx: Exception) {
            Log.e("SignupViewModel", "Sign-in during collision failed: ${signInEx.message}")
        }
        _isLoading.value = false
        _errorMessage.value = "Account already exists. Please Log In instead."
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
            // Update name and userType via updateMe
            viewModelScope.launch {
                val updateResult = repository.updateMe(
                    com.dmb.bestbefore.data.api.models.UpdateMeRequest(
                        name = _name.value.trim(),
                        userType = _userType.value
                    )
                )
                // Proceed regardless of update success to keep flow smooth
                val updatedDto = updateResult.getOrNull() ?: userDto
                sessionManager.saveUser(updatedDto)
                _signupSuccess.emit(updatedDto.email)
            }
        }.onFailure { e ->
            _errorMessage.value = e.message ?: "Server sync failed. Please try again."
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
