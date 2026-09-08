package com.dmb.bestbefore.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * TokenProvider abstracts Firebase ID token retrieval for seamless testability
 * and decoupling of Repositories and ViewModels from Firebase Singletons.
 */
interface TokenProvider {
    suspend fun getIdToken(forceRefresh: Boolean = false): String?
}

/**
 * Default implementation backed by [FirebaseAuth].
 */
class FirebaseTokenProvider(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : TokenProvider {
    override suspend fun getIdToken(forceRefresh: Boolean): String? {
        return try {
            val user = firebaseAuth.currentUser ?: return null
            user.getIdToken(forceRefresh).await()?.token
        } catch (e: Exception) {
            null
        }
    }
}
