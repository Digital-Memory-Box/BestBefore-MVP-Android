package com.ozang.bestbefore_mvp.ui.screens.room

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ozang.bestbefore_mvp.calendar.CalendarHelper
import com.ozang.bestbefore_mvp.calendar.CalendarEvent
import com.ozang.bestbefore_mvp.notifications.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RoomViewModel(application: Application) : AndroidViewModel(application) {
    
    // Stub: No repositories
    private val notificationHelper = NotificationHelper(application)
    private val calendarHelper = CalendarHelper(application)

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

    private val _frame1Image = MutableStateFlow<Uri?>(null)
    val frame1Image: StateFlow<Uri?> = _frame1Image.asStateFlow()

    private val _frame2Image = MutableStateFlow<Uri?>(null)
    val frame2Image: StateFlow<Uri?> = _frame2Image.asStateFlow()

    private val _selectedFrameNumber = MutableStateFlow<Int?>(null)

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private var countdownJob: Job? = null

    fun initialize(roomId: String, roomName: String) {
        _roomId.value = roomId
        _roomName.value = roomName

        // Stub: Load mocked frames if needed
        // Stub: Check if time capsule is active (mocked inactive)
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

    fun selectFrame(frameNumber: Int) {
        _selectedFrameNumber.value = frameNumber
    }

    fun updateSelectedFrame(imageUri: Uri) {
        val frameNumber = _selectedFrameNumber.value ?: return

        when (frameNumber) {
            1 -> _frame1Image.value = imageUri
            2 -> _frame2Image.value = imageUri
        }
        
        // Stub: Not saving to repo
        _selectedFrameNumber.value = null
    }

    fun startTimeCapsule(hours: Int, minutes: Int, seconds: Int) {
        val totalSeconds = hours * 3600L + minutes * 60L + seconds
        if (totalSeconds <= 0) return

        val endTime = System.currentTimeMillis() + (totalSeconds * 1000)
        _lockEndTime.value = endTime

        // Stub: Not saving to DB
        notificationHelper.scheduleTimeCapsuleNotification(
            _roomId.value,
            _roomName.value,
            endTime
        )

        startCountdown(endTime)
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
                    // Stub: deactivated
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
        // Stub
    }

    fun renameRoom(newName: String) {
         _roomName.value = newName
         // Stub: Not updating repo
    }

    private fun loadCalendarEvents() {
        if (!calendarHelper.checkPermission()) {
            return
        }

        viewModelScope.launch {
            try {
                val events = calendarHelper.getUpcomingEvents(30)
                _calendarEvents.value = events
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun createRoomFromCalendarEvent(event: CalendarEvent) {
        // Stub
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}