package com.ozang.bestbefore_mvp.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.app.ActivityCompat
import java.util.*

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val location: String?,
    val startTime: Date,
    val endTime: Date,
    val calendarName: String,
    val calendarColor: Int
)

class CalendarHelper(private val context: Context) {

    companion object {
        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.CALENDAR_COLOR
        )

        private const val PROJECTION_ID_INDEX = 0
        private const val PROJECTION_TITLE_INDEX = 1
        private const val PROJECTION_DESCRIPTION_INDEX = 2
        private const val PROJECTION_LOCATION_INDEX = 3
        private const val PROJECTION_START_INDEX = 4
        private const val PROJECTION_END_INDEX = 5
        private const val PROJECTION_CALENDAR_NAME_INDEX = 6
        private const val PROJECTION_CALENDAR_COLOR_INDEX = 7
    }

    fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getUpcomingEvents(daysAhead: Int = 30): List<CalendarEvent> {
        if (!checkPermission()) {
            return emptyList()
        }

        val events = mutableListOf<CalendarEvent>()
        val contentResolver: ContentResolver = context.contentResolver

        val startMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysAhead)
        }
        val endMillis = calendar.timeInMillis

        val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        val cursor: Cursor? = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_PROJECTION,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val eventId = it.getLong(PROJECTION_ID_INDEX)
                val title = it.getString(PROJECTION_TITLE_INDEX) ?: "Untitled Event"
                val description = it.getString(PROJECTION_DESCRIPTION_INDEX)
                val location = it.getString(PROJECTION_LOCATION_INDEX)
                val startTime = Date(it.getLong(PROJECTION_START_INDEX))
                val endTime = Date(it.getLong(PROJECTION_END_INDEX))
                val calendarName = it.getString(PROJECTION_CALENDAR_NAME_INDEX) ?: "Calendar"
                val calendarColor = it.getInt(PROJECTION_CALENDAR_COLOR_INDEX)

                events.add(
                    CalendarEvent(
                        id = eventId,
                        title = title,
                        description = description,
                        location = location,
                        startTime = startTime,
                        endTime = endTime,
                        calendarName = calendarName,
                        calendarColor = calendarColor
                    )
                )
            }
        }

        return events
    }

    fun getEventById(eventId: Long): CalendarEvent? {
        if (!checkPermission()) {
            return null
        }

        val contentResolver: ContentResolver = context.contentResolver
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)

        val cursor: Cursor? = contentResolver.query(
            uri,
            EVENT_PROJECTION,
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return CalendarEvent(
                    id = it.getLong(PROJECTION_ID_INDEX),
                    title = it.getString(PROJECTION_TITLE_INDEX) ?: "Untitled Event",
                    description = it.getString(PROJECTION_DESCRIPTION_INDEX),
                    location = it.getString(PROJECTION_LOCATION_INDEX),
                    startTime = Date(it.getLong(PROJECTION_START_INDEX)),
                    endTime = Date(it.getLong(PROJECTION_END_INDEX)),
                    calendarName = it.getString(PROJECTION_CALENDAR_NAME_INDEX) ?: "Calendar",
                    calendarColor = it.getInt(PROJECTION_CALENDAR_COLOR_INDEX)
                )
            }
        }

        return null
    }

    fun searchEvents(query: String): List<CalendarEvent> {
        if (!checkPermission()) {
            return emptyList()
        }

        val events = mutableListOf<CalendarEvent>()
        val contentResolver: ContentResolver = context.contentResolver

        val selection = "${CalendarContract.Events.TITLE} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        val cursor: Cursor? = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_PROJECTION,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val eventId = it.getLong(PROJECTION_ID_INDEX)
                val title = it.getString(PROJECTION_TITLE_INDEX) ?: "Untitled Event"
                val description = it.getString(PROJECTION_DESCRIPTION_INDEX)
                val location = it.getString(PROJECTION_LOCATION_INDEX)
                val startTime = Date(it.getLong(PROJECTION_START_INDEX))
                val endTime = Date(it.getLong(PROJECTION_END_INDEX))
                val calendarName = it.getString(PROJECTION_CALENDAR_NAME_INDEX) ?: "Calendar"
                val calendarColor = it.getInt(PROJECTION_CALENDAR_COLOR_INDEX)

                events.add(
                    CalendarEvent(
                        id = eventId,
                        title = title,
                        description = description,
                        location = location,
                        startTime = startTime,
                        endTime = endTime,
                        calendarName = calendarName,
                        calendarColor = calendarColor
                    )
                )
            }
        }

        return events
    }
}

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val color: Int,
    val accountName: String,
    val accountType: String
)