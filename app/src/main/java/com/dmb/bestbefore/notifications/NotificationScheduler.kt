package com.dmb.bestbefore.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    
    fun scheduleRoomUnlock(context: Context, roomId: String, roomName: String, unlockTimeMillis: Long) {
        val delayMillis = unlockTimeMillis - System.currentTimeMillis()
        if (delayMillis <= 0) {
            Log.d(TAG, "Unlock time is in the past for room $roomId, skipping")
            return
        }
        
        val inputData = workDataOf(
            "roomId" to roomId,
            "roomName" to roomName
        )
        
        val workRequest = OneTimeWorkRequestBuilder<RoomUnlockWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("room_unlock_$roomId")
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "room_unlock_$roomId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        
        Log.d(TAG, "Scheduled unlock for room $roomId in ${delayMillis / 1000}s")
    }
    
    fun cancelRoomUnlock(context: Context, roomId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("room_unlock_$roomId")
    }
}
