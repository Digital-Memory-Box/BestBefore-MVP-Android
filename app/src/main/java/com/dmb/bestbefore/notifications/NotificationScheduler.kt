package com.dmb.bestbefore.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dmb.bestbefore.R

object NotificationScheduler {
    private const val CHANNEL_ID = "room_unlock_channel"
    private const val CHANNEL_NAME = "Room Unlock Notifications"
    
    fun scheduleRoomUnlockNotification(
        context: Context,
        roomId: String,
        roomName: String,
        unlockTimeMillis: Long
    ) {
        createNotificationChannel(context)
        
        val intent = Intent(context, RoomUnlockReceiver::class.java).apply {
            putExtra("ROOM_ID", roomId)
            putExtra("ROOM_NAME", roomName)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            roomId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    unlockTimeMillis,
                    pendingIntent
                )
            } else {
                // Fallback for when exact alarms are not permitted
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    unlockTimeMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                unlockTimeMillis,
                pendingIntent
            )
        }
    }
    
    fun cancelRoomUnlockNotification(context: Context, roomId: String) {
        val intent = Intent(context, RoomUnlockReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            roomId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for when time capsule rooms unlock"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

class RoomUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val roomId = intent.getStringExtra("ROOM_ID") ?: return
        val roomName = intent.getStringExtra("ROOM_NAME") ?: "Your Room"
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create intent to open room when notification is tapped - matching MainActivity deep link logic
        val openIntent = Intent(context, com.dmb.bestbefore.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = "com.dmb.bestbefore.OPEN_ROOM"
            putExtra("extra_room_id", roomId)
            putExtra("extra_room_name", roomName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            roomId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, "room_unlock_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this icon exists or use android default
            .setContentTitle("Time Capsule Unlocked")
            .setContentText("\"$roomName\" is now available to see")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(roomId.hashCode(), notification)
    }
}
