package com.dmb.bestbefore.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun formatCountdown(unlockDateIso: String?): String {
        if (unlockDateIso == null) return "No lock set"
        
        return try {
            val unlockDate = isoFormat.parse(unlockDateIso) ?: return "Invalid date"
            val now = Date()
            val diff = unlockDate.time - now.time
            
            if (diff <= 0) return "Unlocked"
            
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            
            if (days > 0) {
                "${days}d ${hours}h ${minutes}m"
            } else if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        } catch (e: Exception) {
            "Invalid date"
        }
    }
    
    fun isLocked(unlockDateIso: String?): Boolean {
        if (unlockDateIso == null) return false
        return try {
            val unlockDate = isoFormat.parse(unlockDateIso) ?: return false
            unlockDate.after(Date())
        } catch (e: Exception) {
            false
        }
    }
}
