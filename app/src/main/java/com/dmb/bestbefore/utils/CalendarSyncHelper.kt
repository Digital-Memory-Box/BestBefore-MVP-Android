package com.dmb.bestbefore.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.dmb.bestbefore.data.models.CalendarEvent
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CalendarSyncHelper {

    private const val ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    fun hasCalendarAccess(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getUpcomingEvents(context: Context, limit: Int = 20): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )
        val now = System.currentTimeMillis()
        val selection = "${CalendarContract.Events.DTSTART} >= ?"
        val selectionArgs = arrayOf(now.toString())

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(CalendarContract.Events._ID)
                val titleCol = c.getColumnIndex(CalendarContract.Events.TITLE)
                val startCol = c.getColumnIndex(CalendarContract.Events.DTSTART)
                val endCol = c.getColumnIndex(CalendarContract.Events.DTEND)

                var count = 0
                while (c.moveToNext() && count < limit) {
                    val id = if (idCol >= 0) c.getString(idCol) ?: "" else ""
                    val title = if (titleCol >= 0) c.getString(titleCol) ?: "Untitled" else "Untitled"
                    val startTime = if (startCol >= 0) c.getLong(startCol) else now
                    val endTime = if (endCol >= 0) c.getLong(endCol) else startTime + 86400000L

                    events.add(
                        CalendarEvent(
                            id = id,
                            title = title,
                            startTime = Date(startTime),
                            endTime = Date(endTime),
                            location = null,
                            description = null
                        )
                    )
                    count++
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            // General query failure
        }
        return events
    }

    /**
     * Extracts (targetTimeMillis, targetHour, targetMinute) from a CalendarEvent's start time
     * using the device's local timezone.
     */
    fun extractEventTargetTime(event: CalendarEvent): Triple<Long, Int, Int> {
        val targetMillis = event.startTime.time
        val cal = Calendar.getInstance().apply {
            timeInMillis = targetMillis
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return Triple(targetMillis, hour, minute)
    }

    /**
     * Formats an epoch millisecond timestamp to UTC ISO-8601 string.
     */
    fun formatUtcIso8601(epochMillis: Long): String {
        return SimpleDateFormat(ISO_8601_PATTERN, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(epochMillis))
    }

    /**
     * Parses an ISO-8601 string into epoch milliseconds.
     * Supports both "Z" suffix and explicit "+00:00" timezone offsets returned by MongoDB.
     */
    fun parseIso8601(dateString: String?): Long {
        if (dateString.isNullOrBlank()) return 0L
        return try {
            OffsetDateTime.parse(dateString).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                ZonedDateTime.parse(dateString).toInstant().toEpochMilli()
            } catch (__: Exception) {
                0L
            }
        }
    }
}
