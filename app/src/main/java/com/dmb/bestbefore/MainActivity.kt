package com.dmb.bestbefore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dmb.bestbefore.ui.navigation.AppNavigation
import com.dmb.bestbefore.ui.theme.BestBeforeTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Handle deep link from notification
        handleNotificationIntent(intent)

        // Request Notification Permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    android.util.Log.d("MainActivity", "Notification permission granted")
                } else {
                    android.util.Log.d("MainActivity", "Notification permission denied")
                }
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Sync FCM Token
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (token != null) {
                    val appContext = applicationContext
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            com.dmb.bestbefore.data.repository.AuthRepository(appContext).updateMe(
                                com.dmb.bestbefore.data.api.models.UpdateMeRequest(fcmToken = token)
                            )
                        } catch(e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to sync FCM Token", e)
                        }
                    }
                }
            }
        }

        com.dmb.bestbefore.ui.theme.ThemeState.init(this)

        setContent {
            BestBeforeTheme(
                appTheme = com.dmb.bestbefore.ui.theme.ThemeState.currentTheme,
                accentColor = com.dmb.bestbefore.ui.theme.ThemeState.currentAccent
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
    
    private fun handleNotificationIntent(intent: android.content.Intent?) {
        val data = intent?.data

        if (data != null) {
            val scheme = data.scheme
            val host = data.host
            val path = data.path ?: ""

            when {
                // ── HTTPS App Links (cross-platform, opened from browser or iOS share) ─────
                scheme == "https" && host == "bestbefore.up.railway.app" -> {
                    when {
                        // https://bestbefore.up.railway.app/join/{roomId}
                        path.startsWith("/join/") -> {
                            val roomId = path.removePrefix("/join/").trim('/')
                            if (roomId.isNotEmpty()) {
                                pendingQRRoomId = roomId
                                android.util.Log.d("MainActivity", "HTTPS App Link: join room $roomId")
                            }
                        }
                        // https://bestbefore.up.railway.app/invite-join/{token}
                        path.startsWith("/invite-join/") -> {
                            val token = path.removePrefix("/invite-join/").trim('/')
                            if (token.isNotEmpty()) {
                                pendingInviteToken = token
                                android.util.Log.d("MainActivity", "HTTPS App Link: invite token $token")
                            }
                        }
                    }
                }

                // ── Custom scheme: invite token (bestbefore://invite/{token}) ──────────────
                scheme == "bestbefore" && host == "invite" -> {
                    val token = data.pathSegments?.firstOrNull()
                    if (!token.isNullOrEmpty()) {
                        pendingInviteToken = token
                        android.util.Log.d("MainActivity", "Custom scheme invite token: $token")
                    }
                }

                // ── Custom scheme: direct room open (bestbefore://room/{roomId}) ──────────
                scheme == "bestbefore" && host == "room" -> {
                    val roomId = data.pathSegments?.firstOrNull()
                    if (!roomId.isNullOrEmpty()) {
                        pendingQRRoomId = roomId
                        android.util.Log.d("MainActivity", "Custom scheme room: $roomId")
                    }
                }
            }
            return
        }

        // Handle notification intent extras (FCM push-tap)
        val roomId = intent?.getStringExtra("extra_room_id")
        val roomName = intent?.getStringExtra("extra_room_name")
        val isInvite = intent?.getBooleanExtra("isInvite", false) == true
        val navigateToNotifs = intent?.getBooleanExtra("navigate_to_notifications", false) == true

        if (navigateToNotifs) {
            pendingNavigateToNotifications = true
        } else if (roomId != null && roomName != null) {
            if (isInvite) {
                pendingInviteRoomId = roomId
                pendingInviteRoomName = roomName
            } else {
                pendingRoomId = roomId
                pendingRoomName = roomName
            }
        }
    }
    
    companion object {
        var pendingNavigateToNotifications: Boolean = false
        var pendingRoomId: String? = null
        var pendingRoomName: String? = null
        var pendingInviteRoomId: String? = null
        var pendingInviteRoomName: String? = null
        var pendingInviteToken: String? = null
        /** Room ID received via QR scan or /join/{id} HTTPS App Link */
        var pendingQRRoomId: String? = null
        
        fun clearPending() {
            pendingNavigateToNotifications = false
            pendingRoomId = null
            pendingRoomName = null
        }
        
        fun clearPendingInvite() {
            pendingInviteRoomId = null
            pendingInviteRoomName = null
        }

        fun clearPendingInviteToken() {
            pendingInviteToken = null
        }

        fun clearPendingQRRoomId() {
            pendingQRRoomId = null
        }
    }
}