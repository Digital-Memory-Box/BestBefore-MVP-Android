package com.dmb.bestbefore.ui.screens.signup

import android.app.Application
import android.os.Looper
import com.dmb.bestbefore.data.api.models.UpdateMeRequest
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.data.repository.AuthRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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

class FakeSignupAuthRepository(application: Application) : AuthRepository(application) {
    var syncBackendResult: Result<UserDto> = Result.success(UserDto(id = "user99", name = "Verified User", email = "test@bestbefore.app"))
    var updateMeResult: Result<UserDto> = Result.success(UserDto(id = "user99", name = "Verified User", email = "test@bestbefore.app"))
    var syncFcmTokenResult: Result<Unit> = Result.success(Unit)

    var syncFcmTokenCalls = 0

    override suspend fun syncWithBackend(firebaseIdToken: String): Result<UserDto> {
        return syncBackendResult
    }

    override suspend fun updateMe(updates: UpdateMeRequest): Result<UserDto> {
        return updateMeResult
    }

    override suspend fun syncFcmToken(): Result<Unit> {
        syncFcmTokenCalls++
        return syncFcmTokenResult
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var fakeAuthRepository: FakeSignupAuthRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var viewModel: SignupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = RuntimeEnvironment.getApplication()
        fakeAuthRepository = FakeSignupAuthRepository(application)
        sessionManager = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)

        viewModel = SignupViewModel(
            application = application,
            repository = fakeAuthRepository,
            sessionManager = sessionManager,
            firebaseAuth = firebaseAuth
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
    fun `attemptSignup with invalid email format sets error without calling Firebase`() {
        viewModel.updateName("Test User")
        viewModel.updateEmail("invalid-email-format")
        viewModel.updatePassword("strongpass123")

        viewModel.attemptSignup()

        assertEquals("Please enter a valid email", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
        verify(exactly = 0) { firebaseAuth.createUserWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun `attemptSignup with short password sets password length error`() {
        viewModel.updateName("Test User")
        viewModel.updateEmail("valid@test.com")
        viewModel.updatePassword("12345") // < 6 chars

        viewModel.attemptSignup()

        assertEquals("Password must be at least 6 characters", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
        verify(exactly = 0) { firebaseAuth.createUserWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun `checkVerificationStatus when user is verified syncs backend and emits success`() = runTest(testDispatcher) {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        val tokenResult = mockk<GetTokenResult>(relaxed = true)
        every { tokenResult.token } returns "mock-firebase-token"

        every { firebaseAuth.currentUser } returns mockUser
        every { mockUser.isEmailVerified } returns true
        every { mockUser.reload() } returns Tasks.forResult(null)
        every { mockUser.getIdToken(false) } returns Tasks.forResult(tokenResult)

        val userDto = UserDto(id = "user99", name = "Verified User", email = "test@bestbefore.app", userType = "normal")
        fakeAuthRepository.syncBackendResult = Result.success(userDto)
        fakeAuthRepository.updateMeResult = Result.success(userDto)

        viewModel.updateName("Verified User")
        viewModel.updateUserType("normal")

        val emissions = mutableListOf<String>()
        val collectJob = launch {
            viewModel.signupSuccess.collect { emissions.add(it) }
        }

        viewModel.checkVerificationStatus()
        drainExecution()

        assertEquals(listOf("test@bestbefore.app"), emissions)
        verify { sessionManager.saveUser(userDto) }
        assertEquals(1, fakeAuthRepository.syncFcmTokenCalls)
        assertFalse(viewModel.isLoading.value)
        collectJob.cancel()
    }

    @Test
    fun `checkVerificationStatus when email is not verified sets pending notification`() = runTest(testDispatcher) {
        val mockUser = mockk<FirebaseUser>(relaxed = true)

        every { firebaseAuth.currentUser } returns mockUser
        every { mockUser.isEmailVerified } returns false
        every { mockUser.reload() } returns Tasks.forResult(null)

        viewModel.checkVerificationStatus()
        drainExecution()

        assertEquals("Email not yet verified. Please check your inbox.", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `cancelSignup resets loading and displays cancellation message`() {
        viewModel.cancelSignup()

        assertFalse(viewModel.isLoading.value)
        assertEquals("Signup cancelled. Please try again.", viewModel.errorMessage.value)
    }

    /**
     * Property-based fuzz test: Random string inputs into fields should never throw an uncaught crash.
     */
    @Test
    fun `property test signup input fuzzing handles random strings gracefully`() {
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('!', '@', '#', '$', '%', '^', '&', '*', ' ', '\n', '\t', '\u0000', 'ñ')
        val random = Random(123)

        repeat(50) {
            val randomName = (1..random.nextInt(0, 50)).map { charPool[random.nextInt(charPool.size)] }.joinToString("")
            val randomEmail = (1..random.nextInt(0, 50)).map { charPool[random.nextInt(charPool.size)] }.joinToString("")
            val randomPass = (1..random.nextInt(0, 50)).map { charPool[random.nextInt(charPool.size)] }.joinToString("")

            try {
                viewModel.updateName(randomName)
                viewModel.updateEmail(randomEmail)
                viewModel.updatePassword(randomPass)
                viewModel.attemptSignup()

                assertNotNull(viewModel.errorMessage)
            } catch (e: Throwable) {
                fail("SignupViewModel threw unexpected exception on fuzz input [name='$randomName', email='$randomEmail', pass='$randomPass']: ${e.message}")
            }
        }
    }
}
