package com.dmb.bestbefore.data.api

import android.content.Context
import com.dmb.bestbefore.data.api.models.SyncAuthResponse
import com.dmb.bestbefore.data.api.models.UpdateMeRequest
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.auth.TokenProvider
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.data.repository.AuthRepository
import com.dmb.bestbefore.utils.AppErrorUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class AuthNetworkFaultMockWebServerTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiService
    private lateinit var shortTimeoutApiService: ApiService
    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var context: Context
    private lateinit var tokenProvider: TokenProvider
    private lateinit var firebaseAuth: FirebaseAuth
    private val gson = Gson()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()

        val standardClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()

        val shortTimeoutClient = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(standardClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)

        shortTimeoutApiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(shortTimeoutClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)

        context = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        tokenProvider = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)

        coEvery { tokenProvider.getIdToken(any()) } returns "mock-test-id-token"

        authRepository = AuthRepository(
            context = context,
            sessionManager = sessionManager,
            api = apiService,
            firebaseAuth = firebaseAuth,
            tokenProvider = tokenProvider
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        unmockkAll()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. HTTP 500 / 503 Server Error Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun syncAuth_server500Error_returnsFailureAndMapsToLoadingError() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error": "Internal Server Database Outage"}""")
        )

        val result = authRepository.syncWithBackend("mock-test-id-token")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("500"))

        // Assert error message mapping identifies 500 error code
        val userErrorMessage = AppErrorUtils.userMessage(exception)
        assertEquals(AppErrorUtils.LOADING_ERROR, userErrorMessage)

        // Ensure session manager never cached corrupt/failed state
        verify(exactly = 0) { sessionManager.saveUser(any()) }
    }

    @Test
    fun updateMe_server503ServiceUnavailable_returnsFailureGracefully() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("Service Temporarily Unavailable")
        )

        val result = authRepository.updateMe(UpdateMeRequest(name = "New Profile Name"))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("503"))

        val userErrorMessage = AppErrorUtils.userMessage(exception)
        assertEquals(AppErrorUtils.LOADING_ERROR, userErrorMessage)
        verify(exactly = 0) { sessionManager.saveUser(any()) }
    }

    @Test
    fun getMe_server500Error_returnsFailureWithoutCrashing() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"message":"Database Connection Pool Exhausted"}""")
        )

        val result = authRepository.getMe()

        assertTrue(result.isFailure)
        assertEquals("Failed to fetch user data: 500", result.exceptionOrNull()?.message)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. Slow Response / Network Timeout Fault Injection Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun syncAuth_slowResponseExceedsTimeout_throwsTimeoutExceptionAndMapsToNoInternet() = runTest {
        val timeoutRepo = AuthRepository(
            context = context,
            sessionManager = sessionManager,
            api = shortTimeoutApiService,
            firebaseAuth = firebaseAuth,
            tokenProvider = tokenProvider
        )

        // Body delay of 1500ms with a 500ms read timeout
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"user":{"id":"u1","name":"Delayed User","email":"delay@test.com"}}""")
                .setBodyDelay(1500, TimeUnit.MILLISECONDS)
        )

        val result = timeoutRepo.syncWithBackend("mock-test-id-token")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            "Exception should be a SocketTimeoutException or timeout-related IOException, got: ${exception!!.javaClass}",
            exception is SocketTimeoutException || exception.message?.contains("timeout", ignoreCase = true) == true
        )

        // Assert timeout translates to NO_INTERNET user-friendly error
        assertEquals(AppErrorUtils.NO_INTERNET, AppErrorUtils.userMessage(exception))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. Malformed / Partial JSON Response Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun syncAuth_malformedTruncatedJson_handlesGracefullyWithoutCrash() = runTest {
        // Enqueue corrupted / incomplete JSON payload
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"user":{"id":"u1","name":"Trunca""")
        )

        val result = authRepository.syncWithBackend("mock-test-id-token")

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
        verify(exactly = 0) { sessionManager.saveUser(any()) }
    }

    @Test
    fun getMe_emptyResponseBodyOn200_returnsFailureWithoutNpe() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )

        val result = authRepository.getMe()

        assertTrue(result.isFailure)
        verify(exactly = 0) { sessionManager.saveUser(any()) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. Recovery After Transient Network Failure
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun syncAuth_recoversOnSecondAttemptAfterInitialFailure() = runTest {
        val expectedUser = UserDto(
            id = "user_recovered_101",
            name = "Recovered User",
            email = "recovered@bestbefore.app"
        )
        val successJson = gson.toJson(SyncAuthResponse(user = expectedUser))

        // 1st request fails with 503
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("Service Temporarily Unavailable")
        )

        // 2nd request succeeds with 200 OK
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(successJson)
        )

        val firstAttempt = authRepository.syncWithBackend("mock-test-id-token")
        assertTrue("First attempt should fail", firstAttempt.isFailure)

        val secondAttempt = authRepository.syncWithBackend("mock-test-id-token")
        assertTrue("Second attempt should recover", secondAttempt.isSuccess)
        assertEquals(expectedUser, secondAttempt.getOrNull())

        verify(exactly = 1) { sessionManager.saveUser(expectedUser) }
        verify(exactly = 1) { sessionManager.saveAuthToken("mock-test-id-token") }
    }
}
