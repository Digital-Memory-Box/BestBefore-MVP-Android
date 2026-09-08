package com.dmb.bestbefore.ui.screens.login

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.data.repository.AuthRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class FakeInstrumentedAuthRepository(context: Context) : AuthRepository(context) {
    var loginResult: Result<UserDto> = Result.success(UserDto(id = "instr_1", name = "Instr User", email = "test@bestbefore.app"))
    var shouldFail: Boolean = false
    var failureMessage: String = "Invalid credentials"

    override suspend fun login(email: String, password: String): Result<UserDto> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            loginResult
        }
    }

    override suspend fun syncFcmToken(): Result<Unit> = Result.success(Unit)
}

@RunWith(AndroidJUnit4::class)
class LoginScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var fakeRepository: FakeInstrumentedAuthRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.setOrientationNatural()
        fakeRepository = FakeInstrumentedAuthRepository(context)
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        viewModel = LoginViewModel(
            application = context as Application,
            repository = fakeRepository,
            sessionManager = sessionManager,
            isNetworkAvailable = { true }
        )
    }

    @After
    fun tearDown() {
        device.setOrientationNatural()
    }

    @Test
    fun happyPath_loginSuccess_triggersCallback() {
        var loginSuccessCalled = false

        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = { loginSuccessCalled = true },
                onNavigateToSignup = {},
                viewModel = viewModel
            )
        }

        // Enter email and password
        composeTestRule.onNodeWithText("email or nickname").performTextInput("test@bestbefore.app")
        composeTestRule.onNodeWithText("password").performTextInput("secure123")

        // Click Login button
        composeTestRule.onNodeWithText("Login").performClick()

        // Wait for Compose idle
        composeTestRule.waitForIdle()

        assertTrue("Login success callback should be invoked", loginSuccessCalled)
    }

    @Test
    fun errorState_loginFailure_displaysErrorMessage() {
        fakeRepository.shouldFail = true
        fakeRepository.failureMessage = "No account found with this email."

        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = {},
                onNavigateToSignup = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("email or nickname").performTextInput("unknown@bestbefore.app")
        composeTestRule.onNodeWithText("password").performTextInput("wrongpass")
        composeTestRule.onNodeWithText("Login").performClick()

        composeTestRule.waitForIdle()

        // Verify error view/message appears in the UI
        composeTestRule.onNodeWithText("No account found with this email.").assertIsDisplayed()
    }

    @Test
    fun rotation_preservesInputDraftText() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = {},
                onNavigateToSignup = {},
                viewModel = viewModel
            )
        }

        val draftEmail = "persistent@bestbefore.app"
        composeTestRule.onNodeWithText("email or nickname").performTextInput(draftEmail)

        // Rotate device to landscape
        device.setOrientationLeft()
        composeTestRule.waitForIdle()

        // Assert state survives rotation
        composeTestRule.onNodeWithText(draftEmail).assertIsDisplayed()

        // Rotate back to portrait
        device.setOrientationNatural()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(draftEmail).assertIsDisplayed()
    }

    @Test
    fun backgrounding_preservesStateOnRelaunch() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = {},
                onNavigateToSignup = {},
                viewModel = viewModel
            )
        }

        val testEmail = "background@bestbefore.app"
        composeTestRule.onNodeWithText("email or nickname").performTextInput(testEmail)

        // Press Home to background the app
        device.pressHome()

        // Relaunch app from package
        val launcherIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        if (launcherIntent != null) {
            context.startActivity(launcherIntent)
            device.wait(Until.hasObject(By.pkg(context.packageName)), 5000)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(testEmail).assertExists()
    }
}
