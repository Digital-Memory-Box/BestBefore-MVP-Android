package com.dmb.bestbefore.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dmb.bestbefore.MainActivity
import com.dmb.bestbefore.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "time_capsule_channel"
        const val CHANNEL_NAME = "Time Capsule Notifications"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for Time Capsule completion"
            enableVibration(true)
            enableLights(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleTimeCapsuleNotification() {
        // Stub: WorkManager removed, no background scheduling
    }



    fun showTimeCapsuleNotification(roomId: String, roomName: String) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = "com.dmb.bestbefore.OPEN_ROOM"
            putExtra("extra_room_id", roomId)
            putExtra("extra_room_name", roomName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            roomId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time Capsule Complete")
            .setContentText("Your room \"$roomName\" is now unlocked!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

class TimeCapsuleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val roomId = intent.getStringExtra("roomId") ?: return
        val roomName = intent.getStringExtra("roomName") ?: "Your Room"

        NotificationHelper(context).showTimeCapsuleNotification(roomId, roomName)
    }
}