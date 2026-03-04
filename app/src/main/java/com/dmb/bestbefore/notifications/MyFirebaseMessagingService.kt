package com.dmb.bestbefore.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dmb.bestbefore.MainActivity
import com.dmb.bestbefore.R
import com.dmb.bestbefore.data.models.AppNotification
import com.dmb.bestbefore.data.models.NotificationType
import com.dmb.bestbefore.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")
        // The token will be sent to the backend when the user logs in or next opens the app
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title ?: "BestBefore"
        val body = remoteMessage.notification?.body ?: "You have a new notification"
        
        val typeStr = remoteMessage.data["type"]
        val roomId = remoteMessage.data["roomId"]
        val roomName = remoteMessage.data["roomName"]

        if (roomId != null && roomName != null) {
            val notifType = when (typeStr) {
                "INVITATION" -> NotificationType.INVITATION
                "INVITE_ACCEPTED" -> NotificationType.GENERAL
                else -> NotificationType.GENERAL
            }
            
            // Save to internal app notifications
            val appNotification = AppNotification(
                title = title,
                message = body,
                type = notifType,
                relatedRoomId = roomId,
                relatedRoomName = roomName
            )
            val repo = NotificationRepository(applicationContext)
            repo.addNotification(appNotification)

            // Show Android System Notification
            val isInvite = typeStr == "INVITATION"
            showSystemNotification(title, body, roomId, roomName, isInvite)
        }
    }

    private fun showSystemNotification(title: String, body: String, roomId: String, roomName: String, isInvite: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "invitations_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Room Invitations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Room Invitations"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("extra_room_id", roomId)
            putExtra("extra_room_name", roomName)
            putExtra("isInvite", isInvite)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            roomId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Use real app icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(roomId.hashCode(), notification)
    }
}
