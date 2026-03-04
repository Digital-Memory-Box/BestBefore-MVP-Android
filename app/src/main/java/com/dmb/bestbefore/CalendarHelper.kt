package com.dmb.bestbefore

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.dmb.bestbefore.data.models.CalendarEvent

object CalendarHelper {
    fun getUpcomingEvents(context: Context): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        
        // Define projection
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )
        
        // Time constraints: from now onwards
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
                while (c.moveToNext() && count < 20) { // Limit to 20 upcoming events
                    val id = if (idCol >= 0) c.getString(idCol) else ""
                    val title = if (titleCol >= 0) c.getString(titleCol) ?: "Untitled" else "Untitled"
                    val startTime = if (startCol >= 0) c.getLong(startCol) else now
                    val endTime = if (endCol >= 0) c.getLong(endCol) else startTime + 86400000L // +1 Day Default
                    
                    events.add(CalendarEvent(id, title, java.util.Date(startTime), java.util.Date(endTime), null, null))
                    count++
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Ignore if no permission
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return events
    }
}
