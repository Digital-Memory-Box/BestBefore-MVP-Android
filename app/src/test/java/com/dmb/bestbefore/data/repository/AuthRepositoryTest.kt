package com.dmb.bestbefore.data.repository

import android.content.Context
import com.dmb.bestbefore.data.api.ApiService
import com.dmb.bestbefore.data.api.models.SyncAuthResponse
import com.dmb.bestbefore.data.api.models.UpdateMeRequest
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.auth.TokenProvider
import com.dmb.bestbefore.data.local.SessionManager
import com.google.firebase.auth.FirebaseAuth
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var tokenProvider: TokenProvider
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        apiService = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        tokenProvider = mockk(relaxed = true)

        repository = AuthRepository(
            context = context,
            sessionManager = sessionManager,
            api = apiService,
            firebaseAuth = firebaseAuth,
            tokenProvider = tokenProvider
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getFirebaseIdToken delegates to TokenProvider seam`() = runTest {
        coEvery { tokenProvider.getIdToken(false) } returns "seam-token-123"

        val token = repository.getFirebaseIdToken(false)

        assertEquals("seam-token-123", token)
        coVerify(exactly = 1) { tokenProvider.getIdToken(false) }
    }

    @Test
    fun `syncWithBackend on HTTP 200 saves user and token to session manager`() = runTest {
        val user = UserDto(id = "u1", name = "Test User", email = "test@bestbefore.app")
        val authResponse = SyncAuthResponse(user = user)
        coEvery { apiService.syncAuth("Bearer test-id-token") } returns Response.success(authResponse)

        val result = repository.syncWithBackend("test-id-token")

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        verify { sessionManager.saveUser(user) }
        verify { sessionManager.saveAuthToken("test-id-token") }
    }

    @Test
    fun `syncWithBackend on HTTP 500 returns failure result without caching session`() = runTest {
        val errorBody = "Internal Server Error".toResponseBody(null)
        coEvery { apiService.syncAuth("Bearer bad-token") } returns Response.error(500, errorBody)

        val result = repository.syncWithBackend("bad-token")

        assertTrue(result.isFailure)
        verify(exactly = 0) { sessionManager.saveUser(any()) }
        verify(exactly = 0) { sessionManager.saveAuthToken(any()) }
    }

    @Test
    fun `updateMe when not signed in returns failure without API call`() = runTest {
        coEvery { tokenProvider.getIdToken(false) } returns null

        val result = repository.updateMe(UpdateMeRequest(name = "New Name"))

        assertTrue(result.isFailure)
        assertEquals("Not signed in", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { apiService.updateMe(any(), any()) }
    }

    @Test
    fun `updateMe on HTTP 200 updates local session user`() = runTest {
        val updatedUser = UserDto(id = "u1", name = "Updated Name", email = "user@test.com")
        val authResponse = SyncAuthResponse(user = updatedUser)
        coEvery { tokenProvider.getIdToken(false) } returns "valid-token"
        coEvery { apiService.updateMe("Bearer valid-token", any()) } returns Response.success(authResponse)

        val result = repository.updateMe(UpdateMeRequest(name = "Updated Name"))

        assertTrue(result.isSuccess)
        assertEquals(updatedUser, result.getOrNull())
        verify { sessionManager.saveUser(updatedUser) }
    }

    @Test
    fun `logout clears local session`() {
        repository.logout()
        verify { sessionManager.clearSession() }
    }
}
