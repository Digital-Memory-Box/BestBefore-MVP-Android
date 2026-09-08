package com.dmb.bestbefore.ui.screens.login

import android.app.Application
import android.os.Looper
import com.dmb.bestbefore.data.api.models.UpdateMeRequest
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.data.repository.AuthRepository
import com.dmb.bestbefore.utils.AppErrorUtils
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.random.Random

class FakeAuthRepository(application: Application) : AuthRepository(application) {
    var loginResult: Result<UserDto> = Result.success(UserDto(id = "user123", name = "Test User", email = "user@test.com"))
    var googleLoginResult: Result<UserDto> = Result.success(UserDto(id = "user123", name = "Test User", email = "user@test.com"))
    var syncBackendResult: Result<UserDto> = Result.success(UserDto(id = "user123", name = "Test User", email = "user@test.com"))
    var updateMeResult: Result<UserDto> = Result.success(UserDto(id = "user123", name = "Test User", email = "user@test.com"))
    var syncFcmTokenResult: Result<Unit> = Result.success(Unit)

    var loginCallCount = 0
    var fcmSyncCallCount = 0

    override suspend fun login(email: String, password: String): Result<UserDto> {
        loginCallCount++
        return loginResult
    }

    override suspend fun loginWithGoogleIdToken(idToken: String): Result<UserDto> {
        return googleLoginResult
    }

    override suspend fun syncWithBackend(firebaseIdToken: String): Result<UserDto> {
        return syncBackendResult
    }

    override suspend fun updateMe(updates: UpdateMeRequest): Result<UserDto> {
        return updateMeResult
    }

    override suspend fun syncFcmToken(): Result<Unit> {
        fcmSyncCallCount++
        return syncFcmTokenResult
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel
    private var isNetworkOnline = true

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = RuntimeEnvironment.getApplication()
        fakeAuthRepository = FakeAuthRepository(application)
        sessionManager = mockk(relaxed = true)
        isNetworkOnline = true

        viewModel = LoginViewModel(
            application = application,
            repository = fakeAuthRepository,
            sessionManager = sessionManager,
            isNetworkAvailable = { isNetworkOnline }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun drainExecution() {
        testDispatcher.scheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `login success for everyone mode saves user, syncs token and invokes success callback`() = runTest(testDispatcher) {
        val testUser = UserDto(
            id = "user123",
            name = "Test User",
            email = "user@test.com",
            userType = "normal"
        )
        fakeAuthRepository.loginResult = Result.success(testUser)

        var successCallbackInvoked = false

        viewModel.login(
            emailParam = "user@test.com",
            passwordParam = "password123",
            loginMode = LoginMode.EVERYONE,
            onSuccess = { successCallbackInvoked = true }
        )

        drainExecution()

        assertTrue(successCallbackInvoked)
        verify { sessionManager.saveUser(testUser) }
        assertEquals(1, fakeAuthRepository.fcmSyncCallCount)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `login with empty email or password sets error without calling repository`() = runTest(testDispatcher) {
        var callbackInvoked = false

        viewModel.login(
            emailParam = "",
            passwordParam = "password123",
            loginMode = LoginMode.EVERYONE,
            onSuccess = { callbackInvoked = true }
        )

        drainExecution()

        assertFalse(callbackInvoked)
        assertEquals("Please enter both email and password", viewModel.errorMessage.value)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `login without internet connection sets no internet error`() = runTest(testDispatcher) {
        isNetworkOnline = false
        var callbackInvoked = false

        viewModel.login(
            emailParam = "user@test.com",
            passwordParam = "pass1234",
            loginMode = LoginMode.EVERYONE,
            onSuccess = { callbackInvoked = true }
        )

        drainExecution()

        assertFalse(callbackInvoked)
        assertEquals(AppErrorUtils.NO_INTERNET, viewModel.errorMessage.value)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `login artist mode with normal account sets artist only error`() = runTest(testDispatcher) {
        val normalUser = UserDto(id = "1", name = "Norm", email = "norm@test.com", userType = "normal")
        fakeAuthRepository.loginResult = Result.success(normalUser)

        var callbackInvoked = false
        viewModel.login(
            emailParam = "norm@test.com",
            passwordParam = "secret",
            loginMode = LoginMode.ARTISTS,
            onSuccess = { callbackInvoked = true }
        )

        drainExecution()

        assertFalse(callbackInvoked)
        assertEquals("This login is for artist accounts only.", viewModel.errorMessage.value)
        verify(exactly = 0) { sessionManager.saveUser(any()) }
    }

    @Test
    fun `login everyone mode with artist account sets artist redirect error`() = runTest(testDispatcher) {
        val artistUser = UserDto(id = "2", name = "Artist", email = "artist@test.com", userType = "artist")
        fakeAuthRepository.loginResult = Result.success(artistUser)

        var callbackInvoked = false
        viewModel.login(
            emailParam = "artist@test.com",
            passwordParam = "secret",
            loginMode = LoginMode.EVERYONE,
            onSuccess = { callbackInvoked = true }
        )

        drainExecution()

        assertFalse(callbackInvoked)
        assertEquals("Artist accounts must use the Artist login screen.", viewModel.errorMessage.value)
    }

    @Test
    fun `login failure with invalid credentials maps to friendly message`() = runTest(testDispatcher) {
        fakeAuthRepository.loginResult = Result.failure(
            FirebaseAuthInvalidCredentialsException("INVALID", "bad creds")
        )

        viewModel.login("bad@test.com", "wrongpass", LoginMode.EVERYONE) {}
        drainExecution()

        assertEquals("Incorrect email or password. Please try again.", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `login failure with invalid user maps to no account found message`() = runTest(testDispatcher) {
        fakeAuthRepository.loginResult = Result.failure(
            FirebaseAuthInvalidUserException("NOT_FOUND", "not found")
        )

        viewModel.login("unknown@test.com", "pass123", LoginMode.EVERYONE) {}
        drainExecution()

        assertEquals("No account found with this email.", viewModel.errorMessage.value)
    }

    @Test
    fun `login failure with network exception maps to NO_INTERNET`() = runTest(testDispatcher) {
        fakeAuthRepository.loginResult = Result.failure(
            FirebaseNetworkException("Network error")
        )

        viewModel.login("user@test.com", "pass123", LoginMode.EVERYONE) {}
        drainExecution()

        assertEquals(AppErrorUtils.NO_INTERNET, viewModel.errorMessage.value)
    }

    @Test
    fun `transitionToPasswordInput with empty email shows validation error`() {
        viewModel.updateEmail("")
        viewModel.transitionToPasswordInput()

        assertEquals("Please enter your email", viewModel.errorMessage.value)
        assertEquals(LoginState.INITIAL, viewModel.loginState.value)
    }

    @Test
    fun `transitionToPasswordInput with non-empty email updates state`() {
        viewModel.updateEmail("valid@test.com")
        viewModel.transitionToPasswordInput()

        assertNull(viewModel.errorMessage.value)
        assertEquals(LoginState.PASSWORD_INPUT, viewModel.loginState.value)
    }

    /**
     * Property-based fuzz test: Random garbage strings (Unicode, boundary lengths, special characters)
     * must never cause an unhandled exception or crash in the ViewModel.
     */
    @Test
    fun `property test login input fuzzing never crashes and properly validates`() = runTest(testDispatcher) {
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('!', '@', '#', '$', '%', '^', '&', '*', ' ', '\n', '\t', '\u0000', 'ü', 'é')
        val random = Random(42)

        fakeAuthRepository.loginResult = Result.failure(Exception("Generic failure"))

        repeat(50) {
            val randomEmailLen = random.nextInt(0, 100)
            val randomPassLen = random.nextInt(0, 100)
            val randomEmail = (1..randomEmailLen).map { charPool[random.nextInt(charPool.size)] }.joinToString("")
            val randomPass = (1..randomPassLen).map { charPool[random.nextInt(charPool.size)] }.joinToString("")

            try {
                viewModel.updateEmail(randomEmail)
                viewModel.updatePassword(randomPass)
                viewModel.login(randomEmail, randomPass, LoginMode.EVERYONE) {}
                drainExecution()

                assertNotNull(viewModel.errorMessage.value)
                assertFalse(viewModel.isLoading.value)
            } catch (e: Throwable) {
                fail("ViewModel threw unhandled exception on fuzz input [email='$randomEmail', pass='$randomPass']: ${e.message}")
            }
        }
    }
}
