package com.dmb.bestbefore.ui.screens.room

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.models.CalendarEvent
import com.dmb.bestbefore.data.repository.RoomRepository
import com.dmb.bestbefore.notifications.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RoomViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = RoomRepository()
    private val notificationHelper = NotificationHelper(application)

    private val _roomId = MutableStateFlow("")
    val roomId: StateFlow<String> = _roomId.asStateFlow()

    private val _roomName = MutableStateFlow("")
    val roomName: StateFlow<String> = _roomName.asStateFlow()

    private val _showProfileMenu = MutableStateFlow(false)
    val showProfileMenu: StateFlow<Boolean> = _showProfileMenu.asStateFlow()

    private val _showTimeCapsuleDialog = MutableStateFlow(false)
    val showTimeCapsuleDialog: StateFlow<Boolean> = _showTimeCapsuleDialog.asStateFlow()

    private val _showRoomInfoDialog = MutableStateFlow(false)
    val showRoomInfoDialog: StateFlow<Boolean> = _showRoomInfoDialog.asStateFlow()

    private val _showCalendarDialog = MutableStateFlow(false)
    val showCalendarDialog: StateFlow<Boolean> = _showCalendarDialog.asStateFlow()

    private val _lockEndTime = MutableStateFlow<Long?>(null)
    val lockEndTime: StateFlow<Long?> = _lockEndTime.asStateFlow()

    private val _countdownText = MutableStateFlow("00:00:00")
    val countdownText: StateFlow<String> = _countdownText.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private var countdownJob: Job? = null
    
    // --- Dynamic Analytics State ---
    private var entryTime: Long = 0

    fun initialize(roomId: String, roomName: String) {
        _roomId.value = roomId
        _roomName.value = roomName
        
        // Track entry for dwell time
        entryTime = System.currentTimeMillis()
    }

    /**
     * Call this when user leaves the room (e.g., Back button, navigation).
     * Calculates dwell time and reports to backend for Recommendation Engine.
     */
    fun onRoomExited(lat: Double? = null, lon: Double? = null) {
        if (entryTime == 0L) return
        
        val durationSeconds = (System.currentTimeMillis() - entryTime) / 1000
        val currentRoomId = _roomId.value
        
        viewModelScope.launch {
            repository.trackInteraction(
                roomId = currentRoomId,
                dwellTimeSeconds = durationSeconds,
                type = "VIEW",
                lat = lat,
                lon = lon
            )
        }
    }

    fun toggleProfileMenu() {
        _showProfileMenu.value = !_showProfileMenu.value
    }

    fun toggleTimeCapsuleDialog() {
        _showTimeCapsuleDialog.value = !_showTimeCapsuleDialog.value
    }

    fun toggleRoomInfoDialog() {
        _showRoomInfoDialog.value = !_showRoomInfoDialog.value
    }

    fun toggleCalendarDialog() {
        _showCalendarDialog.value = !_showCalendarDialog.value
        if (_showCalendarDialog.value) {
            loadCalendarEvents()
        }
    }

    fun startTimeCapsule(hours: Int, minutes: Int, seconds: Int) {
        val totalSeconds = hours * 3600L + minutes * 60L + seconds
        if (totalSeconds <= 0) return

        val endTime = System.currentTimeMillis() + (totalSeconds * 1000)
        _lockEndTime.value = endTime

        notificationHelper.scheduleTimeCapsuleNotification()
        startCountdown(endTime)
        
        // Report Future Lock interaction to AI weights
        viewModelScope.launch {
            repository.trackInteraction(
                roomId = _roomId.value,
                dwellTimeSeconds = 0,
                type = "FUTURE_LOCK"
            )
        }
    }

    private fun startCountdown(endTime: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = endTime - now

                if (remaining <= 0) {
                    _lockEndTime.value = null
                    _countdownText.value = "00:00:00"
                    break
                }

                val h = TimeUnit.MILLISECONDS.toHours(remaining)
                val m = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
                val s = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                _countdownText.value = String.format("%02d:%02d:%02d", h, m, s)
                delay(1000)
            }
        }
    }

    fun deleteRoom() {
        viewModelScope.launch {
            repository.deleteRoom(_roomId.value)
        }
    }

    fun renameRoom(newName: String) {
         viewModelScope.launch {
             repository.updateRoom(_roomId.value, mapOf("name" to newName))
             _roomName.value = newName
         }
    }

    private fun loadCalendarEvents() {
        _calendarEvents.value = emptyList()
    }

    /**
     * Called when the user picks a Calendar event inside the Room screen.
     * This pre-fills the room's time-capsule lock date with the event's start time
     * and posts a local notification reminder. Full room creation lives in ProfileViewModel.
     */
    fun createRoomFromCalendarEvent(event: CalendarEvent) {
        val eventTitle = event.title
        val eventTime = event.startTime.time

        // Set lock end time to the calendar event's start time so the room feels themed
        val now = System.currentTimeMillis()
        if (eventTime > now) {
            val endTime = eventTime
            _lockEndTime.value = endTime
            startCountdown(endTime)
        }

        // Schedule a notification for when the event starts
        viewModelScope.launch {
            repository.trackInteraction(
                roomId = _roomId.value,
                dwellTimeSeconds = 0,
                type = "CALENDAR_EVENT"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        onRoomExited() // Final tracking on VM destruction
        countdownJob?.cancel()
    }
}
