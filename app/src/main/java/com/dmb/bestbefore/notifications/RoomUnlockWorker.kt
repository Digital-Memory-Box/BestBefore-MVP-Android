package com.dmb.bestbefore.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RoomUnlockWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val roomId = inputData.getString("roomId") ?: return Result.failure()
        val roomName = inputData.getString("roomName") ?: "Your room"
        
        NotificationHelper(applicationContext).showTimeCapsuleNotification(roomId, roomName)
        com.dmb.bestbefore.analytics.AnalyticsManager.logUnlockCapsule(roomId, roomName)
        return Result.success()
    }
}
