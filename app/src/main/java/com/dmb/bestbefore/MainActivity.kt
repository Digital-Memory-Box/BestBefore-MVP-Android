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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle deep link from notification
        handleNotificationIntent(intent)
        
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

        if (roomId != null && roomName != null) {
            // Store for navigation to pick up
            pendingRoomId = roomId
            pendingRoomName = roomName
        }
    }
    
    companion object {
        var pendingRoomId: String? = null
        var pendingRoomName: String? = null
        
        fun clearPending() {
            pendingRoomId = null
            pendingRoomName = null
        }
    }
}