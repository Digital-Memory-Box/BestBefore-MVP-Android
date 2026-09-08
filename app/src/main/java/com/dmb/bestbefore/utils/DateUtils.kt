package com.dmb.bestbefore.utils

import java.time.Duration
import java.time.Instant

object DateUtils {
    fun formatCountdown(unlockDateIso: String?): String {
        if (unlockDateIso == null) return "No lock set"
        
        return try {
            val unlockDate = Instant.parse(unlockDateIso)
            formatCountdown(unlockDate.toEpochMilli())
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    fun formatCountdown(unlockTimeMillis: Long): String {
        val now = Instant.now().toEpochMilli()
        val diffMillis = unlockTimeMillis - now
        
        if (diffMillis <= 0) return "Unlocked"
        
        val duration = Duration.ofMillis(diffMillis)
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        
        return if (days > 0) {
            "${days}d ${hours}h ${minutes}m"
        } else if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    fun isLocked(unlockDateIso: String?): Boolean {
        if (unlockDateIso == null) return false
        return try {
            val unlockDate = Instant.parse(unlockDateIso)
            unlockDate.isAfter(Instant.now())
        } catch (e: Exception) {
            false
        }
    }
}
