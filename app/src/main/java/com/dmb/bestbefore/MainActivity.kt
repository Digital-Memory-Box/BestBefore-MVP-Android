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

        setContent {
            BestBeforeTheme {
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
        // Check for both the action AND/OR just the extras, as PendingIntent might not always set action perfectly depending on launch mode
        val roomId = intent?.getStringExtra("extra_room_id")
        val roomName = intent?.getStringExtra("extra_room_name")
        val isInvite = intent?.getBooleanExtra("isInvite", false) == true

        if (roomId != null && roomName != null) {
            if (isInvite) {
                pendingInviteRoomId = roomId
                pendingInviteRoomName = roomName
            } else {
                // Store for navigation to pick up
                pendingRoomId = roomId
                pendingRoomName = roomName
            }
        }
    }
    
    companion object {
        var pendingRoomId: String? = null
        var pendingRoomName: String? = null
        var pendingInviteRoomId: String? = null
        var pendingInviteRoomName: String? = null
        
        fun clearPending() {
            pendingRoomId = null
            pendingRoomName = null
        }
        
        fun clearPendingInvite() {
            pendingInviteRoomId = null
            pendingInviteRoomName = null
        }
    }
}