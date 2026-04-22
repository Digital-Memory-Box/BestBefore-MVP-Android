package com.dmb.bestbefore.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.models.TimeCapsuleRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.widget.Toast
import android.util.Log
import java.io.File

import com.dmb.bestbefore.data.models.CalendarEvent
import com.dmb.bestbefore.ui.theme.AppTheme
import com.dmb.bestbefore.ui.theme.AppThemes
import com.dmb.bestbefore.data.local.PreferencesManager
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.api.models.UpdateMeRequest

class ProfileViewModel : ViewModel() {
    companion object {
        val ARTIST_ROOM_EMOTIONS = listOf("warmed", "moved", "soothed", "struck", "stayed")
    }

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

    // RoomRepository — no token arg; fetches fresh Firebase token per request (matches iOS pattern)
    private val roomRepository = com.dmb.bestbefore.data.repository.RoomRepository()
    private val notificationRepository = com.dmb.bestbefore.data.repository.NotificationRepository()
    
    private val _notifications = MutableStateFlow<List<com.dmb.bestbefore.data.models.AppNotification>>(emptyList())
    val notifications: StateFlow<List<com.dmb.bestbefore.data.models.AppNotification>> = _notifications.asStateFlow()
    
    // Switch createdRooms to loading from DB/API
    private val _createdRooms = MutableStateFlow<List<TimeCapsuleRoom>>(emptyList())
    val createdRooms: StateFlow<List<TimeCapsuleRoom>> = _createdRooms.asStateFlow()
    
    private val _totalRooms = MutableStateFlow(0)
    val totalRooms: StateFlow<Int> = _totalRooms.asStateFlow()
    
    private val _totalMemories = MutableStateFlow(0)
    val totalMemories: StateFlow<Int> = _totalMemories.asStateFlow()
    
    private val _roomingCount = MutableStateFlow(0)
    val roomingCount: StateFlow<Int> = _roomingCount.asStateFlow()

    private val _roomersCount = MutableStateFlow(0)
    val roomersCount: StateFlow<Int> = _roomersCount.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private var audioRecorderHelper: com.dmb.bestbefore.utils.AudioRecorderHelper? = null
    
    private var creationSource: RoomCreationSource = RoomCreationSource.HALLWAY



    // Profile State
    data class RecentActivity(
        val type: ActivityType,
        val title: String, 
        val date: Long,
        val subtitle: String? = null
    )
    
    enum class ActivityType {
         CREATED_ROOM, ADDED_PHOTOS, ADDED_NOTE
    }

    private val _recentActivities = MutableStateFlow<List<RecentActivity>>(emptyList())
    val recentActivities: StateFlow<List<RecentActivity>> = _recentActivities.asStateFlow()

    private val _profileImageUri = MutableStateFlow<Uri?>(null)
    val profileImageUri: StateFlow<Uri?> = _profileImageUri.asStateFlow()

    private val _roomEmotions = MutableStateFlow<Map<String, String>>(emptyMap())
    val roomEmotions: StateFlow<Map<String, String>> = _roomEmotions.asStateFlow()

    
    private val _userName = MutableStateFlow("User") 
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _bio = MutableStateFlow("Digital artist focusing on surreal landscapes and vibrant color theory.")
    val bio: StateFlow<String> = _bio.asStateFlow()

    // ── Profile Tags ─────────────────────────────────────────────────
    private val _profileTags = MutableStateFlow<List<String>>(emptyList())
    val profileTags: StateFlow<List<String>> = _profileTags.asStateFlow()

    fun addProfileTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isNotEmpty() && _profileTags.value.none { it.equals(trimmed, ignoreCase = true) }) {
            _profileTags.value = _profileTags.value + trimmed
        }
    }

    fun removeProfileTag(tag: String) {
        _profileTags.value = _profileTags.value.filter { !it.equals(tag, ignoreCase = true) }
    }

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

    private val _applyAccentToAll = MutableStateFlow(false)
    val applyAccentToAll: StateFlow<Boolean> = _applyAccentToAll.asStateFlow()

    private val _syncAccentWithRoom = MutableStateFlow(false)
    val syncAccentWithRoom: StateFlow<Boolean> = _syncAccentWithRoom.asStateFlow()
    
    fun toggleApplyAccent(context: Context, enabled: Boolean) {
        _applyAccentToAll.value = enabled
        com.dmb.bestbefore.ui.theme.ThemeState.updateApplyAccentToAll(enabled)
    }

    fun toggleSyncAccent(context: Context, enabled: Boolean) {
        _syncAccentWithRoom.value = enabled
        com.dmb.bestbefore.ui.theme.ThemeState.updateSyncAccentWithRoom(enabled)
    }

    // Helper for resolving connected rooms names
    fun getRoomByIdFromRemote(id: String): TimeCapsuleRoom? {
        // First check in created rooms
        _createdRooms.value.find { it.id == id }?.let { return it }
        // Potentially check a global room cache if implemented, 
        // for now we just return null and the UI handles the ID fallback.
        return null
    }
    
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

    // Calendar events
    private val _calendarEvents = MutableStateFlow<List<com.dmb.bestbefore.data.models.CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<com.dmb.bestbefore.data.models.CalendarEvent>> = _calendarEvents.asStateFlow()

    fun loadCalendarEvents(context: Context) {
        _calendarEvents.value = com.dmb.bestbefore.CalendarHelper.getUpcomingEvents(context)
    }

    fun applyCalendarEvent(event: com.dmb.bestbefore.data.models.CalendarEvent) {
        _roomName.value = event.title
        _unlockMethod.value = UnlockMethod.SPECIFIC_DATE
        _targetTime.value = event.startTime.time
        
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = event.startTime.time
        _targetHour.value = cal.get(java.util.Calendar.HOUR_OF_DAY)
        _targetMinute.value = cal.get(java.util.Calendar.MINUTE)
    }

    // Atmosphere room theme (string name, separate from AppTheme)
    private val _roomAtmosphereTheme = MutableStateFlow("Default")
    val roomAtmosphereTheme: StateFlow<String> = _roomAtmosphereTheme.asStateFlow()

    private val _roomTags = MutableStateFlow<List<String>>(emptyList())
    val roomTags: StateFlow<List<String>> = _roomTags.asStateFlow()

    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags: StateFlow<List<String>> = _availableTags.asStateFlow()

    private val _roomDescription = MutableStateFlow("")
    val roomDescription: StateFlow<String> = _roomDescription.asStateFlow()

    fun updateRoomName(name: String) {
        _roomName.value = name
    }

    fun updateRoomDescription(desc: String) {
        _roomDescription.value = desc
    }

    fun addRoomTag(tag: String) {
        if (!_roomTags.value.contains(tag)) {
            _roomTags.value = _roomTags.value + tag
        }
    }

    fun removeRoomTag(tag: String) {
        _roomTags.value = _roomTags.value - tag
    }

    // Atmosphere: background music
    private val _selectedMusic = MutableStateFlow("None")
    val selectedMusic: StateFlow<String> = _selectedMusic.asStateFlow()

    // Rolling expiration: Never | 1 Day (24h) | 7 Days | 30 Days
    private val _rollingExpiration = MutableStateFlow("Never")
    val rollingExpiration: StateFlow<String> = _rollingExpiration.asStateFlow()

    private val _scheduledClosureEnabled = MutableStateFlow(false)
    val scheduledClosureEnabled: StateFlow<Boolean> = _scheduledClosureEnabled.asStateFlow()

    // Scheduled closure datetime
    private val _scheduledClosureTime = MutableStateFlow(System.currentTimeMillis() + 7 * 86400000L)
    val scheduledClosureTime: StateFlow<Long> = _scheduledClosureTime.asStateFlow()

    private val _scheduledClosureHour = MutableStateFlow(23)
    val scheduledClosureHour: StateFlow<Int> = _scheduledClosureHour.asStateFlow()

    private val _scheduledClosureMinute = MutableStateFlow(59)
    val scheduledClosureMinute: StateFlow<Int> = _scheduledClosureMinute.asStateFlow()

    data class InvitedUser(
        val email: String,
        val name: String? = null,
        val role: String = "collaborator" // "collaborator" or "viewer"
    )

    private val _invitedUsers = MutableStateFlow<List<InvitedUser>>(emptyList())
    val invitedUsers: StateFlow<List<InvitedUser>> = _invitedUsers.asStateFlow()

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
    
    // Profile Music state
    private val _profileMusic = MutableStateFlow<String?>("None")
    val profileMusic: StateFlow<String?> = _profileMusic.asStateFlow()

    fun updateProfileMusic(music: String?) {
        _profileMusic.value = music ?: "None"
    }

    fun saveProfileMusic(context: Context, trackName: String?) {
        viewModelScope.launch {
            val updateMusic = if (trackName == "None") null else trackName
            val authRepo = com.dmb.bestbefore.data.repository.AuthRepository(context)
            val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
            val result = authRepo.updateMe(com.dmb.bestbefore.data.api.models.UpdateMeRequest(profileMusic = updateMusic))
            if (result.isSuccess) {
                _profileMusic.value = trackName ?: "None"
                sessionManager.saveProfileMusic(updateMusic)
                android.widget.Toast.makeText(context, "Profile music updated", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Failed to update profile music", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Room unlock dialog state
    private val _showUnlockDialog = MutableStateFlow(false)
    val showUnlockDialog: StateFlow<Boolean> = _showUnlockDialog.asStateFlow()
    
    private val _unlockDialogRoom = MutableStateFlow<TimeCapsuleRoom?>(null)
    val unlockDialogRoom: StateFlow<TimeCapsuleRoom?> = _unlockDialogRoom.asStateFlow()

    // Unlocked Photos Viewer State
    private val _unlockedPhotosRoom = MutableStateFlow<TimeCapsuleRoom?>(null)
    val unlockedPhotosRoom: StateFlow<TimeCapsuleRoom?> = _unlockedPhotosRoom.asStateFlow()

    // Invite Pop-Up State
    private val _pendingInviteRoomId = MutableStateFlow<String?>(null)
    val pendingInviteRoomId: StateFlow<String?> = _pendingInviteRoomId.asStateFlow()
    
    private val _pendingInviteRoomName = MutableStateFlow<String?>(null)
    val pendingInviteRoomName: StateFlow<String?> = _pendingInviteRoomName.asStateFlow()

    fun showInviteDialog(roomId: String, roomName: String) {
        _pendingInviteRoomId.value = roomId
        _pendingInviteRoomName.value = roomName
    }

    fun hideInviteDialog() {
        _pendingInviteRoomId.value = null
        _pendingInviteRoomName.value = null
    }

    fun handleAcceptInvite(context: Context, roomId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = roomRepository.acceptInvite(roomId)
                result.onSuccess {
                    hideInviteDialog()
                    Toast.makeText(context, "Invitation accepted!", Toast.LENGTH_SHORT).show()
                    initDatabase(context) // Refresh room list
                }
                result.onFailure {
                    Toast.makeText(context, "Failed to accept invite", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun handleDeclineInvite(context: Context, roomId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = roomRepository.declineInvite(roomId)
                result.onSuccess {
                    hideInviteDialog()
                    Toast.makeText(context, "Invitation declined", Toast.LENGTH_SHORT).show()
                }
                result.onFailure {
                    Toast.makeText(context, "Failed to decline invite", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            val result = notificationRepository.getNotifications()
            result.onSuccess { list ->
                _notifications.value = list
            }
        }
    }

    fun handleRespondToNotification(context: Context, notification: com.dmb.bestbefore.data.models.AppNotification, accept: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = notificationRepository.respondToInvitation(notification.id, accept)
                result.onSuccess {
                    Toast.makeText(context, if (accept) "Invitation accepted!" else "Invitation ignored", Toast.LENGTH_SHORT).show()
                    fetchNotifications()
                    if (accept) initDatabase(context) // Refresh rooms if accepted
                }
                result.onFailure {
                    Toast.makeText(context, "Failed to respond to invitation", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Callbacks for permissions
    var onRequestNotificationPermission: (() -> Unit)? = null
    var onRequestCalendarPermission: (() -> Unit)? = null
    var onRequestReadCalendarPermission: (() -> Unit)? = null
    var onRequestCameraPermission: (() -> Unit)? = null
    var onRequestGalleryPermission: (() -> Unit)? = null
    var onRequestFilePermission: (() -> Unit)? = null

    // Helper context for DB init (Simple MVP approach)
    fun initDatabase(context: Context) {
        val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val savedName = sessionManager.getUserName()
        val savedMusic = sessionManager.getProfileMusic()
        val savedProfilePhotoUri = sessionManager.getProfilePhotoUri()
        _userName.value = if (!savedName.isNullOrEmpty()) savedName else "User"
        _profileMusic.value = if (!savedMusic.isNullOrEmpty()) savedMusic else "None"
        _profileImageUri.value = savedProfilePhotoUri?.let { Uri.parse(it) }
        _roomEmotions.value = sessionManager.getRoomEmotions(currentUserId)

        viewModelScope.launch {
            try {
                val authRepo = com.dmb.bestbefore.data.repository.AuthRepository(context)
                val meResult = authRepo.getMe()
                meResult.onSuccess { userDto ->
                    if (!userDto.name.isNullOrEmpty()) _userName.value = userDto.name
                    if (!userDto.bio.isNullOrEmpty()) {
                        _bio.value = userDto.bio
                    } else {
                        // User has no bio yet, clear placeholder so field is blank
                        _bio.value = ""
                    }
                    if (!userDto.profileImageUrl.isNullOrEmpty()) {
                        _profileImageUri.value = Uri.parse(userDto.profileImageUrl)
                    }
                    // Load profile tags from backend
                    if (!userDto.tags.isNullOrEmpty()) {
                        _profileTags.value = userDto.tags
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to fetch user profile", e)
            }
        }

        fetchNotifications()

        // RoomRepository now fetches its own fresh Firebase token per request.
        // Just launch the load — no token management needed here.
        viewModelScope.launch {
            try {
                val result = roomRepository.getRooms()
                val allRooms = mutableListOf<TimeCapsuleRoom>()
                result.onSuccess { apiRooms ->
                    Log.d("ProfileViewModel", "Fetched ${apiRooms.size} rooms from backend")
                    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
                    
                    val myCreatedRoomsDtos = apiRooms.filter { room ->
                        room.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true
                    }
                    
                    val myJoinedRoomsDtos = apiRooms.filter { room ->
                        room.ownerEmail?.equals(currentUserEmail, ignoreCase = true) != true &&
                        room.collaborators?.any { element ->
                            if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                                element.asJsonObject.get("email").asString.equals(currentUserEmail, ignoreCase = true)
                            } else false
                        } == true
                    }
                    
                    val uniqueRoomers = mutableSetOf<String>()
                    myCreatedRoomsDtos.forEach { room ->
                        room.collaborators?.forEach { element ->
                            if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                                uniqueRoomers.add(element.asJsonObject.get("email").asString)
                            }
                        }
                    }
                    
                    _roomingCount.value = myJoinedRoomsDtos.size
                    _roomersCount.value = uniqueRoomers.size
                    
                    // The UI requirement stipulates "My Rooms" strictly lists created rooms.
                    allRooms.addAll(mapDtosToRooms(myCreatedRoomsDtos, isSaved = false))
                }
                result.onFailure { e ->
                    Log.e("ProfileViewModel", "getRooms failed: ${e.message}")
                }

                // Fetch memories for each room
                val mediaMap = mutableMapOf<String, List<Uri>>()
                var totalLoadedMemories = 0

                allRooms.forEach { room ->
                    val memoriesUrls = mutableListOf<String>()
                    val memoriesResult = roomRepository.getMemoriesByRoom(room.id)
                    memoriesResult.onSuccess { memories ->
                        memories.forEach { memory ->
                            val content = memory["content"] as? String
                            val type = memory["type"] as? String
                            val title = memory["title"] as? String
                            if (content != null) {
                                when {
                                    type == "audio" -> memoriesUrls.add("data:audio/mp4;base64,$content")
                                    type == "note" -> memoriesUrls.add("NOTE:${title ?: ""}:$content")
                                    content.startsWith("http") -> memoriesUrls.add(content)
                                    content.length > 100 -> memoriesUrls.add("data:image/jpeg;base64,$content")
                                }
                            }
                        }
                    }
                    mediaMap[room.id] = memoriesUrls.map { Uri.parse(it) }
                    totalLoadedMemories += memoriesUrls.size
                }

                _roomMedia.value = mediaMap
                _totalMemories.value = totalLoadedMemories
                refreshRoomLists(allRooms)

                // Fetch tags
                try {
                    val authRepo = com.dmb.bestbefore.data.repository.AuthRepository(context)
                    val token = authRepo.getFirebaseIdToken(false)
                    if (token != null) {
                        val tagsResponse = com.dmb.bestbefore.data.api.RetrofitClient.apiService.getTags("Bearer $token")
                        if (tagsResponse.isSuccessful) {
                            val bodyElement = tagsResponse.body()
                            val parsedTags = mutableListOf<String>()
                            if (bodyElement != null) {
                                if (bodyElement.isJsonArray) {
                                    bodyElement.asJsonArray.forEach { 
                                        if (it.isJsonObject) {
                                            val obj = it.asJsonObject
                                            val tag = obj.get("name")?.asString ?: obj.get("tag")?.asString
                                            if (tag != null) parsedTags.add(tag)
                                        } else if (it.isJsonPrimitive) {
                                            parsedTags.add(it.asString)
                                        }
                                    }
                                } else if (bodyElement.isJsonObject) {
                                    val obj = bodyElement.asJsonObject
                                    // if it's { "tags": [...] }
                                    if (obj.has("tags") && obj.get("tags").isJsonArray) {
                                        obj.get("tags").asJsonArray.forEach { 
                                            if (it.isJsonPrimitive) parsedTags.add(it.asString)
                                            else if (it.isJsonObject) {
                                                val t = it.asJsonObject.get("name")?.asString ?: it.asJsonObject.get("tag")?.asString
                                                if (t != null) parsedTags.add(t)
                                            }
                                        }
                                    }
                                }
                            }
                            _availableTags.value = parsedTags
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Failed to fetch tags", e)
                }

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "initDatabase failed", e)
            }
        }
    }

    private fun mapDtosToRooms(dtos: List<com.dmb.bestbefore.data.api.models.RoomDto>, isSaved: Boolean): List<TimeCapsuleRoom> {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        return dtos.map { dto ->
            val createdMs = parseCreatedAt(dto.createdAt)
            val unlock = createdMs + (dto.capsuleDurationDays * 24 * 3600 * 1000L) +
                          (dto.capsuleDurationHours * 3600 * 1000L) + (dto.capsuleDurationMinutes * 60 * 1000L)
            val closureMs = dto.expirationDate?.let { parseCreatedAt(it) } ?: 0L
            val rollingString = when (dto.rollingExpiryDays) {
                1 -> "24 hours"
                7 -> "1 week"
                30 -> "30 days"
                365 -> "1 year"
                else -> "Never"
            }
            val isViewerOnly = dto.viewers?.any { element ->
                if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                    element.asJsonObject.get("email").asString.equals(currentUserEmail, ignoreCase = true)
                } else false
            } == true
            val isOwnedByMe = dto.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true
            val isCollaborator = dto.collaborators?.any { element ->
                if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                    element.asJsonObject.get("email").asString.equals(currentUserEmail, ignoreCase = true)
                } else false
            } == true

            TimeCapsuleRoom(
                id = dto.id,
                roomName = dto.name,
                capsuleDays = dto.capsuleDurationDays,
                capsuleHours = dto.capsuleDurationHours,
                capsuleMinutes = dto.capsuleDurationMinutes,
                notificationDays = dto.capsuleDurationDays,
                notificationHours = dto.capsuleDurationHours,
                notificationMinutes = dto.capsuleDurationMinutes,
                isPublic = !dto.isPrivate,
                isCollaboration = dto.isTimeCapsule,
                photos = dto.photos ?: emptyList(),
                unlockTime = if (dto.unlockDate != null) parseCreatedAt(dto.unlockDate) else unlock,
                scheduledClosureTime = closureMs,
                dateCreated = createdMs,
                isSaved = isSaved,
                theme = dto.theme ?: "Default",
                tags = dto.tags ?: emptyList(),
                description = dto.description,
                music = dto.backgroundMusic ?: "None",
                rollingExpiration = rollingString,
                isViewerOnly = isViewerOnly,
                connectedRooms = dto.connectedRooms ?: emptyList(),
                isOwnedByMe = isOwnedByMe,
                isCollaborator = isCollaborator,
                ownerUserType = dto.ownerUserType
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
        _totalMemories.value = active.sumOf { room -> 
            _roomMedia.value[room.id]?.size ?: 0 
        }
    }
    
    // Camera capture state
    private val _capturedImageUri = kotlinx.coroutines.flow.MutableStateFlow<android.net.Uri?>(null)
    val capturedImageUri: kotlinx.coroutines.flow.StateFlow<android.net.Uri?> = _capturedImageUri.asStateFlow()
    
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

    fun finalizeRoom(context: Context? = null) {
        // Request notification permission before creating room
        onRequestNotificationPermission?.invoke()
        
        // Calculate duration depending on the unlock method
        val now = System.currentTimeMillis()
        val days: Int
        val hours: Int
        val minutes: Int
        val finalTargetTime: Long
        
        if (_unlockMethod.value == UnlockMethod.DURATION) {
            days = _capsuleDays.value
            hours = _capsuleHours.value
            minutes = _capsuleMins.value
            val durationMillis = (days * 24L * 3600 * 1000) + (hours * 3600L * 1000) + (minutes * 60L * 1000)
            finalTargetTime = now + durationMillis
        } else {
            val durationMillis = (_targetTime.value - now).coerceAtLeast(0)
            days = (durationMillis / (24 * 3600 * 1000)).toInt()
            hours = ((durationMillis % (24 * 3600 * 1000)) / (3600 * 1000)).toInt()
            minutes = ((durationMillis % (3600 * 1000)) / (60 * 1000)).toInt()
            finalTargetTime = _targetTime.value
        }
        
        val newRoom = TimeCapsuleRoom(
            id = java.util.UUID.randomUUID().toString(),
            roomName = _roomName.value,
            capsuleDays = days,
            capsuleHours = hours,
            capsuleMinutes = minutes,
            notificationDays = days,
            notificationHours = hours,
            notificationMinutes = minutes,
            isPublic = _isPublic.value,
            isCollaboration = _isTimeCapsuleEnabled.value,
            unlockTime = finalTargetTime,
            scheduledClosureTime = if (_scheduledClosureEnabled.value) _scheduledClosureTime.value else 0L,
            theme = _roomAtmosphereTheme.value,
            tags = _roomTags.value,
            description = _roomDescription.value,
            music = _selectedMusic.value,
            rollingExpiration = _rollingExpiration.value,
            isOwnedByMe = true,
            isCollaborator = false
        )
        // Create Room via API
        viewModelScope.launch {
             // Convert scheduledClosureTime millis -> ISO-8601 string for backend
             val closureIso: String? = if (_scheduledClosureEnabled.value) {
                 java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                     timeZone = java.util.TimeZone.getTimeZone("UTC")
                 }.format(java.util.Date(_scheduledClosureTime.value))
             } else null

             val unlockIso: String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                 timeZone = java.util.TimeZone.getTimeZone("UTC")
             }.format(java.util.Date(newRoom.unlockTime))

             val rollingDays = when (newRoom.rollingExpiration) {
                 "24 hours" -> 1
                 "1 week" -> 7
                 "30 days" -> 30
                 "1 year" -> 365
                 else -> 0
             }

             val result = roomRepository.createRoom(
                 newRoom.roomName,
                 newRoom.capsuleDays,
                 newRoom.capsuleHours,
                 newRoom.capsuleMinutes,
                 newRoom.isPublic,
                 newRoom.isCollaboration,
                 newRoom.theme,
                 collaborators = _invitedUsers.value.filter { it.role == "collaborator" }.map { it.email },
                 viewers = _invitedUsers.value.filter { it.role == "viewer" }.map { it.email },
                 scheduledClosureIso = closureIso,
                 unlockDateIso = unlockIso,
                 rollingExpiryDays = rollingDays,
                 description = newRoom.description,
                 tags = newRoom.tags,
                 music = newRoom.music
             )

             val finalRoom = if (result.isSuccess) {
                 val realId = result.getOrNull()
                 Log.d("ProfileViewModel", "Room created with id=$realId")
                 if (realId != null) {
                     newRoom.copy(id = realId, isOwnedByMe = true, isCollaborator = false)
                 } else {
                     newRoom
                 }
             } else {
                 Log.e("ProfileViewModel", "createRoom failed: ${result.exceptionOrNull()?.message}")
                 newRoom
             }
             
             // Add to local list
             val updatedList = _createdRooms.value + finalRoom
             refreshRoomLists(updatedList)
             
             // Keep user in flow by navigating to the new room detail
             selectRoom(finalRoom)
             
             // Save Room Created notification to in-app Notification Center
             context?.let { ctx ->
                val appNotification = com.dmb.bestbefore.data.models.AppNotification(
                    title = "Room Created",
                    message = "You successfully created the room \"${finalRoom.roomName}\"",
                    type = com.dmb.bestbefore.data.models.NotificationType.ROOM_CREATED,
                    relatedRoomId = finalRoom.id
                )
                // Persisting to backend is not available here; update local state for immediate UI feedback.
                _notifications.value = listOf(appNotification) + _notifications.value
            }
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
            // we skip local calendar write logic. Backend could potentially create it if we added that endpoint.
            // For now, removing local calendar write logic.
        }
    }
    
    fun sortRoomsByDateCreated() {
        _createdRooms.value = _createdRooms.value.sortedByDescending { it.dateCreated }
    }
    
    // Media Persistence (Room ID -> List of Uris)

    fun uploadNote(context: Context, noteContent: String) {
        val currentRoomId = _selectedRoom.value?.id ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val memoryData: Map<String, Any> = mapOf(
                    "type" to "note",
                    "title" to "Written Note",
                    "content" to noteContent,
                    "metadata" to emptyMap<String, Any>()
                )
                val result = roomRepository.addMemoryToRoom(currentRoomId, memoryData)
                result.onSuccess {
                    val currentMedia = _roomMedia.value[currentRoomId] ?: emptyList()
                    val dataUri = Uri.parse("NOTE:Written Note:$noteContent")
                    _roomMedia.value = _roomMedia.value + (currentRoomId to (currentMedia + dataUri))
                    _totalMemories.value += 1

                    val newActivity = RecentActivity(
                        type = ActivityType.ADDED_NOTE,
                        title = "Added a note to \"${_selectedRoom.value?.roomName}\"",
                        date = System.currentTimeMillis()
                    )
                    _recentActivities.value = listOf(newActivity) + _recentActivities.value

                    Toast.makeText(context, "Note saved!", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Failed to save note", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to upload note", e)
                Toast.makeText(context, "Error saving note", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }
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
                val uploadedDataUris = mutableListOf<String>()

                currentSelection.forEach { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri) ?: return@forEach
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        val mimeType = (context.contentResolver.getType(uri) ?: "").lowercase()
                        if (mimeType.startsWith("audio/")) {
                            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            val memoryData: Map<String, Any> = mapOf(
                                "type" to "audio",
                                "title" to "Audio Drop",
                                "content" to base64,
                                "metadata" to mapOf("mimeType" to mimeType)
                            )
                            val result = roomRepository.addMemoryToRoom(currentRoomId, memoryData)
                            result.onSuccess {
                                uploadedDataUris.add("data:$mimeType;base64,$base64")
                            }
                        } else {
                            // Downsample image payloads for safer upload and render.
                            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                            var inSampleSize = 1
                            while (options.outWidth / inSampleSize > 1024 || options.outHeight / inSampleSize > 1024) {
                                inSampleSize *= 2
                            }

                            options.inJustDecodeBounds = false
                            options.inSampleSize = inSampleSize
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                            if (bitmap == null) return@forEach

                            val bos = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, bos)
                            val compressed = bos.toByteArray()
                            val base64 = android.util.Base64.encodeToString(compressed, android.util.Base64.NO_WRAP)

                            val memoryData: Map<String, Any> = mapOf(
                                "type" to "photo",
                                "title" to "Photo Drop",
                                "content" to base64,
                                "metadata" to emptyMap<String, Any>()
                            )
                            val result = roomRepository.addMemoryToRoom(currentRoomId, memoryData)
                            result.onSuccess {
                                uploadedDataUris.add("data:image/jpeg;base64,$base64")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Upload failed for uri=$uri", e)
                    }
                }

                if (uploadedDataUris.isNotEmpty()) {
                    // Update local room media state so photos appear instantly
                    val currentMedia = _roomMedia.value[currentRoomId] ?: emptyList()
                    val newUris = uploadedDataUris.map { Uri.parse(it) }
                    _roomMedia.value = _roomMedia.value + (currentRoomId to (currentMedia + newUris))

                    _selectedMediaUris.value = emptyList()
                    _totalMemories.value += uploadedDataUris.size

                    val newActivity = RecentActivity(
                        type = ActivityType.ADDED_PHOTOS,
                        title = "Added ${uploadedDataUris.size} photo(s) to \"${_selectedRoom.value?.roomName}\"",
                        date = System.currentTimeMillis()
                    )
                    _recentActivities.value = listOf(newActivity) + _recentActivities.value

                    Toast.makeText(context, "Uploaded ${uploadedDataUris.size} photo(s)!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload failed – check connection", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Refresh memories for the current room from the backend (pull-to-refresh)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshRoomMemories() {
        val currentRoomId = _selectedRoom.value?.id ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val memoriesUrls = mutableListOf<String>()
                val memoriesResult = roomRepository.getMemoriesByRoom(currentRoomId)
                memoriesResult.onSuccess { memories ->
                    memories.forEach { memory ->
                        val content = memory["content"] as? String
                        val type = memory["type"] as? String
                        val title = memory["title"] as? String
                        @Suppress("UNCHECKED_CAST")
                        val metadata = memory["metadata"] as? Map<String, Any?>
                        val mimeType = metadata?.get("mimeType") as? String
                        if (content != null) {
                            when {
                                type == "audio" -> memoriesUrls.add("data:${mimeType ?: "audio/mp4"};base64,$content")
                                type == "note" -> memoriesUrls.add("NOTE:${title ?: ""}:$content")
                                content.startsWith("http") -> memoriesUrls.add(content)
                                content.length > 100 -> memoriesUrls.add("data:image/jpeg;base64,$content")
                            }
                        }
                    }
                }
                _roomMedia.value = _roomMedia.value + (currentRoomId to memoriesUrls.map { Uri.parse(it) })
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "refreshRoomMemories failed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ── Invite Token Management ─────────────────────────────────────────────
    private val _inviteToken = MutableStateFlow<String?>(null)
    val inviteToken: StateFlow<String?> = _inviteToken.asStateFlow()

    private val _inviteLink = MutableStateFlow<String?>(null)
    val inviteLink: StateFlow<String?> = _inviteLink.asStateFlow()

    private val _isGeneratingToken = MutableStateFlow(false)
    val isGeneratingToken: StateFlow<Boolean> = _isGeneratingToken.asStateFlow()

    fun generateInviteToken() {
        val roomId = _selectedRoom.value?.id ?: return
        viewModelScope.launch {
            _isGeneratingToken.value = true
            try {
                val result = roomRepository?.generateInviteToken(roomId)
                result?.onSuccess { data ->
                    _inviteToken.value = data["token"] as? String
                    _inviteLink.value = data["inviteLink"] as? String
                }
                result?.onFailure { e ->
                    Log.e("ProfileViewModel", "Failed to generate invite token", e)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "generateInviteToken exception", e)
            } finally {
                _isGeneratingToken.value = false
            }
        }
    }

    fun joinRoomViaToken(context: Context, token: String) {
        viewModelScope.launch {
            try {
                val result = roomRepository?.joinViaInviteToken(token)
                result?.onSuccess { data ->
                    val roomName = data["roomName"] as? String ?: "Room"
                    val alreadyMember = data["alreadyMember"] as? Boolean ?: false
                    if (alreadyMember) {
                        Toast.makeText(context, "You're already a member of \"$roomName\"", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Joined \"$roomName\" successfully!", Toast.LENGTH_SHORT).show()
                    }
                    initDatabase(context)
                }
                result?.onFailure { e ->
                    Toast.makeText(context, "Failed to join: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "joinRoomViaToken exception", e)
                Toast.makeText(context, "Failed to join room", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Join a room via QR scan or /join/{roomId} HTTPS App Link — works cross-platform with iOS */
    fun joinRoomViaQR(context: Context, roomId: String) {
        viewModelScope.launch {
            try {
                val result = roomRepository.joinViaQR(roomId)
                result.onSuccess { data ->
                    val roomName = data["roomName"] as? String ?: "Room"
                    val alreadyMember = data["alreadyMember"] as? Boolean ?: false
                    if (alreadyMember) {
                        Toast.makeText(context, "You're already in \"$roomName\"", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Join request sent for \"$roomName\"!", Toast.LENGTH_SHORT).show()
                    }
                    initDatabase(context)
                }
                result.onFailure { e ->
                    Toast.makeText(context, "Failed to join room: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "joinRoomViaQR exception", e)
                Toast.makeText(context, "Failed to join room", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendHandshakeInvite(context: Context, email: String) {
        val roomId = _selectedRoom.value?.id ?: return
        viewModelScope.launch {
            try {
                val result = roomRepository.createHandshakeInvite(roomId, email)
                result.onSuccess {
                    Toast.makeText(context, "Invite sent to $email!", Toast.LENGTH_SHORT).show()
                }
                result.onFailure { e ->
                    Toast.makeText(context, "Failed to send invite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "sendHandshakeInvite exception", e)
                Toast.makeText(context, "Failed to send invite", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearInviteToken() {
        _inviteToken.value = null
        _inviteLink.value = null
    }

    fun startAudioRecording(context: Context) {
        if (audioRecorderHelper == null) {
            audioRecorderHelper = com.dmb.bestbefore.utils.AudioRecorderHelper(context)
        }
        audioRecorderHelper?.startRecording()
        _isRecordingAudio.value = true
    }

    fun stopAudioRecordingAndUpload(context: Context) {
        val file = audioRecorderHelper?.stopRecording()
        _isRecordingAudio.value = false
        
        if (file != null && file.exists()) {
            val currentRoomId = _selectedRoom.value?.id ?: return
            viewModelScope.launch {
                try {
                    val bytes = file.readBytes()
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val memoryData = mapOf(
                        "type" to "audio",
                        "title" to "Voice Memory",
                        "content" to base64,
                        "metadata" to emptyMap<String, Any>()
                    )
                    val result = roomRepository.addMemoryToRoom(currentRoomId, memoryData)
                    result.onSuccess {
                        // Use a custom scheme or parameters so the UI knows it's audio
                        val uploadedDataUri = "data:audio/mp4;base64,$base64"
                        val currentMedia = _roomMedia.value[currentRoomId] ?: emptyList()
                        _roomMedia.value = _roomMedia.value + (currentRoomId to (currentMedia + Uri.parse(uploadedDataUri)))
                        _totalMemories.value += 1
                        
                        val newActivity = RecentActivity(
                            type = ActivityType.ADDED_NOTE, 
                            title = "Added a voice memory to \"${_selectedRoom.value?.roomName}\"",
                            date = System.currentTimeMillis()
                        )
                        _recentActivities.value = listOf(newActivity) + _recentActivities.value

                        Toast.makeText(context, "Voice memory uploaded!", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, "Failed to upload voice memory", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Audio upload failed", e)
                    Toast.makeText(context, "Error processing audio", Toast.LENGTH_SHORT).show()
                } finally {
                    file.delete()
                }
            }
        }
    }

    private var mediaPlayer: android.media.MediaPlayer? = null

    fun playBase64Audio(context: Context, dataUri: String) {
        try {
            mediaPlayer?.release()

            val base64String = dataUri.substringAfter("base64,")
            val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)

            val extension = when {
                dataUri.startsWith("data:audio/mpeg") -> ".mp3"
                dataUri.startsWith("data:audio/wav") -> ".wav"
                dataUri.startsWith("data:audio/ogg") -> ".ogg"
                else -> ".m4a"
            }
            val tempFile = java.io.File.createTempFile("playing_audio", extension, context.cacheDir)
            tempFile.writeBytes(decodedBytes)

            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to play audio", e)
            Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
        }
    }

    fun playAudio(context: Context, source: String) {
        if (source.startsWith("data:audio")) {
            playBase64Audio(context, source)
            return
        }
        try {
            mediaPlayer?.release()
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(context, Uri.parse(source))
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to play uri audio", e)
            Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
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
                closeOverlay()
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
        _invitedUsers.value = emptyList()
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

    /** Opens a room from the Hallway card stack. Finds the matching TimeCapsuleRoom from createdRooms
     *  or creates a lightweight placeholder so the detail screen can load memories from the backend. */
    fun selectRoomFromHallway(card: com.dmb.bestbefore.data.models.HallwayCard) {
        val existing = _createdRooms.value.find { it.id == card.id }
        val room = existing ?: TimeCapsuleRoom(
            id = card.id,
            roomName = card.title,
            capsuleDays = card.timeCapsuleDays,
            capsuleHours = 0,
            capsuleMinutes = 0,
            notificationDays = 0,
            notificationHours = 0,
            isPublic = true,
            description = card.description,
            photos = card.photos,
            theme = "Default",
            tags = card.tags,
            music = card.backgroundMusic ?: "None",
            isViewerOnly = card.isViewerOnly,
            isOwnedByMe = card.isOwnedByMe,
            isCollaborator = card.isCollaborator,
            ownerUserType = card.ownerUserType
        )
        selectRoom(room)
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
    fun updateScheduledClosure(enabled: Boolean) { _scheduledClosureEnabled.value = enabled }
    fun updateScheduledClosureTime(millis: Long) { _scheduledClosureTime.value = millis }
    fun updateScheduledClosureHour(h: Int) {
        _scheduledClosureHour.value = h
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = _scheduledClosureTime.value
        cal.set(java.util.Calendar.HOUR_OF_DAY, h)
        _scheduledClosureTime.value = cal.timeInMillis
    }
    fun updateScheduledClosureMinute(m: Int) {
        _scheduledClosureMinute.value = m
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = _scheduledClosureTime.value
        cal.set(java.util.Calendar.MINUTE, m)
        _scheduledClosureTime.value = cal.timeInMillis
    }
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
    fun addInvitedUser(user: InvitedUser) {
        if (!_invitedUsers.value.any { it.email == user.email }) {
            _invitedUsers.value = _invitedUsers.value + user
        }
    }
    fun removeInvitedUser(email: String) {
        _invitedUsers.value = _invitedUsers.value.filter { it.email != email }
    }
    fun updateInvitedUserRole(email: String, role: String) {
        _invitedUsers.value = _invitedUsers.value.map {
            if (it.email == email) it.copy(role = role) else it
        }
    }

    // Live search state (UI-friendly model)
    private val _userSearchResults = MutableStateFlow<List<InvitedUser>>(emptyList())
    val userSearchResults: StateFlow<List<InvitedUser>> = _userSearchResults.asStateFlow()

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _userSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val result = roomRepository.searchUsers(query)
            if (result.isSuccess) {
                _userSearchResults.value = result.getOrNull().orEmpty().map { dto ->
                    InvitedUser(email = dto.email, name = dto.name)
                }
            } else {
                _userSearchResults.value = emptyList()
            }
        }
    }
    
    fun selectRoomForEditing(room: TimeCapsuleRoom) {
        _selectedRoom.value = room
        // Populate all wizard state from the room so Edit Room shows current values
        _roomName.value = room.roomName
        _isPublic.value = room.isPublic
        _isTimeCapsuleEnabled.value = room.isCollaboration  // isCollaboration stores isTimeCapsule
        _roomAtmosphereTheme.value = room.theme
        _selectedMusic.value = room.music
        _rollingExpiration.value = room.rollingExpiration
        _scheduledClosureEnabled.value = room.scheduledClosureTime > 0
        _scheduledClosureTime.value = if (room.scheduledClosureTime > 0) room.scheduledClosureTime
            else System.currentTimeMillis() + 7 * 86400000L
        _roomTags.value = room.tags
        _roomDescription.value = room.description ?: ""
        // Time capsule duration restored from local model
        _capsuleDays.value = room.capsuleDays
        _capsuleHours.value = room.capsuleHours
        _capsuleMins.value = room.capsuleMinutes
        _currentStep.value = ProfileStep.EDIT_ROOM
    }

    fun updateCollaboration(enable: Boolean) {
        _isCollaboration.value = enable
    }
    
    fun updateProfileImage(uri: Uri, context: Context) {
        val localUri = copyPickedImageToAppStorage(context, uri)
        if (localUri != null) {
            _profileImageUri.value = localUri
            val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
            sessionManager.saveProfilePhotoUri(localUri.toString())
            Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Photo could not be updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyPickedImageToAppStorage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            input.use { stream ->
                val mimeType = context.contentResolver.getType(sourceUri)
                val extension = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val targetFile = File(context.filesDir, "profile_photo.$extension")
                targetFile.outputStream().use { output ->
                    stream.copyTo(output)
                }
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to copy selected profile image", e)
            null
        }
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

    
    // Keep room — feature removed (no keepRoom endpoint in backend)
    fun keepRoom(context: Context, room: TimeCapsuleRoom) {
        dismissUnlockDialog()
        Toast.makeText(context, "Room \"${room.roomName}\" kept.", Toast.LENGTH_SHORT).show()
    }
    
    // Delete room from backend and local list
    // fromInsideRoom=true  → navigate back to hallway (closeOverlay)
    // fromInsideRoom=false → stay in current list view, just remove from list
    fun deleteRoom(context: Context, room: TimeCapsuleRoom, fromInsideRoom: Boolean = true) {
        viewModelScope.launch {
            try {
                roomRepository?.deleteRoom(room.id)
                _createdRooms.value = _createdRooms.value.filter { it.id != room.id }
                _roomMedia.value = _roomMedia.value.filterKeys { it != room.id }
                
                // Refresh top stats after local deletion
                _totalRooms.value = _createdRooms.value.size
                _totalMemories.value = _createdRooms.value.sumOf { r -> _roomMedia.value[r.id]?.size ?: 0 }
                
                dismissUnlockDialog()
                if (fromInsideRoom) {
                    _currentStep.value = ProfileStep.PROFILE_MENU
                    _selectedRoom.value = null
                }
                Toast.makeText(context, "Room \"${room.roomName}\" deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error deleting room", e)
                Toast.makeText(context, "Failed to delete room", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Save current VM state back to the backend for an existing room (Edit Room). */
    fun saveRoomEdits(context: Context) {
        val room = _selectedRoom.value ?: return
        val closureIso: String? = if (_scheduledClosureEnabled.value) {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(_scheduledClosureTime.value))
        } else null

        val fields: Map<String, Any?> = mapOf(
            "name" to _roomName.value,
            "isPrivate" to !_isPublic.value,
            "isTimeCapsule" to _isTimeCapsuleEnabled.value,
            "capsuleDurationDays" to _capsuleDays.value,
            "capsuleDurationHours" to _capsuleHours.value,
            "capsuleDurationMinutes" to _capsuleMins.value,
            "theme" to _roomAtmosphereTheme.value,
            "music" to _selectedMusic.value,
            "rollingExpiration" to _rollingExpiration.value,
            "scheduledClosureTime" to closureIso,
            "description" to _roomDescription.value.ifBlank { null },
            "tags" to _roomTags.value
        ).filterValues { it != null }

        @Suppress("UNCHECKED_CAST")
        viewModelScope.launch {
            val result = roomRepository?.updateRoom(room.id, fields as Map<String, Any>)
            if (result?.isSuccess == true) {
                // Update local list
                val updatedRoom = room.copy(
                    roomName = _roomName.value,
                    isPublic = _isPublic.value,
                    isCollaboration = _isTimeCapsuleEnabled.value,
                    capsuleDays = _capsuleDays.value,
                    capsuleHours = _capsuleHours.value,
                    capsuleMinutes = _capsuleMins.value,
                    theme = _roomAtmosphereTheme.value,
                    music = _selectedMusic.value,
                    rollingExpiration = _rollingExpiration.value,
                    scheduledClosureTime = if (_scheduledClosureEnabled.value) _scheduledClosureTime.value else 0L,
                    description = _roomDescription.value.ifBlank { null },
                    tags = _roomTags.value
                )
                _createdRooms.value = _createdRooms.value.map { if (it.id == room.id) updatedRoom else it }
                _selectedRoom.value = updatedRoom
                Toast.makeText(context, "Room saved!", Toast.LENGTH_SHORT).show()
                goBack()
            } else {
                Toast.makeText(context, "Failed to save room", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ========== PROFILE CUSTOMIZATION ====================

    fun updateUserName(name: String) {
        _userName.value = name
    }

    fun updateBio(newBio: String) {
        _bio.value = newBio
    }

    fun saveCustomization(context: Context) {
        viewModelScope.launch {
            try {
                _isUpdatingCredential.value = true
                val authRepo = com.dmb.bestbefore.data.repository.AuthRepository(context)
                val firebaseToken = authRepo.getFirebaseIdToken(false)
                if (firebaseToken != null) {
                    val rawProfileImageUri = _profileImageUri.value?.toString()
                    val profileImageUrlForBackend = if (
                        rawProfileImageUri != null &&
                        (rawProfileImageUri.startsWith("http://") ||
                            rawProfileImageUri.startsWith("https://") ||
                            rawProfileImageUri.startsWith("data:image"))
                    ) rawProfileImageUri else null
                    val profileImageBase64 = _profileImageUri.value?.let { uri ->
                        encodeProfileImageBase64(context, uri)
                    }
                    val updateResult = authRepo.updateMe(
                        com.dmb.bestbefore.data.api.models.UpdateMeRequest(
                            name = _userName.value,
                            bio = _bio.value,
                            profileImageUrl = profileImageUrlForBackend,
                            profileImageBase64 = profileImageBase64,
                            tags = _profileTags.value.ifEmpty { null },
                            theme = _selectedTheme.value.name,
                            accentColor = colorToHex(_accentColor.value)
                        )
                    )
                    if (updateResult.isSuccess) {
                        Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error saving profile", e)
                Toast.makeText(context, "Error updating profile", Toast.LENGTH_SHORT).show()
            } finally {
                _isUpdatingCredential.value = false
            }
        }
    }

    private fun encodeProfileImageBase64(context: Context, uri: Uri): String? {
        return runCatching {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val raw = stream.use { it.readBytes() }
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.size, options)
            var inSampleSize = 1
            while (options.outWidth / inSampleSize > 512 || options.outHeight / inSampleSize > 512) {
                inSampleSize *= 2
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.size, options) ?: return null
            val bos = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, bos)
            android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun colorToHex(color: Color): String {
        return String.format(
            "#%06X",
            0xFFFFFF and android.graphics.Color.rgb(
                (color.red * 255f).toInt().coerceIn(0, 255),
                (color.green * 255f).toInt().coerceIn(0, 255),
                (color.blue * 255f).toInt().coerceIn(0, 255)
            )
        )
    }

    // ========== THEME & CUSTOMIZATION FUNCTIONS ==========
    
    fun loadThemePreferences(context: Context) {
        com.dmb.bestbefore.ui.theme.ThemeState.init(context)
        _selectedTheme.value = com.dmb.bestbefore.ui.theme.ThemeState.currentTheme
        _accentColor.value = com.dmb.bestbefore.ui.theme.ThemeState.currentAccent
        _applyAccentToAll.value = com.dmb.bestbefore.ui.theme.ThemeState.applyAccentToAll
        _syncAccentWithRoom.value = com.dmb.bestbefore.ui.theme.ThemeState.syncAccentWithRoom
    }
    
    fun selectTheme(context: Context, theme: AppTheme) {
        com.dmb.bestbefore.ui.theme.ThemeState.selectTheme(context, theme)
        _selectedTheme.value = theme
    }
    
    fun selectAccentColor(context: Context, color: Color) {
        com.dmb.bestbefore.ui.theme.ThemeState.selectAccent(context, color)
        _accentColor.value = color
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
                        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                // Update email in backend MongoDB via PATCH /auth/me
                                viewModelScope.launch {
                                    try {
                                        val authRepo = com.dmb.bestbefore.data.repository.AuthRepository(context)
                                        val firebaseToken = authRepo.getFirebaseIdToken(true) // force refresh after email change
                                        if (firebaseToken != null) {
                                            val updateResult = authRepo.updateMe(
                                                com.dmb.bestbefore.data.api.models.UpdateMeRequest(email = newEmail)
                                            )
                                            if (updateResult.isSuccess) {
                                                _credentialUpdateSuccess.value = "Verification email sent to $newEmail. Please verify to complete the change."
                                                val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
                                                sessionManager.saveUserEmail(newEmail)
                                            } else {
                                                _credentialUpdateError.value = "Backend update failed"
                                            }
                                        } else {
                                            _credentialUpdateError.value = "Auth token unavailable"
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
                                // Password is managed entirely in Firebase — no backend call needed
                                viewModelScope.launch {
                                    _credentialUpdateSuccess.value = "Password updated successfully!"
                                    _isUpdatingCredential.value = false
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
        _roomEmotions.value = emptyMap()
    }

    fun setArtistRoomEmotion(context: Context, roomId: String, emotion: String?) {
        if (emotion != null && emotion !in ARTIST_ROOM_EMOTIONS) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val current = _roomEmotions.value.toMutableMap()
        if (emotion == null) current.remove(roomId) else current[roomId] = emotion
        _roomEmotions.value = current
        com.dmb.bestbefore.data.local.SessionManager(context).saveRoomEmotions(uid, current)

        // Best-effort backend signal so artists can later aggregate emotional responses.
        if (emotion != null) {
            viewModelScope.launch {
                roomRepository.trackInteraction(
                    roomId = roomId,
                    dwellTimeSeconds = 0,
                    type = "EMOTION_${emotion.uppercase()}",
                    lat = null,
                    lon = null
                )
            }
        }
    }

    suspend fun getAuthToken(context: Context): String? {
        val authRepo = com.dmb.bestbefore.data.repository.AuthRepository(context)
        return authRepo.getFirebaseIdToken(false)
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
    CAMERA_ACTION,
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
