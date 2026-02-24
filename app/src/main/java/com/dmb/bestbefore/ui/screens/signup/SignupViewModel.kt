package com.dmb.bestbefore.ui.screens.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    private val repository = com.dmb.bestbefore.data.repository.AuthRepository(application)

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
                // 1. Create user in Firebase first
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                
                // Check if we already created the user but just need to verify (handle re-tries)
                var user = auth.currentUser
                if (user == null || user.email != emailValue) {
                     val firebaseResult = kotlin.coroutines.suspendCoroutine<com.google.firebase.auth.AuthResult> { continuation ->
                        auth.createUserWithEmailAndPassword(emailValue, passwordValue)
                            .addOnSuccessListener { result -> continuation.resumeWith(Result.success(result)) }
                            .addOnFailureListener { e -> continuation.resumeWith(Result.failure(e)) }
                    }
                    user = firebaseResult.user
                }

                // 2. Send verification email
                user?.sendEmailVerification()
                _isLoading.value = false
                _isVerificationSent.value = true // Show verification UI

            } catch (e: Exception) {
                if (e is FirebaseAuthUserCollisionException) {
                    // User exists in Firebase but maybe not in Backend. Try to sign in to recover.
                    try {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val signInResult = kotlin.coroutines.suspendCoroutine<com.google.firebase.auth.AuthResult> { cont ->
                            auth.signInWithEmailAndPassword(emailValue, passwordValue)
                                .addOnSuccessListener { cont.resumeWith(Result.success(it)) }
                                .addOnFailureListener { cont.resumeWith(Result.failure(it)) }
                        }
                        
                        val user = signInResult.user
                        if (user != null) {
                            // Recovered! 
                            if (!user.isEmailVerified) {
                                user.sendEmailVerification()
                                _isLoading.value = false
                                _isVerificationSent.value = true
                            } else {
                                // Already verified, try to create backend user
                                completeBackendSignup()
                            }
                            return@launch
                        }
                    } catch (signInEx: Exception) {
                         // Sign in failed (probably wrong password), fall through to error message
                    }
                }
                
                _isLoading.value = false
                _errorMessage.value = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Password is too weak. Please enter a stronger password."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                    is FirebaseAuthUserCollisionException -> "Account already exists for this email. Please Log In."
                    else -> e.message ?: "Signup failed. Please try again."
                }
            }
        }
    }
    
    fun checkVerificationStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val user = auth.currentUser
                
                user?.reload()?.await() // Requires play-services-tasks await extension or manual wrapper
                
                if (user?.isEmailVerified == true) {
                     // 3. Proceed to backend creation ONLY if verified
                     completeBackendSignup()
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Email not verified yet. Please check your inbox."
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Failed to check status: ${e.message}"
            }
        }
    }
    
    // Helper to interact with Tasks in coroutines if headers missing
    suspend fun com.google.android.gms.tasks.Task<*>.await(): Unit = kotlin.coroutines.suspendCoroutine { cont ->
        this.addOnSuccessListener { cont.resumeWith(Result.success(Unit)) }
        .addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }


    private suspend fun completeBackendSignup() {
         val nameValue = _name.value.trim()
         val emailValue = _email.value.trim()
         val passwordValue = _password.value
         
         val result = repository.signup(nameValue, emailValue, passwordValue)
         _isLoading.value = false
         
         result.onSuccess { authResponse ->
             _signupSuccess.emit(authResponse.user.email)
         }.onFailure { e ->
             _errorMessage.value = e.message ?: "Signup failed on server"
         }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
