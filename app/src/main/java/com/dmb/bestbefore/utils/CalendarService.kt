package com.dmb.bestbefore.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Calendar

data class CelebrationEvent(
    val title: String,
    val calendarName: String
)

class CalendarService private constructor() {

    companion object {
        val shared = CalendarService()
        private const val TAG = "CalendarService"
    }

    /**
     * Checks if the app has READ_CALENDAR permissions.
     * Note: Requesting permissions dynamically in Android must be handled by an Activity or Fragment.
     * This utility checks if the permission is already granted.
     */
    fun hasAccess(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Scans for birthdays and holiday events occurring today.
     * Corresponds exactly to the iOS version logic.
     */
    fun fetchTodayCelebrations(context: Context): List<CelebrationEvent> {
        if (!hasAccess(context)) {
            Log.w(TAG, "Calendar access not granted.")
            return emptyList()
        }

        val celebrations = mutableListOf<CelebrationEvent>()

        val now = Calendar.getInstance()
        
        // Start of today
        val startOfDay = now.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        // End of today
        val endOfDay = startOfDay.clone() as Calendar
        endOfDay.add(Calendar.DAY_OF_YEAR, 1)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        )

        // For Instances table, we must use content URI built with start/end time
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startOfDay.timeInMillis)
        android.content.ContentUris.appendId(builder, endOfDay.timeInMillis)

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                null
            )

            cursor?.use { c ->
                val titleCol = c.getColumnIndex(CalendarContract.Instances.TITLE)
                val calNameCol = c.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

                while (c.moveToNext()) {
                    val title = if (titleCol >= 0) c.getString(titleCol) ?: "" else ""
                    val calendarName = if (calNameCol >= 0) c.getString(calNameCol) ?: "" else ""

                    val titleLower = title.lowercase()
                    val calendarNameLower = calendarName.lowercase()

                    // Check for birthday keywords
                    val isBirthday = titleLower.contains("birthday") ||
                            calendarNameLower.contains("birthday")

                    // Check for holiday keywords
                    val isHoliday = titleLower.contains("holiday") ||
                            titleLower.contains("celebration") ||
                            calendarNameLower.contains("holiday")

                    if (isBirthday || isHoliday) {
                        Log.d(TAG, "Suggested event: ${title.ifEmpty { "Untitled" }} from $calendarName")
                        celebrations.add(CelebrationEvent(title, calendarName))
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching events: ${e.message}")
        }

        return celebrations
    }
}
