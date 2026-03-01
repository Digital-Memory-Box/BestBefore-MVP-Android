package com.dmb.bestbefore.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.models.TimeCapsuleRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.widget.Toast
import android.util.Log

import com.dmb.bestbefore.data.models.CalendarEvent
import com.dmb.bestbefore.ui.theme.AppTheme
import com.dmb.bestbefore.ui.theme.AppThemes
import com.dmb.bestbefore.data.local.PreferencesManager
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider

class ProfileViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(ProfileStep.NONE)
    val currentStep: StateFlow<ProfileStep> = _currentStep.asStateFlow()

    private fun parseCreatedAt(dateString: String?): Long {
        if (dateString == null) return System.currentTimeMillis()
        return try {
             // Assuming ISO 8601 like "2023-10-27T10:00:00.000Z"
             val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
             sdf.timeZone = TimeZone.getTimeZone("UTC")
             sdf.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
             System.currentTimeMillis()
        }
    }

    // Database Integration - REMOVED (API Only)
    private var roomRepository: com.dmb.bestbefore.data.repository.RoomRepository? = null
    
    // Switch createdRooms to loading from DB/API
    private val _createdRooms = MutableStateFlow<List<TimeCapsuleRoom>>(emptyList())
    val createdRooms: StateFlow<List<TimeCapsuleRoom>> = _createdRooms.asStateFlow()
    
    // Stats State
    private val _totalRooms = MutableStateFlow(0)
    val totalRooms: StateFlow<Int> = _totalRooms.asStateFlow()
    
    private val _totalMemories = MutableStateFlow(0)
    val totalMemories: StateFlow<Int> = _totalMemories.asStateFlow()
    
    private var creationSource: RoomCreationSource = RoomCreationSource.HALLWAY



    // Profile State
    data class RecentActivity(
        val type: ActivityType,
        val title: String, 
        val date: Long,
        val subtitle: String? = null
    )
    
    enum class ActivityType {
         CREATED_ROOM, ADDED_PHOTOS
    }

    private val _recentActivities = MutableStateFlow<List<RecentActivity>>(emptyList())
    val recentActivities: StateFlow<List<RecentActivity>> = _recentActivities.asStateFlow()

    private val _profileImageUri = MutableStateFlow<Uri?>(null)

    
    private val _userName = MutableStateFlow("User") 
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _showOnlySaved = MutableStateFlow(false)
    val showOnlySaved: StateFlow<Boolean> = _showOnlySaved.asStateFlow()

    private val _selectedRoom = MutableStateFlow<TimeCapsuleRoom?>(null)
    val selectedRoom: StateFlow<TimeCapsuleRoom?> = _selectedRoom.asStateFlow()

    // Theme & Customization State
    private var preferencesManager: PreferencesManager? = null
    
    private val _selectedTheme = MutableStateFlow(AppThemes.Default)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()
    
    private val _accentColor = MutableStateFlow(Color(0xFF007AFF))
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()
    
    // Credential Update State
    private val _isUpdatingCredential = MutableStateFlow(false)
    val isUpdatingCredential: StateFlow<Boolean> = _isUpdatingCredential.asStateFlow()
    
    private val _credentialUpdateError = MutableStateFlow<String?>(null)
    val credentialUpdateError: StateFlow<String?> = _credentialUpdateError.asStateFlow()
    
    private val _credentialUpdateSuccess = MutableStateFlow<String?>(null)
    val credentialUpdateSuccess: StateFlow<String?> = _credentialUpdateSuccess.asStateFlow()

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

    // ---- New wizard state ----
    private val _isTimeCapsuleEnabled = MutableStateFlow(true)
    val isTimeCapsuleEnabled: StateFlow<Boolean> = _isTimeCapsuleEnabled.asStateFlow()

    // DURATION or SPECIFIC_DATE
    private val _unlockMethod = MutableStateFlow(UnlockMethod.DURATION)
    val unlockMethod: StateFlow<UnlockMethod> = _unlockMethod.asStateFlow()

    private val _capsuleDays = MutableStateFlow(0)
    val capsuleDays: StateFlow<Int> = _capsuleDays.asStateFlow()

    private val _capsuleHours = MutableStateFlow(0)
    val capsuleHours: StateFlow<Int> = _capsuleHours.asStateFlow()

    private val _capsuleMins = MutableStateFlow(0)
    val capsuleMins: StateFlow<Int> = _capsuleMins.asStateFlow()

    private val _selectedPreset = MutableStateFlow<String?>("21 Days")
    val selectedPreset: StateFlow<String?> = _selectedPreset.asStateFlow()

    // Atmosphere room theme (string name, separate from AppTheme)
    private val _roomAtmosphereTheme = MutableStateFlow("Default")
    val roomAtmosphereTheme: StateFlow<String> = _roomAtmosphereTheme.asStateFlow()

    // Atmosphere: background music
    private val _selectedMusic = MutableStateFlow("None")
    val selectedMusic: StateFlow<String> = _selectedMusic.asStateFlow()

    // Rolling expiration: Never | 1 Day (24h) | 7 Days | 30 Days
    private val _rollingExpiration = MutableStateFlow("Never")
    val rollingExpiration: StateFlow<String> = _rollingExpiration.asStateFlow()

    private val _scheduledClosureEnabled = MutableStateFlow(false)
    val scheduledClosureEnabled: StateFlow<Boolean> = _scheduledClosureEnabled.asStateFlow()

    private val _inviteEmails = MutableStateFlow<List<String>>(emptyList())
    val inviteEmails: StateFlow<List<String>> = _inviteEmails.asStateFlow()

    private val _isPublic = MutableStateFlow(true)
    val isPublic: StateFlow<Boolean> = _isPublic.asStateFlow()

    private val _isCollaboration = MutableStateFlow(false)
    val isCollaboration: StateFlow<Boolean> = _isCollaboration.asStateFlow()

    private val _isAllMediaVisible = MutableStateFlow(false)
    val isAllMediaVisible: StateFlow<Boolean> = _isAllMediaVisible.asStateFlow()

    private val _selectedMediaUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedMediaUris: StateFlow<List<Uri>> = _selectedMediaUris.asStateFlow()
    
    // Room Media Map
    private val _roomMedia = MutableStateFlow<Map<String, List<Uri>>>(emptyMap())
    val roomMedia: StateFlow<Map<String, List<Uri>>> = _roomMedia.asStateFlow()
    
    // Gallery viewer state
    private val _isGalleryViewerOpen = MutableStateFlow(false)
    val isGalleryViewerOpen: StateFlow<Boolean> = _isGalleryViewerOpen.asStateFlow()
    
    private val _galleryViewerMedia = MutableStateFlow<List<Uri>>(emptyList())
    val galleryViewerMedia: StateFlow<List<Uri>> = _galleryViewerMedia.asStateFlow()
    
    private val _galleryViewerIndex = MutableStateFlow(0)
    val galleryViewerIndex: StateFlow<Int> = _galleryViewerIndex.asStateFlow()
    
    // Room unlock dialog state
    private val _showUnlockDialog = MutableStateFlow(false)
    val showUnlockDialog: StateFlow<Boolean> = _showUnlockDialog.asStateFlow()
    
    private val _unlockDialogRoom = MutableStateFlow<TimeCapsuleRoom?>(null)
    val unlockDialogRoom: StateFlow<TimeCapsuleRoom?> = _unlockDialogRoom.asStateFlow()

    // Unlocked Photos Viewer State
    private val _unlockedPhotosRoom = MutableStateFlow<TimeCapsuleRoom?>(null)
    val unlockedPhotosRoom: StateFlow<TimeCapsuleRoom?> = _unlockedPhotosRoom.asStateFlow()

    // Callbacks for permissions
    var onRequestNotificationPermission: (() -> Unit)? = null
    var onRequestCalendarPermission: (() -> Unit)? = null
    var onRequestCameraPermission: (() -> Unit)? = null
    var onRequestGalleryPermission: (() -> Unit)? = null
    var onRequestFilePermission: (() -> Unit)? = null

    // Helper context for DB init (Simple MVP approach)
    fun initDatabase(context: Context) {
        val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
        val token = sessionManager.getToken()
        
        if (token != null) {
            context.getSharedPreferences("BestBeforePrefs", Context.MODE_PRIVATE)
            // Ideally RoomRepository should also take context or session manager, but for now we keep using token
            roomRepository = com.dmb.bestbefore.data.repository.RoomRepository(token)
            
            val savedName = sessionManager.getUserName()
            _userName.value = if (!savedName.isNullOrEmpty()) savedName else "User"
        }

        if (roomRepository != null) {
            viewModelScope.launch {
                val result = roomRepository!!.getRooms()
                val savedResult = roomRepository!!.getSavedRooms() // Fetch saved rooms too

                val allRooms = mutableListOf<TimeCapsuleRoom>()

                // Process Created Rooms
                result.onSuccess { apiRooms ->
                     allRooms.addAll(mapDtosToRooms(apiRooms, isSaved = false))
                }

                // Process Saved Rooms
                savedResult.onSuccess { savedDtos ->
                     val savedRooms = mapDtosToRooms(savedDtos, isSaved = true)
                     allRooms.addAll(savedRooms)
                }
                
                // Re-map media for all rooms
                val mediaMap = allRooms.associate { room -> 
                    room.id to (room.photos.map { Uri.parse(it) } )
                }
                _roomMedia.value = mediaMap
                
                refreshRoomLists(allRooms)
            }
        }
    }

    private fun mapDtosToRooms(dtos: List<com.dmb.bestbefore.data.api.models.RoomDto>, isSaved: Boolean): List<TimeCapsuleRoom> {
        return dtos.map { dto ->
            TimeCapsuleRoom(
                id = dto.id,
                roomName = dto.name,
                capsuleDays = 0,
                capsuleHours = 0,
                notificationDays = 0,
                notificationHours = 0,
                isPublic = true,
                isCollaboration = dto.isCollaboration,
                photos = dto.photos ?: emptyList(),
                unlockTime = (parseCreatedAt(dto.createdAt)) + 
                             ((dto.capsuleDays * 24 * 3600 * 1000L) + 
                              (dto.capsuleHours * 3600 * 1000L) + 
                              (dto.capsuleMinutes * 60 * 1000L)),
                dateCreated = parseCreatedAt(dto.createdAt),
                isSaved = isSaved
            )
        }
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
        
        // Generate Recent Activity from Rooms
        val activities = mutableListOf<RecentActivity>()
        
        // 1. "Joined BestBefore" (Static for MVP, or based on user creation date if available)
        // For now, we don't have user creation date in this VM, so we skip or mock it if needed.
        // Let's add a static one for "App Installed" or similar if requested, but user asked for "Not dummy".
        // The user said: "if user created a room write in under there"
        
        active.forEach { room ->
            activities.add(
                RecentActivity(
                    type = ActivityType.CREATED_ROOM,
                    title = "Created room \"${room.roomName}\"",
                    date = room.dateCreated,
                    subtitle = null
                )
            )
            
            // If we had photo upload timestamps, we would add them here. 
            // For MVP, we only track room creation date. 
            // We can check if room has photos and add a generic "Added photos" activity if needed, 
            // but without specific timestamps it might look odd if it's old.
            // We will handle "Added photos" dynamically in uploadMedia for the current session.
        }
        
        _recentActivities.value = activities.sortedByDescending { it.date }
        
        // Update Stats
        _totalRooms.value = active.size
        _totalMemories.value = active.sumOf { it.photos.size }
    }
    
    
    // Calendar Events Integration
    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    fun loadUpcomingEvents(context: Context) {
        if (roomRepository == null) return
        
        viewModelScope.launch {
            val result = roomRepository!!.getUpcomingEvents()
            
            if (result.isSuccess) {
                val dtos = result.getOrNull() ?: emptyList()
                val events = dtos.mapNotNull { dto ->
                     try {
                         // Parse ISO 8601 strings from backend
                         // Backend likely sends specific format (e.g. 2023-10-25T10:00:00.000Z)
                         // SimpleDateFormat with 'X' or 'Z' might work, or Instants if API level allows.
                         // For MVP, handling simplified ISO.
                         val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                         sdf.timeZone = TimeZone.getTimeZone("UTC")
                         // Fallback attempt if pattern mismatches could be added
                         
                         val start = sdf.parse(dto.start)
                         val end = sdf.parse(dto.end)
                         
                         if (start != null && end != null) {
                             CalendarEvent(
                                 id = dto.id,
                                 title = dto.title,
                                 startTime = start,
                                 endTime = end,
                                 location = dto.location,
                                 description = dto.description
                             )
                         } else null
                     } catch (e: Exception) {
                         null
                     }
                }
                _calendarEvents.value = events
            } else {
                // If failed (likely 401), we might want to prompt connection
                // For MVP, just clear list
                _calendarEvents.value = emptyList()
                Log.e("ProfileVM", "Failed to calendar events", result.exceptionOrNull())
            }
        }
    }
    
    fun connectGoogleCalendar(context: Context) {
         if (roomRepository == null) {
             Toast.makeText(context, "Sychronizing... Please wait.", Toast.LENGTH_SHORT).show()
             return
         }
         viewModelScope.launch {
             val result = roomRepository!!.getCalendarAuthUrl()
             if (result.isSuccess) {
                 val url = result.getOrNull()
                 if (!url.isNullOrEmpty()) {
                     val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                     context.startActivity(intent)
                 }
             }
         }
    } 

    
    // Callback for read permission
    var onRequestReadCalendarPermission: (() -> Unit)? = null

    // Image upload state
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    fun updateSelectedImage(uri: Uri) {
        _selectedImageUri.value = uri
    }
    
    // Camera capture state
    private val _capturedImageUri = MutableStateFlow<Uri?>(null)
    val capturedImageUri: StateFlow<Uri?> = _capturedImageUri.asStateFlow()
    
    fun setCapturedImage(uri: Uri) {
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

    fun applyCalendarEvent(event: CalendarEvent) {
        _roomName.value = event.title
        _targetTime.value = event.startTime.time
        
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = event.startTime.time
        _targetHour.value = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        _targetMinute.value = calendar.get(java.util.Calendar.MINUTE)
    }

    fun finalizeRoom(context: Context? = null) {
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
            isCollaboration = _isCollaboration.value,
            unlockTime = _targetTime.value // Explicitly set unlock time
        )
        // Create Room via API
        viewModelScope.launch {
             // 2. API Save - Wait for response to get real ID
             val result = roomRepository?.createRoom(
                 newRoom.roomName,
                 newRoom.capsuleDays,
                 newRoom.capsuleHours,
                 newRoom.capsuleMinutes,
                 newRoom.isPublic,
                 newRoom.isCollaboration
             )
             
             val finalRoom = if (result != null && result.isSuccess) {
                 val realId = result.getOrNull()
                 if (realId != null) {
                     newRoom.copy(id = realId)
                 } else {
                     newRoom
                 }
             } else {
                 // API failed or offline - keep local UUID (but upload will fail until synced)
                 // For MVP, we proceed with local UUID but warn
                 newRoom
             }
             
             // Add to local list
             val updatedList = _createdRooms.value + finalRoom
             refreshRoomLists(updatedList)
             
             // Keep user in flow by navigating to the new room detail
             selectRoom(finalRoom)
        }

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
            // Calendar event creation in User's calendar is now handled via Backend-App integration if desired,
            // or we simply trust the user to have it.
            // Since we deleted local CalendarHelper and user said "move to backend",
            // we skip local creation here. Backend could potentially create it if we added that endpoint.
            // For now, removing local calendar write logic.
        }
    }
    
    fun sortRoomsByDateCreated() {
        _createdRooms.value = _createdRooms.value.sortedByDescending { it.dateCreated }
    }
    
    // Media Persistence (Room ID -> List of Uris)


    fun toggleAllMedia(visible: Boolean) {
        _isAllMediaVisible.value = visible
    }

    fun updateSelectedMedia(uris: List<Uri>) {
        _selectedMediaUris.value = uris
    }
    


    fun uploadMedia(context: Context) {
        viewModelScope.launch {
            val currentSelection = _selectedMediaUris.value
            val currentRoomId = _selectedRoom.value?.id
            
            if (currentSelection.isNotEmpty() && currentRoomId != null) {
                val uploadedUrls = mutableListOf<String>()
                
                currentSelection.forEach { uri ->
                    try {
                        val file = getFileFromUri(context, uri)
                        if (file != null) {
                            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                            
                            val result = roomRepository?.uploadPhoto(currentRoomId, body)
                            result?.onSuccess { url -> 
                                uploadedUrls.add(url) 
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                if (uploadedUrls.isNotEmpty()) {
                    // Update local state
                    _createdRooms.value = _createdRooms.value.map { room ->
                        if (room.id == currentRoomId) {
                            room.copy(photos = room.photos + uploadedUrls)
                        } else room
                    }
                    
                    // Also update _roomMedia for the detail screen
                    val currentMedia = _roomMedia.value[currentRoomId] ?: emptyList()
                    val newUris = uploadedUrls.map { Uri.parse(it) }
                    _roomMedia.value = _roomMedia.value + (currentRoomId to (currentMedia + newUris))
                    
                    Toast.makeText(context, "Uploaded ${uploadedUrls.size} photos!", Toast.LENGTH_SHORT).show()
                    _selectedMediaUris.value = emptyList()
                    
                    // Add "Added Photos" activity
                    val newActivity = RecentActivity(
                        type = ActivityType.ADDED_PHOTOS,
                        title = "Added ${uploadedUrls.size} photos to \"${_selectedRoom.value?.roomName}\"",
                        date = System.currentTimeMillis()
                    )
                    _recentActivities.value = listOf(newActivity) + _recentActivities.value
                    
                    // Update Memory Count immediately
                    // Update roomMedia first to reflect locally
                     val updatedMap = _roomMedia.value.toMutableMap()
                     val roomID = _selectedRoom.value?.id ?: ""
                     if (roomID.isNotEmpty()) {
                         val currentList = updatedMap[roomID] ?: emptyList()
                         updatedMap[roomID] = currentList + uploadedUrls.map { Uri.parse(it) }
                         _roomMedia.value = updatedMap
                         
                         // Recalculate total memories
                         // Note: We might be desynced with _createdRooms if we don't update that too, 
                         // but for the stats counter, using roomMedia is one way, or just incrementing.
                         // Let's just increment for now to be safe and simple.
                         _totalMemories.value += uploadedUrls.size
                     }
                } else {
                     Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
            ProfileStep.ROOM_TIME_CAPSULE -> {
                goToStep(ProfileStep.ROOM_NAME)
                true
            }
            ProfileStep.ROOM_ATMOSPHERE -> {
                goToStep(ProfileStep.ROOM_TIME_CAPSULE)
                true
            }
            ProfileStep.ROOM_MEMORY_RULES -> {
                goToStep(ProfileStep.ROOM_ATMOSPHERE)
                true
            }
            ProfileStep.ROOM_INVITE -> {
                goToStep(ProfileStep.ROOM_MEMORY_RULES)
                true
            }
            ProfileStep.TIME_CAPSULE_LIST -> {
                closeOverlay()
                true
            }
            ProfileStep.ROOM_DETAIL -> {
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
        _isPublic.value = true
        _isTimeCapsuleEnabled.value = true
        _unlockMethod.value = UnlockMethod.DURATION
        _capsuleDays.value = 21
        _capsuleHours.value = 0
        _capsuleMins.value = 0
        _selectedPreset.value = "21 Days"
        _roomAtmosphereTheme.value = "Default"
        _selectedMusic.value = "None"
        _rollingExpiration.value = "Never"
        _scheduledClosureEnabled.value = false
        _inviteEmails.value = emptyList()
        _targetTime.value = System.currentTimeMillis() + 86400000
    }

    fun openProfileMenu() {
        _currentStep.value = ProfileStep.PROFILE_MENU
    }

    fun showTimeCapsuleList() {
        _showOnlySaved.value = false
        _currentStep.value = ProfileStep.TIME_CAPSULE_LIST
    }

    fun showSavedRooms() {
        _showOnlySaved.value = true
        _currentStep.value = ProfileStep.TIME_CAPSULE_LIST
    }

    fun selectRoom(room: TimeCapsuleRoom) {
        _selectedRoom.value = room
        _currentStep.value = ProfileStep.ROOM_DETAIL
        
        // check for unlock
        checkRoomUnlockStatus(room)
    }

    private fun checkRoomUnlockStatus(room: TimeCapsuleRoom) {
         if (!room.isSaved && System.currentTimeMillis() >= room.unlockTime && room.unlockTime > 0) {
             // Room is expired/unlocked but not saved/kept yet -> Show Dialog
             showUnlockDialog(room)
         }
    }
    
    // Deep Link Handler
    // Deep Link Handler
    fun handleDeepLink(roomId: String) {
        val room = _createdRooms.value.find { it.id == roomId }
        if (room != null) {
            // Show photos first (Unlocked Room Flow Step 1)
            _unlockedPhotosRoom.value = room
        } else {
             // If room not found in current loaded list (e.g. fresh start), 
             // we might need to fetch it or wait for init. 
             // For MVP, we assume initDatabase loads it.
             // We can also trigger a specific "Load Room" here if needed.
        }
    }
    
    fun dismissUnlockedPhotos(room: TimeCapsuleRoom? = null) {
        val targetRoom = room ?: _unlockedPhotosRoom.value
        _unlockedPhotosRoom.value = null
        // Show Save/Delete Dialog (Unlocked Room Flow Step 2)
        targetRoom?.let { showUnlockDialog(it) }
    }

    // State Updates
    fun updateRoomName(name: String) { _roomName.value = name }

    fun updateRoomMode(public: Boolean) { _isPublic.value = public }

    fun updateTargetTime(hour: Int, minute: Int) {
        _targetHour.value = hour
        _targetMinute.value = minute
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = _targetTime.value
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        _targetTime.value = calendar.timeInMillis
    }

    fun updateTargetDate(dateMillis: Long) { _targetTime.value = dateMillis }

    // New wizard state updaters
    fun updateTimeCapsuleEnabled(enabled: Boolean) { _isTimeCapsuleEnabled.value = enabled }
    fun updateUnlockMethod(method: UnlockMethod) { _unlockMethod.value = method }
    fun updateCapsuleDays(d: Int) { _capsuleDays.value = d.coerceAtLeast(0) }
    fun updateCapsuleHours(h: Int) { _capsuleHours.value = h.coerceAtLeast(0) }
    fun updateCapsuleMins(m: Int) { _capsuleMins.value = m.coerceAtLeast(0) }
    fun selectPreset(preset: String) {
        _selectedPreset.value = preset
        when (preset) {
            "1 Week"  -> { _capsuleDays.value = 7;  _capsuleHours.value = 0; _capsuleMins.value = 0 }
            "21 Days" -> { _capsuleDays.value = 21; _capsuleHours.value = 0; _capsuleMins.value = 0 }
            "1 Month" -> { _capsuleDays.value = 30; _capsuleHours.value = 0; _capsuleMins.value = 0 }
        }
    }
    fun updateSelectedTheme(theme: String) { _roomAtmosphereTheme.value = theme }
    fun updateSelectedMusic(music: String) { _selectedMusic.value = music }
    fun updateRollingExpiration(option: String) { _rollingExpiration.value = option }
    fun updateScheduledClosure(enabled: Boolean) { _scheduledClosureEnabled.value = enabled }
    fun addInviteEmail(email: String) {
        if (email.isNotBlank() && !_inviteEmails.value.contains(email)) {
            _inviteEmails.value = _inviteEmails.value + email
        }
    }
    fun removeInviteEmail(email: String) {
        _inviteEmails.value = _inviteEmails.value.filter { it != email }
    }
    
    fun selectRoomForEditing(room: TimeCapsuleRoom) {
        _selectedRoom.value = room
        _roomName.value = room.roomName
        _isPublic.value = room.isPublic
        _currentStep.value = ProfileStep.EDIT_ROOM
    }

    fun updateCollaboration(enable: Boolean) {
        _isCollaboration.value = enable
    }
    
    fun updateProfileImage(uri: Uri, context: Context) {
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
    
    // Helper to get media for a room
    fun getRoomMediaUris(roomId: String): List<Uri> {
        return _roomMedia.value[roomId] ?: emptyList()
    }
    
    // Gallery viewer functions
    fun openGalleryViewer(mediaList: List<Uri>, startIndex: Int = 0) {
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
    fun keepRoom(context: Context, room: TimeCapsuleRoom) {
        viewModelScope.launch {
            try {
                // Call backend to save
                val result = roomRepository?.keepRoom(room.id)
                
                if (result != null && result.isSuccess) {
                    dismissUnlockDialog()
                    Toast.makeText(
                        context,
                        "Room \"${room.roomName}\" saved to your collection",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Refresh saved status locally if we want to reflect it immediately?
                    // Or just reload rooms.
                    // For MVP simplicity, we might just re-fetch or assume success.
                } else {
                     Toast.makeText(context, "Failed to save room", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error keeping room", e)
                Toast.makeText(context, "Error saving room", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Delete room from backend and local storage
    fun deleteRoom(context: Context, room: TimeCapsuleRoom) {
        viewModelScope.launch {
            try {
                // TODO: Call backend API to delete room
                // For now, just remove locally
                _createdRooms.value = _createdRooms.value.filter { it.id != room.id }
                _roomMedia.value = _roomMedia.value.filterKeys { it != room.id }
                
                dismissUnlockDialog()
                closeOverlay()
                
                Toast.makeText(
                    context,
                    "Room \"${room.roomName}\" deleted",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error deleting room", e)
                Toast.makeText(
                    context,
                    "Failed to delete room",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ========== THEME & CUSTOMIZATION FUNCTIONS ==========
    
    fun loadThemePreferences(context: Context) {
        if (preferencesManager == null) {
            preferencesManager = PreferencesManager(context)
        }
        _selectedTheme.value = AppThemes.getThemeByName(preferencesManager!!.getTheme())
        _accentColor.value = preferencesManager!!.getAccentColor()
    }
    
    fun selectTheme(context: Context, theme: AppTheme) {
        if (preferencesManager == null) {
            preferencesManager = PreferencesManager(context)
        }
        _selectedTheme.value = theme
        preferencesManager!!.saveTheme(theme.name)
    }
    
    fun selectAccentColor(context: Context, color: Color) {
        if (preferencesManager == null) {
            preferencesManager = PreferencesManager(context)
        }
        _accentColor.value = color
        preferencesManager!!.saveAccentColor(color)
    }
    
    // ========== CREDENTIAL UPDATE FUNCTIONS ==========
    
    fun updateEmail(context: Context, newEmail: String, currentPassword: String) {
        viewModelScope.launch {
            _isUpdatingCredential.value = true
            _credentialUpdateError.value = null
            _credentialUpdateSuccess.value = null
            
            try {
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                
                if (user == null) {
                    _credentialUpdateError.value = "Not authenticated"
                    _isUpdatingCredential.value = false
                    return@launch
                }
                
                // Re-authenticate first (Firebase requirement)
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                    if (reAuthTask.isSuccessful) {
                        // Update email in Firebase
                        user.updateEmail(newEmail).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                // Update email in backend MongoDB
                                viewModelScope.launch {
                                    try {
                                        val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
                                        val token = sessionManager.getToken()
                                        
                                        if (token != null) {
                                            val apiService = com.dmb.bestbefore.data.api.RetrofitClient.apiService
                                            val response = apiService.updateUserEmail(mapOf("email" to newEmail))
                                            
                                            if (response.isSuccessful) {
                                                _credentialUpdateSuccess.value = "Email updated successfully!"
                                                // Update session with new email
                                                sessionManager.saveUserEmail(newEmail)
                                            } else {
                                                _credentialUpdateError.value = "Backend update failed"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        _credentialUpdateError.value = "Backend error: ${e.message}"
                                    } finally {
                                        _isUpdatingCredential.value = false
                                    }
                                }
                            } else {
                                _credentialUpdateError.value = updateTask.exception?.message ?: "Firebase update failed"
                                _isUpdatingCredential.value = false
                            }
                        }
                    } else {
                        _credentialUpdateError.value = "Re-authentication failed. Check password."
                        _isUpdatingCredential.value = false
                    }
                }
            } catch (e: Exception) {
                _credentialUpdateError.value = e.message ?: "Update failed"
                _isUpdatingCredential.value = false
            }
        }
    }
    
    fun updatePassword(context: Context, newPassword: String, currentPassword: String) {
        viewModelScope.launch {
            _isUpdatingCredential.value = true
            _credentialUpdateError.value = null
            _credentialUpdateSuccess.value = null
            
            try {
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                
                if (user == null) {
                    _credentialUpdateError.value = "Not authenticated"
                    _isUpdatingCredential.value = false
                    return@launch
                }
                
                // Re-authenticate first
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                    if (reAuthTask.isSuccessful) {
                        // Update password in Firebase
                        user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                // Update password in backend MongoDB
                                viewModelScope.launch {
                                    try {
                                        val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
                                        val token = sessionManager.getToken()
                                        
                                        if (token != null) {
                                            val apiService = com.dmb.bestbefore.data.api.RetrofitClient.apiService
                                            val response = apiService.updateUserPassword(mapOf("password" to newPassword))
                                            
                                            if (response.isSuccessful) {
                                                _credentialUpdateSuccess.value = "Password updated successfully!"
                                            } else {
                                                _credentialUpdateError.value = "Backend update failed"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        _credentialUpdateError.value = "Backend error: ${e.message}"
                                    } finally {
                                        _isUpdatingCredential.value = false
                                    }
                                }
                            } else {
                                _credentialUpdateError.value = updateTask.exception?.message ?: "Firebase update failed"
                                _isUpdatingCredential.value = false
                            }
                        }
                    } else {
                        _credentialUpdateError.value = "Re-authentication failed. Check current password."
                        _isUpdatingCredential.value = false
                    }
                }
            } catch (e: Exception) {
                _credentialUpdateError.value = e.message ?: "Update failed"
                _isUpdatingCredential.value = false
            }
        }
    }
    
    fun clearCredentialMessages() {
        _credentialUpdateError.value = null
        _credentialUpdateSuccess.value = null
    }

    fun logout(context: Context) {
        // Clear session data
        val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
        sessionManager.clearSession()
        
        // Clear auth repository prefs as well if they are separate (they seem to be inconsistent in the codebase)
        // Ideally we should unify, but for safety lets clear the one used in initDatabase too
        val prefs = context.getSharedPreferences("BestBeforePrefs", Context.MODE_PRIVATE)
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
    PROFILE_MENU,
    // Create Room wizard steps (in order)
    ROOM_NAME,              // Step 1: name + public/private
    ROOM_TIME_CAPSULE,      // Step 2: time capsule settings
    ROOM_ATMOSPHERE,        // Step 3: room theme + background music
    ROOM_MEMORY_RULES,      // Step 4: memory dump rules
    ROOM_INVITE,            // Step 5: invite friends
    // Other
    ROOM_DETAIL,
    EDIT_ROOM,
    CREATE_HALLWAY,
    CAMERA,
    TIME_CAPSULE_LIST,
    NO_OP,
    SAVED_ROOMS_LIST,
    // Legacy
    ROOM_TIME,
    ROOM_MODE
}

enum class UnlockMethod { DURATION, SPECIFIC_DATE }

enum class RoomCreationSource {
    HALLWAY,
    PROFILE_MENU
}
