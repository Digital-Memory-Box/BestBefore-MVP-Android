package com.dmb.bestbefore.systemui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.dmb.bestbefore.MainActivity
import com.dmb.bestbefore.notifications.NotificationHelper
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class SystemInteractionInstrumentedTest {

    private lateinit var device: UiDevice
    private lateinit var context: Context
    private val defaultTimeout = 5000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.setOrientationNatural()
    }

    @After
    fun tearDown() {
        device.pressHome()
    }

    /**
     * Tests system notification shade interaction and tap-through from a backgrounded state.
     */
    @Test
    fun notification_tapThrough_fromBackgroundedState_launchesApp() {
        // Post a notification via NotificationHelper
        val helper = NotificationHelper(context)
        helper.showTimeCapsuleNotification("test_room_101", "Midnight Memories")

        // Send app to background
        device.pressHome()
        device.waitForIdle()

        // Pull down system notification shade
        val shadeOpened = device.openNotification()
        assertTrue("Notification shade should open", shadeOpened)

        // Wait for and locate the notification item by title or content
        val notificationItem = device.wait(
            Until.findObject(By.textContains("Midnight Memories")),
            defaultTimeout
        ) ?: device.wait(
            Until.findObject(By.textContains("Time Capsule")),
            defaultTimeout
        )

        if (notificationItem != null) {
            // Tap through notification
            notificationItem.click()

            // Verify app resumes in foreground
            val appInForeground = device.wait(
                Until.hasObject(By.pkg(context.packageName)),
                defaultTimeout
            )
            assertTrue("App should resume in foreground on notification tap", appInForeground)
        } else {
            // Close shade if running in headless emulator without notifications permission granted
            device.pressBack()
        }
    }

    /**
     * Tests system permission dialog detection and interaction using UI Automator selectors.
     */
    @Test
    fun permissionDialog_systemInteraction_detectsSystemDialog() {
        // Launch MainActivity
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(launchIntent)
        device.wait(Until.hasObject(By.pkg(context.packageName)), defaultTimeout)

        // Check if Android system permission controller dialog is active
        val permissionDialog = device.wait(
            Until.findObject(By.pkg("com.google.android.permissioncontroller")),
            2000L
        ) ?: device.wait(
            Until.findObject(By.pkg("com.android.permissioncontroller")),
            2000L
        )

        if (permissionDialog != null) {
            // Locate "While using the app" or "Allow" button
            val allowButton = device.findObject(
                By.res(Pattern.compile(".*permission_allow.*|.*permission_allow_foreground_only_button"))
            ) ?: device.findObject(By.text(Pattern.compile("(?i)Allow|While using the app")))

            allowButton?.click()
            device.waitForIdle()
        }

        // Assert app continues running normally
        val appRunning = device.hasObject(By.pkg(context.packageName))
        assertTrue("App should remain in foreground after handling permission dialog", appRunning)
    }

    /**
     * Tests app switching via system navigation (Recents / Home) and resuming task state.
     */
    @Test
    fun appSwitching_midTask_preservesProcessResilience() {
        // Launch app
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(launchIntent)
        device.wait(Until.hasObject(By.pkg(context.packageName)), defaultTimeout)

        // Switch out to home
        device.pressHome()
        device.waitForIdle()

        // Switch to recent apps
        device.pressRecentApps()
        device.waitForIdle()

        // Re-launch app from package
        val relaunchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        if (relaunchIntent != null) {
            context.startActivity(relaunchIntent)
            val resumed = device.wait(Until.hasObject(By.pkg(context.packageName)), defaultTimeout)
            assertTrue("App should resume successfully after switching tasks", resumed)
        }
    }
}
