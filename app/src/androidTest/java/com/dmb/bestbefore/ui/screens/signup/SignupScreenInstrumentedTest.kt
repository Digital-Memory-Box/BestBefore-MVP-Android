package com.dmb.bestbefore.ui.screens.signup

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
import com.dmb.bestbefore.data.api.models.UpdateMeRequest
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.data.repository.AuthRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class FakeInstrumentedSignupRepository(context: Context) : AuthRepository(context) {
    var syncBackendResult: Result<UserDto> = Result.success(UserDto(id = "user_instr_99", name = "Verified User", email = "test@bestbefore.app"))
    var updateMeResult: Result<UserDto> = Result.success(UserDto(id = "user_instr_99", name = "Verified User", email = "test@bestbefore.app"))

    override suspend fun syncWithBackend(firebaseIdToken: String): Result<UserDto> = syncBackendResult
    override suspend fun updateMe(updates: UpdateMeRequest): Result<UserDto> = updateMeResult
    override suspend fun syncFcmToken(): Result<Unit> = Result.success(Unit)
}

@RunWith(AndroidJUnit4::class)
class SignupScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var fakeRepository: FakeInstrumentedSignupRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: SignupViewModel
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.setOrientationNatural()

        fakeRepository = FakeInstrumentedSignupRepository(context)
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        viewModel = SignupViewModel(
            application = context as Application,
            repository = fakeRepository,
            sessionManager = sessionManager
        )
    }

    @After
    fun tearDown() {
        device.setOrientationNatural()
    }

    @Test
    fun happyPath_signupForm_selectsArtistAndFillsInputs() {
        composeTestRule.setContent {
            SignupScreen(
                onNavigateBack = {},
                onSignupSuccess = {},
                viewModel = viewModel
            )
        }

        // Fill Name, Email, Password
        composeTestRule.onNodeWithText("name").performTextInput("Taylor Swift")
        composeTestRule.onNodeWithText("email").performTextInput("taylor@bestbefore.app")
        composeTestRule.onNodeWithText("password").performTextInput("secret123")

        // Switch to Artist user type
        composeTestRule.onNodeWithText("Artist").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Taylor Swift", viewModel.name.value)
        assertEquals("taylor@bestbefore.app", viewModel.email.value)
        assertEquals("secret123", viewModel.password.value)
        assertEquals("artist", viewModel.userType.value)
    }

    @Test
    fun errorState_invalidEmail_displaysValidationError() {
        composeTestRule.setContent {
            SignupScreen(
                onNavigateBack = {},
                onSignupSuccess = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("name").performTextInput("Test User")
        composeTestRule.onNodeWithText("email").performTextInput("not-an-email")
        composeTestRule.onNodeWithText("password").performTextInput("pass123")

        // Trigger attempt signup
        viewModel.attemptSignup()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Please enter a valid email").assertIsDisplayed()
    }

    @Test
    fun rotation_preservesSignupDraftInputs() {
        composeTestRule.setContent {
            SignupScreen(
                onNavigateBack = {},
                onSignupSuccess = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("name").performTextInput("Rotation Artist")
        composeTestRule.onNodeWithText("email").performTextInput("rotate@bestbefore.app")

        device.setOrientationLeft()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rotation Artist").assertIsDisplayed()
        composeTestRule.onNodeWithText("rotate@bestbefore.app").assertIsDisplayed()

        device.setOrientationNatural()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rotation Artist").assertIsDisplayed()
    }

    @Test
    fun backgrounding_preservesStateOnAppRelaunch() {
        composeTestRule.setContent {
            SignupScreen(
                onNavigateBack = {},
                onSignupSuccess = {},
                viewModel = viewModel
            )
        }

        val uniqueName = "Backgrounded User"
        composeTestRule.onNodeWithText("name").performTextInput(uniqueName)

        device.pressHome()

        val launcherIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        if (launcherIntent != null) {
            context.startActivity(launcherIntent)
            device.wait(Until.hasObject(By.pkg(context.packageName)), 5000)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(uniqueName).assertExists()
    }
}
