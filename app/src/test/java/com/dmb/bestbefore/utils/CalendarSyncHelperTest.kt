package com.dmb.bestbefore.utils

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.provider.CalendarContract
import com.dmb.bestbefore.data.models.CalendarEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class CalendarSyncHelperTest {

    @Test
    fun `hasCalendarAccess returns true when READ_CALENDAR granted`() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)

        assertTrue(CalendarSyncHelper.hasCalendarAccess(app))
    }

    @Test
    fun `hasCalendarAccess returns false when READ_CALENDAR denied`() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).denyPermissions(Manifest.permission.READ_CALENDAR)

        assertFalse(CalendarSyncHelper.hasCalendarAccess(app))
    }

    @Test
    fun `getUpcomingEvents reads cursor rows and maps to CalendarEvent list`() {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val matrixCursor = MatrixCursor(
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )
        )
        val startTime = 1700000000000L
        val endTime = 1700003600000L
        matrixCursor.addRow(arrayOf("101", "Team Sync", startTime, endTime))
        matrixCursor.addRow(arrayOf("102", "Sprint Demo", startTime + 10000, endTime + 10000))

        every { context.contentResolver } returns contentResolver
        every {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns matrixCursor

        val events = CalendarSyncHelper.getUpcomingEvents(context)

        assertEquals(2, events.size)
        assertEquals("101", events[0].id)
        assertEquals("Team Sync", events[0].title)
        assertEquals(startTime, events[0].startTime.time)
        assertEquals(endTime, events[0].endTime.time)
    }

    @Test
    fun `extractEventTargetTime accurately decomposes timestamp into millis, hour, and minute`() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 35)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val event = CalendarEvent(
            id = "test-1",
            title = "Release Meeting",
            startTime = cal.time,
            endTime = Date(cal.timeInMillis + 3600000),
            location = null,
            description = null
        )

        val (targetMillis, hour, minute) = CalendarSyncHelper.extractEventTargetTime(event)

        assertEquals(cal.timeInMillis, targetMillis)
        val localCal = Calendar.getInstance().apply { timeInMillis = targetMillis }
        assertEquals(localCal.get(Calendar.HOUR_OF_DAY), hour)
        assertEquals(localCal.get(Calendar.MINUTE), minute)
    }

    @Test
    fun `formatUtcIso8601 formats timestamp to UTC string ending in Z`() {
        val millis = 1704067200000L // 2024-01-01T00:00:00.000Z
        val iso = CalendarSyncHelper.formatUtcIso8601(millis)

        assertEquals("2024-01-01T00:00:00.000Z", iso)
    }

    @Test
    fun `parseIso8601 parses Z suffix format and returns epoch millis`() {
        val iso = "2024-01-01T00:00:00.000Z"
        val millis = CalendarSyncHelper.parseIso8601(iso)

        assertEquals(1704067200000L, millis)
    }

    @Test
    fun `parseIso8601_mongoDbOffsetFormatWithPlusZero_returnsCorrectEpochMillis`() {
        // Regression test: MongoDB returns ISO dates with "+00:00" timezone offsets (e.g. "2026-03-04T22:55:00.000+00:00").
        // SimpleDateFormat with literal 'Z' failed to parse these, causing unlock and expiration dates to return 0L.
        val mongoDbIsoString = "2026-03-04T22:55:00.000+00:00"
        val expectedEpochMillis = 1772664900000L

        val resultMillis = CalendarSyncHelper.parseIso8601(mongoDbIsoString)

        assertNotEquals("Should not return 0L failure default", 0L, resultMillis)
        assertEquals(expectedEpochMillis, resultMillis)
    }

    @Test
    fun `parseIso8601 returns 0L on null or blank or invalid date string`() {
        assertEquals(0L, CalendarSyncHelper.parseIso8601(null))
        assertEquals(0L, CalendarSyncHelper.parseIso8601(""))
        assertEquals(0L, CalendarSyncHelper.parseIso8601("invalid-iso-string"))
    }
}
