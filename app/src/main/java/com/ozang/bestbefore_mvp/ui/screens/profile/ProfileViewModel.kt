package com.ozang.bestbefore_mvp.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.ozang.bestbefore_mvp.data.models.TimeCapsuleRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(ProfileStep.NONE)
    val currentStep: StateFlow<ProfileStep> = _currentStep.asStateFlow()

    private val _createdRooms = MutableStateFlow<List<TimeCapsuleRoom>>(emptyList())
    val createdRooms: StateFlow<List<TimeCapsuleRoom>> = _createdRooms.asStateFlow()

    private val _selectedRoom = MutableStateFlow<TimeCapsuleRoom?>(null)
    val selectedRoom: StateFlow<TimeCapsuleRoom?> = _selectedRoom.asStateFlow()

    // Room creation state
    private val _roomName = MutableStateFlow("")
    val roomName: StateFlow<String> = _roomName.asStateFlow()

    private val _capsuleDays = MutableStateFlow(0)
    val capsuleDays: StateFlow<Int> = _capsuleDays.asStateFlow()

    private val _capsuleHours = MutableStateFlow(0)
    val capsuleHours: StateFlow<Int> = _capsuleHours.asStateFlow()

    private val _notifyDays = MutableStateFlow(0)
    val notifyDays: StateFlow<Int> = _notifyDays.asStateFlow()

    private val _notifyHours = MutableStateFlow(0)
    val notifyHours: StateFlow<Int> = _notifyHours.asStateFlow()

    private val _isPublic = MutableStateFlow(true)
    val isPublic: StateFlow<Boolean> = _isPublic.asStateFlow()

    private val _isCollaboration = MutableStateFlow(false)
    val isCollaboration: StateFlow<Boolean> = _isCollaboration.asStateFlow()

    fun startCreateRoom() {
        _roomName.value = ""
        _capsuleDays.value = 0
        _capsuleHours.value = 0
        _notifyDays.value = 0
        _notifyHours.value = 0
        _isPublic.value = true
        _isCollaboration.value = false
        _currentStep.value = ProfileStep.ROOM_NAME
    }

    fun updateRoomName(name: String) {
        _roomName.value = name
    }

    fun updateCapsuleTime(days: Int, hours: Int) {
        _capsuleDays.value = days
        _capsuleHours.value = hours
    }

    fun updateNotifyTime(days: Int, hours: Int) {
        _notifyDays.value = days
        _notifyHours.value = hours
    }

    fun updateRoomMode(isPublic: Boolean) {
        _isPublic.value = isPublic
    }

    fun updateCollaboration(enabled: Boolean) {
        _isCollaboration.value = enabled
    }

    fun goToStep(step: ProfileStep) {
        _currentStep.value = step
    }

    fun closeOverlay() {
        _currentStep.value = ProfileStep.NONE
        _selectedRoom.value = null
    }

    fun showTimeCapsuleList() {
        _currentStep.value = ProfileStep.TIME_CAPSULE_LIST
    }

    fun selectRoom(room: TimeCapsuleRoom) {
        _selectedRoom.value = room
        _currentStep.value = ProfileStep.ROOM_DETAIL
    }

    fun finalizeRoom() {
        val newRoom = TimeCapsuleRoom(
            roomName = _roomName.value,
            capsuleDays = _capsuleDays.value,
            capsuleHours = _capsuleHours.value,
            notificationDays = _notifyDays.value,
            notificationHours = _notifyHours.value,
            isPublic = _isPublic.value,
            isCollaboration = _isCollaboration.value
        )
        _createdRooms.value = _createdRooms.value + newRoom
        closeOverlay()
    }
}

enum class ProfileStep {
    NONE,
    ROOM_NAME,
    TIME_CAPSULE,
    NOTIFICATION,
    ROOM_MODE,
    FINALIZE_PUBLIC,
    FINALIZE_PRIVATE,
    COLLABORATION,
    TIME_CAPSULE_LIST,
    ROOM_DETAIL
}
