package com.dmb.bestbefore.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.models.TimeCapsuleRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(ProfileStep.NONE)
    val currentStep: StateFlow<ProfileStep> = _currentStep.asStateFlow()

    // Database Integration - REMOVED (API Only)
    private var roomRepository: com.dmb.bestbefore.data.repository.RoomRepository? = null
    
    // Switch createdRooms to loading from DB/API
    private val _createdRooms = MutableStateFlow<List<TimeCapsuleRoom>>(emptyList())
    val createdRooms: StateFlow<List<TimeCapsuleRoom>> = _createdRooms.asStateFlow()
    
    private var creationSource: RoomCreationSource = RoomCreationSource.HALLWAY



    // Profile State
    private val _profileImageUri = MutableStateFlow<android.net.Uri?>(null)
    val profileImageUri: StateFlow<android.net.Uri?> = _profileImageUri.asStateFlow()
    
    private val _userName = MutableStateFlow("User") 
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _selectedRoom = MutableStateFlow<TimeCapsuleRoom?>(null)
    val selectedRoom: StateFlow<TimeCapsuleRoom?> = _selectedRoom.asStateFlow()

    // Room creation state
    private val _roomName = MutableStateFlow("")
    val roomName: StateFlow<String> = _roomName.asStateFlow()

    // Target Time state (replaces capsuleDays/Hours and notifyDate)
    private val _targetTime = MutableStateFlow(System.currentTimeMillis() + 86400000) // Default +24h
    val targetTime: StateFlow<Long> = _targetTime.asStateFlow()

    private val _targetHour = MutableStateFlow(12)
    val targetHour: StateFlow<Int> = _targetHour.asStateFlow()

    private val _targetMinute = MutableStateFlow(0)
    val targetMinute: StateFlow<Int> = _targetMinute.asStateFlow()

    private val _addToCalendar = MutableStateFlow(false)
    val addToCalendar: StateFlow<Boolean> = _addToCalendar.asStateFlow()

    private val _isPublic = MutableStateFlow(true)
    val isPublic: StateFlow<Boolean> = _isPublic.asStateFlow()

    private val _isCollaboration = MutableStateFlow(false)
    val isCollaboration: StateFlow<Boolean> = _isCollaboration.asStateFlow()

    private val _isAllMediaVisible = MutableStateFlow(false)
    val isAllMediaVisible: StateFlow<Boolean> = _isAllMediaVisible.asStateFlow()

    private val _selectedMediaUris = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val selectedMediaUris: StateFlow<List<android.net.Uri>> = _selectedMediaUris.asStateFlow()
    
    // Room Media Map
    private val _roomMedia = MutableStateFlow<Map<String, List<android.net.Uri>>>(emptyMap())
    val roomMedia: StateFlow<Map<String, List<android.net.Uri>>> = _roomMedia.asStateFlow()
    
    // Gallery viewer state
    private val _isGalleryViewerOpen = MutableStateFlow(false)
    val isGalleryViewerOpen: StateFlow<Boolean> = _isGalleryViewerOpen.asStateFlow()
    
    private val _galleryViewerMedia = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val galleryViewerMedia: StateFlow<List<android.net.Uri>> = _galleryViewerMedia.asStateFlow()
    
    private val _galleryViewerIndex = MutableStateFlow(0)
    val galleryViewerIndex: StateFlow<Int> = _galleryViewerIndex.asStateFlow()
    
    // Room unlock dialog state
    private val _showUnlockDialog = MutableStateFlow(false)
    val showUnlockDialog: StateFlow<Boolean> = _showUnlockDialog.asStateFlow()
    
    private val _unlockDialogRoom = MutableStateFlow<TimeCapsuleRoom?>(null)
    val unlockDialogRoom: StateFlow<TimeCapsuleRoom?> = _unlockDialogRoom.asStateFlow()

    // Callbacks for permissions
    var onRequestNotificationPermission: (() -> Unit)? = null
    var onRequestCalendarPermission: (() -> Unit)? = null
    var onRequestCameraPermission: (() -> Unit)? = null
    var onRequestGalleryPermission: (() -> Unit)? = null
    var onRequestFilePermission: (() -> Unit)? = null

    // Helper context for DB init (Simple MVP approach)
    fun initDatabase(context: android.content.Context) {
        val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
        val token = sessionManager.getToken()
        
        if (token != null) {
            val prefs = context.getSharedPreferences("BestBeforePrefs", android.content.Context.MODE_PRIVATE)
            // Ideally RoomRepository should also take context or session manager, but for now we keep using token
            roomRepository = com.dmb.bestbefore.data.repository.RoomRepository(token)
            
            val savedName = sessionManager.getUserName()
            _userName.value = if (!savedName.isNullOrEmpty()) savedName else "User"
        }

        if (roomRepository != null) {
            viewModelScope.launch {
                val result = roomRepository!!.getRooms()
                result.onSuccess { apiRooms ->
                    val uiRooms = apiRooms.map { dto ->
                        TimeCapsuleRoom(
                            id = dto.id,
                            roomName = dto.name,
                            capsuleDays = 0,
                            capsuleHours = 0,
                            notificationDays = 0,
                            notificationHours = 0,
                            isPublic = true,
                            isCollaboration = false,
                            // In a real app, 'isSaved' and 'dateCreated' should come from API.
                            // For MVP, if we load it now, assume it's valid unless local persistence says otherwise.
                            // Since we don't have isSaved in API, we default to false (Active/Pending).
                        )
                    }
                    refreshRoomLists(uiRooms)
                }
            }
        }
        // ... profile image ...
    }

    // Sort rooms (Active only for MVP cleanup)
    private fun refreshRoomLists(allRooms: List<TimeCapsuleRoom>) {
        val now = System.currentTimeMillis()
        val active = mutableListOf<TimeCapsuleRoom>()

        allRooms.forEach { room ->
            // Simple active check or just show all for now to avoid hiding everything
            // User asked to remove "saved rooms feature", so we treat everything as active/created list for now
             active.add(room)
        }
        
        _createdRooms.value = active
    }
    
    
    // Calendar Events Integration
    private val _calendarEvents = MutableStateFlow<List<com.dmb.bestbefore.calendar.CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<com.dmb.bestbefore.calendar.CalendarEvent>> = _calendarEvents.asStateFlow()

    fun loadUpcomingEvents(context: android.content.Context) {
        // Request permission handled by caller via launcher
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
             viewModelScope.launch {
                 val helper = com.dmb.bestbefore.calendar.CalendarHelper(context)
                 val events = helper.getUpcomingEvents()
                 _calendarEvents.value = events
             }
        } else {
            // Trigger permission request
            onRequestReadCalendarPermission?.invoke()
        }
    } 

    
    // Callback for read permission
    var onRequestReadCalendarPermission: (() -> Unit)? = null

    // Image upload state
    private val _selectedImageUri = MutableStateFlow<android.net.Uri?>(null)
    val selectedImageUri: StateFlow<android.net.Uri?> = _selectedImageUri.asStateFlow()

    fun updateSelectedImage(uri: android.net.Uri) {
        _selectedImageUri.value = uri
    }
    
    // Camera capture state
    private val _capturedImageUri = MutableStateFlow<android.net.Uri?>(null)
    val capturedImageUri: StateFlow<android.net.Uri?> = _capturedImageUri.asStateFlow()
    
    fun setCapturedImage(uri: android.net.Uri) {
        _capturedImageUri.value = uri
    }
    
    fun clearCapturedImage() {
        _capturedImageUri.value = null
    }
    
    fun acceptCapturedImage() {
        _capturedImageUri.value?.let { uri ->
            _selectedMediaUris.value = _selectedMediaUris.value + uri
            _capturedImageUri.value = null
        }
    }

    fun applyCalendarEvent(event: com.dmb.bestbefore.calendar.CalendarEvent) {
        _roomName.value = event.title
        _targetTime.value = event.startTime.time
        
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = event.startTime.time
        _targetHour.value = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        _targetMinute.value = calendar.get(java.util.Calendar.MINUTE)
    }

    fun finalizeRoom(context: android.content.Context? = null) {
        // Request notification permission before creating room
        onRequestNotificationPermission?.invoke()
        
        // Calculate duration
        val now = System.currentTimeMillis()
        val durationMillis = (_targetTime.value - now).coerceAtLeast(0)
        
        val days = (durationMillis / (24 * 60 * 60 * 1000)).toInt()
        val hours = ((durationMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
        val minutes = ((durationMillis % (60 * 60 * 1000)) / (60 * 1000)).toInt()
        
        val newRoom = TimeCapsuleRoom(
            id = java.util.UUID.randomUUID().toString(), // Assign a local ID for immediate use
            roomName = _roomName.value,
            capsuleDays = days,
            capsuleHours = hours,
            capsuleMinutes = minutes,
            notificationDays = days, // Using same duration for compatibility
            notificationHours = hours,
            notificationMinutes = minutes,
            isPublic = _isPublic.value,
            isCollaboration = _isCollaboration.value
        )
        // Create Room via API
        viewModelScope.launch {
             // 2. API Save (Fire and forget for MVP UI speed, but practically should wait or queue)
             roomRepository?.createRoom(newRoom.roomName)
             
             // Add to local list for immediate UI feedback (Optimistic Update)
             val updatedList = _createdRooms.value + newRoom
             refreshRoomLists(updatedList)
        }
        

        
        // Keep user in flow by navigating to the new room detail
        selectRoom(newRoom)

        // Schedule notification (Fixed: Added back)
        context?.let { ctx ->
            val unlockTimeMillis = _targetTime.value
            com.dmb.bestbefore.notifications.NotificationScheduler.scheduleRoomUnlockNotification(
                ctx,
                newRoom.roomName.hashCode().toString(),
                newRoom.roomName,
                unlockTimeMillis
            )
            
            // Calendar event
            if (_addToCalendar.value) {
                try {
                     val calendarHelper = com.dmb.bestbefore.calendar.CalendarHelper(ctx)
                     calendarHelper.createCalendarEvent(
                         roomName = newRoom.roomName,
                         notificationTime = unlockTimeMillis,
                         description = "Time capsule unlock notification for ${newRoom.roomName}"
                     )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun sortRoomsByDateCreated() {
        _createdRooms.value = _createdRooms.value.sortedByDescending { it.dateCreated }
    }
    
    // Media Persistence (Room ID -> List of Uris)


    fun toggleAllMedia(visible: Boolean) {
        _isAllMediaVisible.value = visible
    }

    fun updateSelectedMedia(uris: List<android.net.Uri>) {
        _selectedMediaUris.value = uris
    }
    
    fun getMediaForRoom(roomId: String): List<android.net.Uri> {
        return _roomMedia.value[roomId] ?: emptyList()
    }

    fun uploadMedia(context: android.content.Context) {
        // Mock upload for MVP - In real app, this would send files to API
        viewModelScope.launch {
            val currentSelection = _selectedMediaUris.value
            val currentRoomId = _selectedRoom.value?.id
            
            if (currentSelection.isNotEmpty() && currentRoomId != null) {
                // Simulate network delay
                kotlinx.coroutines.delay(1000)
                
                // Persist to local map
                val currentMap = _roomMedia.value.toMutableMap()
                val existingList = currentMap[currentRoomId] ?: emptyList()
                currentMap[currentRoomId] = existingList + currentSelection
                _roomMedia.value = currentMap
                
                android.widget.Toast.makeText(context, "Photos uploaded to Time Capsule!", android.widget.Toast.LENGTH_SHORT).show()
                _selectedMediaUris.value = emptyList()
            }
        }
    }

    // Navigation Helpers
    fun goToStep(step: ProfileStep) {
        _currentStep.value = step
    }

    fun goBack(): Boolean {
        return when (_currentStep.value) {
            ProfileStep.NONE -> false
            ProfileStep.PROFILE_MENU -> {
                closeOverlay()
                true
            }
            ProfileStep.ROOM_NAME -> {
                if (creationSource == RoomCreationSource.PROFILE_MENU) {
                    goToStep(ProfileStep.PROFILE_MENU)
                } else {
                    closeOverlay()
                }
                true
            }
            ProfileStep.ROOM_TIME -> {
                goToStep(ProfileStep.ROOM_NAME)
                true
            }
            ProfileStep.ROOM_MODE -> {
                goToStep(ProfileStep.ROOM_TIME)
                true
            }
            ProfileStep.FINALIZE_PUBLIC, ProfileStep.FINALIZE_PRIVATE -> {
                goToStep(ProfileStep.ROOM_MODE)
                true
            }
            ProfileStep.COLLABORATION -> {
                goToStep(ProfileStep.FINALIZE_PUBLIC)
                true
            }
            ProfileStep.TIME_CAPSULE_LIST -> {
                closeOverlay()
                true
            }
            ProfileStep.ROOM_DETAIL -> {
                 // return to Profile Menu default now? or Hallway logic?
                 // Current default was Profile Menu, keeping it for now
                 goToStep(ProfileStep.PROFILE_MENU)
                 true
            }
            else -> {
                closeOverlay()
                true
            }
        }
    }

    fun closeOverlay() {
        _currentStep.value = ProfileStep.NONE
        _selectedRoom.value = null
    }

    fun startCreateRoom(source: RoomCreationSource = RoomCreationSource.HALLWAY) {
        creationSource = source
        _currentStep.value = ProfileStep.ROOM_NAME
        // Reset state
        _roomName.value = ""
        _targetTime.value = System.currentTimeMillis() + 86400000
    }

    fun openProfileMenu() {
        _currentStep.value = ProfileStep.PROFILE_MENU
    }

    fun showTimeCapsuleList() {
        _currentStep.value = ProfileStep.TIME_CAPSULE_LIST
    }

    fun selectRoom(room: TimeCapsuleRoom) {
        _selectedRoom.value = room
        _currentStep.value = ProfileStep.ROOM_DETAIL
    }
    
    // Deep Link Handler
    fun handleDeepLink(roomId: String) {
        val room = _createdRooms.value.find { it.id == roomId }
        if (room != null) {
            // Show the unlock dialog for this room
            showUnlockDialog(room)
        } else {
             // If room not found in current loaded list (e.g. fresh start), 
             // we might need to fetch it or wait for init. 
             // For MVP, we assume initDatabase loads it.
             // We can also trigger a specific "Load Room" here if needed.
        }
    }

    // State Updates
    fun updateRoomName(name: String) {
        _roomName.value = name
    }

    fun updateTargetTime(hour: Int, minute: Int) {
        _targetHour.value = hour
        _targetMinute.value = minute
        
        // Update timestamp
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = _targetTime.value
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        _targetTime.value = calendar.timeInMillis
    }

    fun updateTargetDate(dateMillis: Long) {
        _targetTime.value = dateMillis
        // Preserve time logic if needed, but for MVP dateMillis is sufficient base
    }

    fun updateAddToCalendar(enable: Boolean) {
        _addToCalendar.value = enable
    }

    fun updateRoomMode(public: Boolean) {
        _isPublic.value = public
    }

    fun updateCollaboration(enable: Boolean) {
        _isCollaboration.value = enable
    }
    
    fun updateProfileImage(uri: android.net.Uri, context: android.content.Context) {
        _profileImageUri.value = uri
        // Persist permissions?
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    // Permission request helpers
    fun requestCameraPermission() {
        onRequestCameraPermission?.invoke()
    }
    
    fun requestGalleryPermission() {
        onRequestGalleryPermission?.invoke()
    }
    
    fun requestFilePermission() {
        onRequestFilePermission?.invoke()
    }
    
    // Gallery viewer functions
    fun openGalleryViewer(mediaList: List<android.net.Uri>, startIndex: Int = 0) {
        _galleryViewerMedia.value = mediaList
        _galleryViewerIndex.value = startIndex
        _isGalleryViewerOpen.value = true
    }
    
    fun closeGalleryViewer() {
        _isGalleryViewerOpen.value = false
    }
    
    fun updateGalleryIndex(index: Int) {
        _galleryViewerIndex.value = index
    }
    
    // Room unlock dialog functions
    fun showUnlockDialog(room: TimeCapsuleRoom) {
        _unlockDialogRoom.value = room
        _showUnlockDialog.value = true
    }
    
    fun dismissUnlockDialog() {
        _showUnlockDialog.value = false
        _unlockDialogRoom.value = null
    }
    
    // Schedule notification for room unlock

    
    // Keep room - mark as saved/permanent
    fun keepRoom(context: android.content.Context, room: TimeCapsuleRoom) {
        viewModelScope.launch {
            try {
                // Room is already in created rooms, just dismiss dialog
                dismissUnlockDialog()
                android.widget.Toast.makeText(
                    context,
                    "Room \"${room.roomName}\" kept in your collection",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error keeping room", e)
            }
        }
    }
    
    // Delete room from backend and local storage
    fun deleteRoom(context: android.content.Context, room: TimeCapsuleRoom) {
        viewModelScope.launch {
            try {
                // TODO: Call backend API to delete room
                // For now, just remove locally
                _createdRooms.value = _createdRooms.value.filter { it.id != room.id }
                _roomMedia.value = _roomMedia.value.filterKeys { it != room.id }
                
                dismissUnlockDialog()
                closeOverlay()
                
                android.widget.Toast.makeText(
                    context,
                    "Room \"${room.roomName}\" deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error deleting room", e)
                android.widget.Toast.makeText(
                    context,
                    "Failed to delete room",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun logout(context: android.content.Context) {
        // Clear session data
        val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
        sessionManager.clearSession()
        
        // Clear auth repository prefs as well if they are separate (they seem to be inconsistent in the codebase)
        // Ideally we should unify, but for safety lets clear the one used in initDatabase too
        val prefs = context.getSharedPreferences("BestBeforePrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Reset local state
        _currentStep.value = ProfileStep.NONE
        _userName.value = "User"
        _createdRooms.value = emptyList()
        _profileImageUri.value = null
    }

}


enum class ProfileStep {
    NONE,
    ROOM_NAME,
    ROOM_TIME, // Replaces TIME_CAPSULE and NOTIFICATION
    ROOM_MODE,
    FINALIZE_PUBLIC,
    FINALIZE_PRIVATE,
    COLLABORATION,
    TIME_CAPSULE_LIST,
    ROOM_DETAIL,
    PROFILE_MENU
}

enum class RoomCreationSource {
    HALLWAY,
    PROFILE_MENU
}
