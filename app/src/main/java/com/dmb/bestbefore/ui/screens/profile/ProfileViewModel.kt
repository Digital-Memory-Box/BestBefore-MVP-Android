package com.dmb.bestbefore.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.models.TimeCapsuleRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.util.Base64
import android.widget.Toast
import android.util.Log
import java.io.File

import com.dmb.bestbefore.data.models.CalendarEvent
import com.dmb.bestbefore.data.models.MemoryItem
import com.dmb.bestbefore.ui.theme.AppTheme
import com.dmb.bestbefore.ui.theme.AppThemes
import com.dmb.bestbefore.data.local.PreferencesManager
import androidx.compose.ui.graphics.Color
import com.dmb.bestbefore.CalendarHelper
import com.dmb.bestbefore.data.ai.AiRepository
import com.dmb.bestbefore.data.ai.AiRoomSuggestion
import com.dmb.bestbefore.data.ai.UpdatePreferenceResponse
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.MemoryPreview
import com.dmb.bestbefore.data.api.models.RoomDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.dmb.bestbefore.data.api.models.UserDto
import com.dmb.bestbefore.data.api.models.UpdateMeRequest
import com.dmb.bestbefore.data.api.models.RoomSuggestionDto
import com.dmb.bestbefore.data.api.models.RoomSuggestionsResponse
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.data.models.AppNotification
import com.dmb.bestbefore.data.models.HallwayCard
import com.dmb.bestbefore.data.models.NotificationType
import com.dmb.bestbefore.data.repository.AuthRepository
import com.dmb.bestbefore.data.repository.NotificationRepository
import com.dmb.bestbefore.data.repository.RoomRepository
import com.dmb.bestbefore.notifications.NotificationScheduler
import com.dmb.bestbefore.ui.theme.ThemeState
import com.dmb.bestbefore.utils.AudioRecorderHelper
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.UUID

class ProfileViewModel : ViewModel() {
    companion object {
        val ARTIST_ROOM_EMOTIONS = listOf("warmed", "moved", "soothed", "struck", "stayed")
    }

    private val _currentStep = MutableStateFlow(ProfileStep.NONE)
    val currentStep: StateFlow<ProfileStep> = _currentStep.asStateFlow()

    // Single ISO-8601 parser — handles both "Z" suffix and "+00:00" offset returned by MongoDB.
    // The old SimpleDateFormat("...'Z'") treated Z as a literal character so "+00:00" dates
    // always failed to parse, causing every unlock/closure time to be wrong.
    private fun parseIso8601(dateString: String?): Long {
        if (dateString == null) return 0L
        return try {
            OffsetDateTime.parse(dateString).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                ZonedDateTime.parse(dateString).toInstant().toEpochMilli()
            } catch (__: Exception) { 0L }
        }
    }

    private fun parseCreatedAt(dateString: String?): Long {
        val ms = parseIso8601(dateString)
        return if (ms > 0L) ms else System.currentTimeMillis()
    }

    // RoomRepository — no token arg; fetches fresh Firebase token per request (matches iOS pattern)
    private val roomRepository = RoomRepository()
    private val notificationRepository = NotificationRepository()
    // Initialised in initDatabase(context) so we can persist preference updates from AI responses.
    private var authRepository: AuthRepository? = null

    // ── AI Service integration ────────────────────────────────────────────────
    private val aiRepository = AiRepository()

    /** Last UserDto fetched from the backend – used to supply preference context to AI calls. */
    private var _cachedUserDto: UserDto? = null

    /** AI-powered personalised room suggestions (sorted by combined similarity). */
    private val _aiSuggestions = MutableStateFlow<List<AiRoomSuggestion>>(emptyList())
    val aiSuggestions: StateFlow<List<AiRoomSuggestion>> = _aiSuggestions.asStateFlow()

    private val _isLoadingAiSuggestions = MutableStateFlow(false)
    val isLoadingAiSuggestions: StateFlow<Boolean> = _isLoadingAiSuggestions.asStateFlow()

    /** AI-generated description text for the room creation wizard. */
    private val _aiGeneratedDescription = MutableStateFlow<String?>(null)
    val aiGeneratedDescription: StateFlow<String?> = _aiGeneratedDescription.asStateFlow()

    // ── Connect Rooms ─────────────────────────────────────────────────────────
    private val _showConnectRooms = MutableStateFlow(false)
    val showConnectRooms: StateFlow<Boolean> = _showConnectRooms.asStateFlow()

    private val _connectionSuggestions = MutableStateFlow<List<RoomSuggestionDto>>(emptyList())
    val connectionSuggestions: StateFlow<List<RoomSuggestionDto>> = _connectionSuggestions.asStateFlow()

    private val _isLoadingConnectionSuggestions = MutableStateFlow(false)
    val isLoadingConnectionSuggestions: StateFlow<Boolean> = _isLoadingConnectionSuggestions.asStateFlow()
    // ─────────────────────────────────────────────────────────────────────────
    
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()
    
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

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private var audioRecorderHelper: AudioRecorderHelper? = null
    
    private var creationSource: RoomCreationSource = RoomCreationSource.HALLWAY
    // Remembers which step was active before navigating into ROOM_DETAIL so goBack()
    // can return there instead of always resetting to NONE (Hallway).
    private var previousStepBeforeRoomDetail: ProfileStep = ProfileStep.NONE



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

    private val _profileImageUri = MutableStateFlow<Any?>(null)
    val profileImageUri: StateFlow<Any?> = _profileImageUri.asStateFlow()

    private val _roomEmotions = MutableStateFlow<Map<String, String>>(emptyMap())
    val roomEmotions: StateFlow<Map<String, String>> = _roomEmotions.asStateFlow()

    
    private val _userName = MutableStateFlow("User") 
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio.asStateFlow()

    // ── Preferred Tags (for recommendation) ──────────────────────────
    private val _preferredTags = MutableStateFlow<List<String>>(emptyList())
    val preferredTags: StateFlow<List<String>> = _preferredTags.asStateFlow()

    fun addProfileTag(tag: String, context: Context? = null) {
        val trimmed = tag.trim().trimStart('#').lowercase(Locale.US)
        if (trimmed.isNotEmpty() && _preferredTags.value.none { it.equals(trimmed, ignoreCase = true) }) {
            val updated = _preferredTags.value + trimmed
            _preferredTags.value = updated
            context?.let { SessionManager(it).saveManualProfileTags(updated) }
        }
    }

    fun removeProfileTag(tag: String, context: Context? = null) {
        val normalized = tag.trim().trimStart('#')
        val updated = _preferredTags.value.filter { !it.equals(normalized, ignoreCase = true) }
        _preferredTags.value = updated
        context?.let { SessionManager(it).saveManualProfileTags(updated) }
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
        ThemeState.updateApplyAccentToAll(enabled)
    }

    fun toggleSyncAccent(context: Context, enabled: Boolean) {
        _syncAccentWithRoom.value = enabled
        ThemeState.updateSyncAccentWithRoom(enabled)
    }

    // Helper for resolving connected rooms names
    suspend fun getRoomByIdFromRemote(id: String): TimeCapsuleRoom? {
        // First check in created rooms
        _createdRooms.value.find { it.id == id }?.let { return it }
        
        return try {
            val result = roomRepository.getRoomById(id)
            result.getOrNull()?.let { dto ->
                val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val isOwner = dto.ownerEmail?.equals(myEmail, ignoreCase = true) == true
                val isCollab = isCollaborator(dto, myEmail)
                val isView = isViewer(dto, myEmail)
                
                TimeCapsuleRoom(
                    id = dto.id,
                    roomName = dto.name,
                    capsuleDays = dto.capsuleDurationDays,
                    capsuleHours = dto.capsuleDurationHours,
                    capsuleMinutes = dto.capsuleDurationMinutes,
                    notificationDays = 0,
                    notificationHours = 0,
                    isPublic = dto.isPublic ?: !dto.isPrivate,
                    isCollaboration = dto.isTimeCapsule,
                    photos = dto.photos ?: emptyList(),
                    unlockTime = dto.unlockDate?.let { parseISO8601(it) } ?: 0L,
                    scheduledClosureTime = dto.expirationDate?.let { parseISO8601(it) } ?: 0L,
                    theme = dto.theme ?: "Default",
                    tags = dto.tags ?: emptyList(),
                    description = dto.description,
                    music = dto.backgroundMusic ?: "None",
                    connectedRooms = dto.connectedRooms ?: emptyList(),
                    isOwnedByMe = isOwner,
                    isCollaborator = isCollab,
                    isViewerOnly = !isOwner && !isCollab && (isView || !dto.isPrivate),
                    ownerUserType = dto.ownerUserType
                )
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to fetch remote room $id", e)
            null
        }
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
    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    fun loadCalendarEvents(context: Context) {
        _calendarEvents.value = CalendarHelper.getUpcomingEvents(context)
    }

    fun applyCalendarEvent(event: CalendarEvent) {
        _roomName.value = event.title
        _unlockMethod.value = UnlockMethod.SPECIFIC_DATE
        _targetTime.value = event.startTime.time
        
        val cal = Calendar.getInstance()
        cal.timeInMillis = event.startTime.time
        _targetHour.value = cal.get(Calendar.HOUR_OF_DAY)
        _targetMinute.value = cal.get(Calendar.MINUTE)
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

    // Upload Start Date — blocks uploads until this date
    private val _uploadStartDateEnabled = MutableStateFlow(false)
    val uploadStartDateEnabled: StateFlow<Boolean> = _uploadStartDateEnabled.asStateFlow()

    private val _uploadStartDate = MutableStateFlow(System.currentTimeMillis() + 86400000L)
    val uploadStartDate: StateFlow<Long> = _uploadStartDate.asStateFlow()

    private val _uploadStartHour = MutableStateFlow(0)
    val uploadStartHour: StateFlow<Int> = _uploadStartHour.asStateFlow()

    private val _uploadStartMinute = MutableStateFlow(0)
    val uploadStartMinute: StateFlow<Int> = _uploadStartMinute.asStateFlow()

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

    // Memory metadata map: roomId -> (uriString -> MemoryItem)
    // Populated from refreshRoomMemories. Allows ownership checks and deletion by URI.
    private val _roomMemoryItems = MutableStateFlow<Map<String, Map<String, MemoryItem>>>(emptyMap())

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
            val authRepo = AuthRepository(context)
            val sessionManager = SessionManager(context)
            val result = authRepo.updateMe(UpdateMeRequest(profileMusic = updateMusic))
            if (result.isSuccess) {
                _profileMusic.value = trackName ?: "None"
                sessionManager.saveProfileMusic(updateMusic)
                Toast.makeText(context, "Profile music updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to update profile music", Toast.LENGTH_SHORT).show()
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

    fun handleRespondToNotification(context: Context, notification: AppNotification, accept: Boolean) {
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
        if (authRepository == null) authRepository = AuthRepository(context)
        val sessionManager = SessionManager(context)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val savedName = sessionManager.getUserName()
        val savedMusic = sessionManager.getProfileMusic()
        val savedProfilePhotoUri = sessionManager.getProfilePhotoUri()
        val savedProfileImageUrl = sessionManager.getProfileImageUrl()
        _userName.value = if (!savedName.isNullOrEmpty()) savedName else "User"
        _profileMusic.value = if (!savedMusic.isNullOrEmpty()) savedMusic else "None"
        // Prefer the backend-persisted URL (survives app restarts); fall back to local file URI
        _profileImageUri.value = when {
            !savedProfileImageUrl.isNullOrBlank() -> savedProfileImageUrl // Keep as String for data: or http:
            !savedProfilePhotoUri.isNullOrBlank() -> Uri.parse(savedProfilePhotoUri)
            else -> null
        }
        _roomEmotions.value = sessionManager.getRoomEmotions(currentUserId)
        val savedBio = sessionManager.getBio()
        if (!savedBio.isNullOrEmpty()) _bio.value = savedBio

        viewModelScope.launch {
            try {
                val authRepo = AuthRepository(context)
                
                // Use coroutineScope for structured concurrency and proper async resolution
                coroutineScope {
                    val meDeferred = async { authRepo.getMe() }
                    val roomsDeferred = async { roomRepository.getRooms() }
                    val notificationsDeferred = async { fetchNotifications() }
                    val tagsDeferred = async { fetchTagsLocally(context) }
                    
                    val meResult: Result<UserDto> = meDeferred.await()
                    val roomsResult: Result<List<RoomDto>> = roomsDeferred.await()
                    notificationsDeferred.await()
                    val tagsList: List<String> = tagsDeferred.await()
                    _availableTags.value = tagsList

                    if (meResult.isSuccess) {
                        val userDto: UserDto = meResult.getOrThrow()
                        applyUserDtoToState(userDto, context)
                    }


                    val allRooms = mutableListOf<TimeCapsuleRoom>()
                    if (roomsResult.isSuccess) {
                        val apiRooms = roomsResult.getOrThrow()
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
                        
                        allRooms.addAll(mapDtosToRooms(myCreatedRoomsDtos, isSaved = false))
                        allRooms.addAll(mapDtosToRooms(myJoinedRoomsDtos, isSaved = false))
                    } else {
                        val e = roomsResult.exceptionOrNull()
                        Log.e("ProfileViewModel", "getRooms failed: ${e?.message}")
                    }

                    // Memories: Count from all rooms (owned + collaborated)
                    _totalRooms.value = allRooms.size
                    _totalMemories.value = allRooms.sumOf { it.photos.size }
                    refreshRoomLists(allRooms)

                    // AI: fetch personalised suggestions after rooms and user profile are loaded
                    val userForAi = _cachedUserDto
                    val allApiRooms = roomsResult.getOrNull() ?: emptyList()
                    if (userForAi != null && allApiRooms.isNotEmpty()) {
                        launch { fetchAiSuggestions(userForAi, allApiRooms) }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "initDatabase failed", e)
            }
        }}


    private fun applyUserDtoToState(userDto: UserDto, context: Context) {
        val sessionManager = SessionManager(context)
        val manualProfileTags = sessionManager.getManualProfileTags()
        val backendTags = userDto.preferredTags.orEmpty()
        val hasLearnedPreferenceTags = !userDto.preferenceTagWeights.isNullOrEmpty()
        val visibleProfileTags = when {
            manualProfileTags != null -> manualProfileTags
            backendTags.isNotEmpty() && !hasLearnedPreferenceTags -> backendTags
            else -> emptyList()
        }

        _cachedUserDto = userDto.copy(preferredTags = visibleProfileTags)  // cache for AI calls
        if (!userDto.name.isNullOrBlank()) _userName.value = userDto.name
        _bio.value = userDto.bio ?: ""
        
        // Allow both HTTP URLs and data: URIs (Base64) from backend.
        if (!userDto.profileImageUrl.isNullOrBlank()) {
            _profileImageUri.value = userDto.profileImageUrl
        } else if (!userDto.profileImageData.isNullOrBlank()) {
            // Fallback to raw base64 data if present
            _profileImageUri.value = "data:image/jpeg;base64,${userDto.profileImageData}"
        }
        
        _preferredTags.value = visibleProfileTags
        
        // Sync theme, accentColor
        if (!userDto.theme.isNullOrBlank()) {
            val serverTheme = AppThemes.getThemeByName(userDto.theme)
            _selectedTheme.value = serverTheme
            ThemeState.selectTheme(context, serverTheme)
        }
        if (!userDto.accentColor.isNullOrBlank()) {
            runCatching {
                val c = Color(android.graphics.Color.parseColor(userDto.accentColor))
                _accentColor.value = c
                ThemeState.selectAccent(context, c)
            }
        }

        if (!userDto.profileMusic.isNullOrBlank() && userDto.profileMusic != "None") {
            _profileMusic.value = userDto.profileMusic
        }
        
        // Save to cache via SessionManager (for updates)
        sessionManager.saveUser(userDto.copy(preferredTags = visibleProfileTags))
    }

    private suspend fun fetchTagsLocally(context: Context): List<String> {
        val parsedTags = mutableListOf<String>()
        try {
            val authRepo = AuthRepository(context)
            val token = authRepo.getFirebaseIdToken(false)
            if (token != null) {
                val tagsResponse = RetrofitClient.apiService.getTags("Bearer $token")
                if (tagsResponse.isSuccessful) {
                    val bodyElement = tagsResponse.body()
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
                            if (obj.has("tags") && obj.get("tags").isJsonArray) {
                                obj.get("tags").asJsonArray.forEach { 
                                    if (it.isJsonPrimitive) parsedTags.add(it.asString)
                                    else if (it.isJsonObject) {
                                        val t = it.asJsonObject.get("name")?.asString ?: it.asJsonObject.get("tag")?.asString
                                        if (t != null) parsedTags.add(t)
                                    }
                                }
                            } else {
                                // Handle category object { "category": ["tag1", "tag2"] }
                                obj.entrySet().forEach { entry ->
                                    if (entry.value.isJsonArray) {
                                        entry.value.asJsonArray.forEach { 
                                            if (it.isJsonPrimitive) parsedTags.add(it.asString)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to fetch tags", e)
        }
        return parsedTags
    }


    private fun mapDtosToRooms(dtos: List<RoomDto>, isSaved: Boolean): List<TimeCapsuleRoom> {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        return dtos.map { dto ->
            val createdMs = parseCreatedAt(dto.createdAt)
            val unlock = createdMs + (dto.capsuleDurationDays * 24 * 3600 * 1000L) +
                          (dto.capsuleDurationHours * 3600 * 1000L) + (dto.capsuleDurationMinutes * 60 * 1000L)
            val closureMs = dto.expirationDate?.let { parseCreatedAt(it) } ?: 0L
            val uploadStartMs = dto.uploadStartDate?.let { parseCreatedAt(it) } ?: 0L
            val rollingString = when (dto.rollingExpiryDays) {
                1 -> "1 Day (24...)"
                7 -> "7 Days"
                30 -> "30 Days"
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
                isPublic = dto.isPublic ?: !dto.isPrivate,
                isCollaboration = dto.isTimeCapsule,
                photos = dto.photos ?: emptyList(),
                unlockTime = if (dto.unlockDate != null) parseCreatedAt(dto.unlockDate) else unlock,
                scheduledClosureTime = closureMs,
                uploadStartDate = uploadStartMs,
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
            val media = _roomMedia.value[room.id]
            media?.size ?: room.photos.size 
        }
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

    fun finalizeRoom(context: Context? = null) {
        // Request notification permission before creating room
        onRequestNotificationPermission?.invoke()

        // Calculate duration depending on the unlock method
        val now = System.currentTimeMillis()
        val days: Int
        val hours: Int
        val minutes: Int
        val finalTargetTime: Long

        if (!_isTimeCapsuleEnabled.value) {
            days = 0
            hours = 0
            minutes = 0
            finalTargetTime = now
        } else if (_unlockMethod.value == UnlockMethod.DURATION) {
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

        viewModelScope.launch {
            // --- 1. AI KORSAN SIZINTISI BAŞLIYOR ---
            val name = _roomName.value
            val tags = _roomTags.value
            val isPrivateMode = !_isPublic.value
            val isTimeCap = _isTimeCapsuleEnabled.value
            var finalDescription = _roomDescription.value.trim()

            try {
                // Şairane yapay zekayı gizlice çağırıyoruz
                val aiResult = aiRepository.generateRoomDescription(
                    roomName = name,
                    tags = tags,
                    isPrivate = isPrivateMode,
                    isTimeCapsule = isTimeCap
                )

                aiResult.onSuccess { generatedText ->
                    if (finalDescription.isBlank()) {
                        finalDescription = generatedText
                    } else {
                        // Kullanıcı zaten bir şeyler yazmışsa, AI'ın metnini altına ekle
                        finalDescription = "$finalDescription\n\n$generatedText"
                    }
                    // UI state'i de güncelleyelim
                    _roomDescription.value = finalDescription
                    Log.d("ProfileViewModel", "AI Description başarıyla eklendi!")
                }.onFailure {
                    // Eğer AI sunucusunda bir anlık takılma olursa uygulama çökmesin, devam etsin
                    Log.e("ProfileViewModel", "AI otomatik oluşturulamadı: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "AI Call failed", e)
            }
            // --- AI KORSAN SIZINTISI BİTTİ ---

            // 2. ODA OBJESİNİ OLUŞTUR (Artık zenginleştirilmiş finalDescription ile)
            val newRoom = TimeCapsuleRoom(
                id = UUID.randomUUID().toString(),
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
                uploadStartDate = if (_uploadStartDateEnabled.value) _uploadStartDate.value else 0L,
                theme = _roomAtmosphereTheme.value,
                tags = _roomTags.value,
                description = finalDescription, // <-- SİHİR BURADA: AI Metni buraya giriyor
                music = _selectedMusic.value,
                rollingExpiration = _rollingExpiration.value,
                isOwnedByMe = true,
                isCollaborator = false
            )

            // Convert scheduledClosureTime millis -> ISO-8601 string for backend
            val closureIso: String? = if (_scheduledClosureEnabled.value) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(_scheduledClosureTime.value))
            } else null

            val uploadStartIso: String? = if (_uploadStartDateEnabled.value) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(_uploadStartDate.value))
            } else null

            val unlockIso: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(newRoom.unlockTime))

            val rollingDays = when (newRoom.rollingExpiration) {
                "1 Day (24...)" -> 1
                "7 Days" -> 7
                "30 Days" -> 30
                else -> 0
            }

            // 3. BACKEND'E KAYDET
            val result = roomRepository.createRoom(
                newRoom.roomName,
                newRoom.capsuleDays,
                newRoom.capsuleHours,
                newRoom.capsuleMinutes,
                newRoom.isPublic,
                newRoom.isCollaboration,
                newRoom.theme,
                collaborators = if (newRoom.isPublic) {
                    _invitedUsers.value.map { it.email }
                } else {
                    _invitedUsers.value.filter { it.role == "collaborator" }.map { it.email }
                },
                viewers = if (newRoom.isPublic) {
                    emptyList()
                } else {
                    _invitedUsers.value.filter { it.role == "viewer" }.map { it.email }
                },
                scheduledClosureIso = closureIso,
                uploadStartDateIso = uploadStartIso,
                unlockDateIso = unlockIso,
                rollingExpiryDays = rollingDays,
                description = newRoom.description, // Artık AI metnini de içeriyor!
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

            if (result.isSuccess && !newRoom.isPublic) {
                _invitedUsers.value.forEach { invited ->
                    roomRepository.createHandshakeInvite(finalRoom.id, invited.email, invited.role)
                        .onFailure { e ->
                            Log.w("ProfileViewModel", "Failed to send invite to ${invited.email}: ${e.message}")
                        }
                }
            }

            // Add to local list
            val updatedList = _createdRooms.value + finalRoom
            refreshRoomLists(updatedList)

            // Keep user in flow by navigating to the new room detail
            selectRoom(finalRoom)

            context?.let { ctx ->
                NotificationRepository(ctx).addNotification(
                    AppNotification(
                        title = "Room Created",
                        message = "You successfully created the room \"${finalRoom.roomName}\"",
                        type = NotificationType.ROOM_CREATED,
                        relatedRoomId = finalRoom.id,
                        relatedRoomName = finalRoom.roomName
                    )
                )
            }
        }

        // Schedule notification (Fixed: Added back)
        context?.let { ctx ->
            val unlockTimeMillis = _targetTime.value
            NotificationScheduler.scheduleRoomUnlockNotification(
                ctx,
                _roomName.value.hashCode().toString(),
                _roomName.value,
                unlockTimeMillis
            )
        }
    }
    
    fun sortRoomsByDateCreated() {
        _createdRooms.value = _createdRooms.value.sortedByDescending { it.dateCreated }
    }

    // ── AI Service Functions ──────────────────────────────────────────────────

    /**
     * Fetch personalised room suggestions from the AI service.
     * Called automatically after [initDatabase] finishes loading rooms.
     * Results are exposed via [aiSuggestions].
     */
    private suspend fun fetchAiSuggestions(
        user: UserDto,
        candidateRooms: List<RoomDto>
    ) {
        _isLoadingAiSuggestions.value = true
        try {
            aiRepository.getPersonalisedSuggestions(
                user = user,
                candidateRooms = candidateRooms
            ).onSuccess { response ->
                _aiSuggestions.value = response.suggestions
                Log.d("ProfileViewModel", "AI returned ${response.count} suggestions")
            }.onFailure { e ->
                Log.w("ProfileViewModel", "AI suggestions failed: ${e.message}")
            }
        } finally {
            _isLoadingAiSuggestions.value = false
        }
    }

    /**
     * Track a LIKE interaction for the currently selected room.
     * Call this when the user taps the "Like" / heart button on a room.
     */
    fun trackLikeForCurrentRoom() {
        val room = _selectedRoom.value ?: return
        val userDto = _cachedUserDto ?: return
        viewModelScope.launch {
            val candidateDto = RoomDto(
                id = room.id,
                name = room.roomName,
                ownerEmail = null,
                createdAt = null,
                isPrivate = !room.isPublic,
                isTimeCapsule = room.isCollaboration,
                tags = room.tags
            )
            aiRepository.trackRoomInteraction(
                user = userDto,
                room = candidateDto,
                interactionType = "LIKE"
            ).onSuccess { updatedPrefs ->
                Log.d("ProfileViewModel", "AI LIKE tracked. Top tags: ${updatedPrefs.preferredTags.take(5)}")
                persistPreferenceUpdate(updatedPrefs)
            }
        }
    }

    /**
     * Track a VIEW interaction for [room] (called when a room detail is opened).
     * Fire-and-forget: persists preference signals without blocking the UI.
     */
    fun trackViewForRoom(room: TimeCapsuleRoom) {
        val userDto = _cachedUserDto ?: return
        viewModelScope.launch {
            val candidateDto = RoomDto(
                id = room.id, name = room.roomName, ownerEmail = null, createdAt = null,
                isPrivate = !room.isPublic, isTimeCapsule = room.isCollaboration, tags = room.tags
            )
            aiRepository.trackRoomInteraction(userDto, candidateDto, "VIEW")
                .onSuccess { persistPreferenceUpdate(it) }
        }
    }

    /**
     * Ask the AI service to generate a description for a room that has none.
     * Triggered automatically when the room detail is opened (generative_service.py backend).
     * Saves the result via PATCH /rooms/{id} for owned rooms so it persists.
     * For collaborator/viewer rooms, updates local display state only.
     */
    private fun generateAndSaveRoomDescription(room: TimeCapsuleRoom) {
        viewModelScope.launch {
            aiRepository.generateRoomDescription(
                roomName = room.roomName,
                tags = room.tags,
                isPrivate = !room.isPublic,
                isTimeCapsule = room.isCollaboration
            ).onSuccess { description ->
                if (description.isBlank()) return@onSuccess
                // Update local state immediately so the UI shows it right away
                val updated = room.copy(description = description)
                if (_selectedRoom.value?.id == room.id) _selectedRoom.value = updated
                _createdRooms.value = _createdRooms.value.map { if (it.id == room.id) updated else it }
                // Persist for owned rooms so it survives app restarts
                if (room.isOwnedByMe) {
                    roomRepository.updateRoom(room.id, mapOf("generatedDescription" to description))
                        .onSuccess { Log.d("ProfileViewModel", "AI desc saved for '${room.roomName}'") }
                        .onFailure { e -> Log.w("ProfileViewModel", "AI desc save failed: ${e.message}") }
                }
            }.onFailure { e ->
                Log.w("ProfileViewModel", "AI description generation failed for '${room.roomName}': ${e.message}")
            }
        }
    }

    /**
     * Save the AI-returned preference snapshot back to MongoDB via PATCH /me.
     * This is what keeps Android's preference data in sync with iOS (same document schema).
     *
     * Fields written: preferenceTagWeights, preferenceRoomTypes,
     * preferenceEmbedding, preferenceUpdatedAt, lastLat, lastLon.
     */
    private fun persistPreferenceUpdate(prefs: UpdatePreferenceResponse) {
        val repo = authRepository ?: return
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .also { it.timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date())
            repo.updateMe(
                UpdateMeRequest(
                    preferenceTagWeights = prefs.preferenceTagWeights,
                    preferenceRoomTypes = prefs.preferenceRoomTypes,
                    preferenceEmbedding = prefs.preferenceEmbedding,
                    preferenceUpdatedAt = now,
                    lastLat = prefs.lastLat,
                    lastLon = prefs.lastLon
                )
            ).onSuccess { updatedUser ->
                _cachedUserDto = updatedUser.copy(preferredTags = _preferredTags.value)
                Log.d("ProfileViewModel", "Preferences persisted → ${updatedUser.preferenceTagWeights?.keys?.take(5)}")
            }.onFailure { e ->
                Log.w("ProfileViewModel", "Failed to persist preference update: ${e.message}")
            }
        }
    }

    /**
     * Generate an AI description for the room currently being created in the wizard.
     * Result is stored in [aiGeneratedDescription] and can be auto-filled into the description field.
     */
    fun generateRoomDescriptionWithAi() {
        viewModelScope.launch {
            val name = _roomName.value.ifBlank { return@launch }
            aiRepository.generateRoomDescription(
                roomName = name,
                tags = _roomTags.value,
                isPrivate = !_isPublic.value,
                isTimeCapsule = _isTimeCapsuleEnabled.value
            ).onSuccess { description ->
                _aiGeneratedDescription.value = description
                // Auto-fill if the description field is still empty
                if (_roomDescription.value.isBlank()) {
                    _roomDescription.value = description
                }
            }.onFailure { e ->
                Log.w("ProfileViewModel", "AI description generation failed: ${e.message}")
            }
        }
    }

    /** Clear the AI-generated description (e.g. when wizard is reset). */
    fun clearAiGeneratedDescription() {
        _aiGeneratedDescription.value = null
    }

    // ─────────────────────────────────────────────────────────────────────────

    // Media Persistence (Room ID -> List of Uris)

    fun uploadNote(context: Context, noteContent: String) {
        val currentRoomId = _selectedRoom.value?.id ?: return
        
        // Text size limit: 5MB
        val bytesSize = noteContent.toByteArray().size
        if (bytesSize > 5 * 1024 * 1024) {
            Toast.makeText(context, "Note exceeds 5MB limit", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModelScope.launch {
            try {
                _isUploading.value = true
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

                    refreshRoomMemories(showRefreshIndicator = false)
                    Toast.makeText(context, "Note saved!", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Failed to save note", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to upload note", e)
                Toast.makeText(context, "Error saving note", Toast.LENGTH_SHORT).show()
            } finally {
                _isUploading.value = false
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
                _isUploading.value = true
                val uploadedDataUris = mutableListOf<String>()

                currentSelection.forEach { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri) ?: return@forEach
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        
                        val mimeType = (context.contentResolver.getType(uri) ?: "").lowercase()
                        
                        // Check limits based on requested bounds
                        val byteSize = bytes.size
                        if (mimeType.startsWith("audio/") && byteSize > 15 * 1024 * 1024) {
                            withContext(Dispatchers.Main) { Toast.makeText(context, "Audio exceeds 15MB limit", Toast.LENGTH_SHORT).show() }
                            return@forEach
                        } else if (mimeType.startsWith("video/") && byteSize > 100 * 1024 * 1024) {
                            withContext(Dispatchers.Main) { Toast.makeText(context, "Video exceeds 100MB limit", Toast.LENGTH_SHORT).show() }
                            return@forEach
                        } else if (mimeType.startsWith("image/") && byteSize > 25 * 1024 * 1024) {
                            withContext(Dispatchers.Main) { Toast.makeText(context, "Photo exceeds 25MB limit", Toast.LENGTH_SHORT).show() }
                            return@forEach
                        }

                        if (mimeType.startsWith("audio/")) {
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
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
                        } else if (mimeType.startsWith("video/")) {
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            val memoryData: Map<String, Any> = mapOf(
                                "type" to "video",
                                "title" to "Video Drop",
                                "content" to base64,
                                "metadata" to mapOf("mimeType" to mimeType)
                            )
                            val result = roomRepository.addMemoryToRoom(currentRoomId, memoryData)
                            result.onSuccess {
                                uploadedDataUris.add("data:$mimeType;base64,$base64")
                            }
                        } else {
                            // Downsample image payloads for safer upload and render.
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                            var inSampleSize = 1
                            while (options.outWidth / inSampleSize > 1024 || options.outHeight / inSampleSize > 1024) {
                                inSampleSize *= 2
                            }

                            options.inJustDecodeBounds = false
                            options.inSampleSize = inSampleSize
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                            if (bitmap == null) return@forEach

                            val bos = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bos)
                            val compressed = bos.toByteArray()
                            val base64 = Base64.encodeToString(compressed, Base64.NO_WRAP)

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
                        title = "Added ${uploadedDataUris.size} media file(s) to \"${_selectedRoom.value?.roomName}\"",
                        date = System.currentTimeMillis()
                    )
                    _recentActivities.value = listOf(newActivity) + _recentActivities.value

                    refreshRoomMemories(showRefreshIndicator = false)
                    Toast.makeText(context, "Uploaded ${uploadedDataUris.size} media file(s)!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload failed – check connection", Toast.LENGTH_SHORT).show()
                }
                
                _isUploading.value = false
            }
        }
    }

    // Refresh memories for the current room from the backend (pull-to-refresh)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshRoomMemories(showRefreshIndicator: Boolean = true, limit: Int? = 12) {
        val currentRoomId = _selectedRoom.value?.id ?: return
        viewModelScope.launch {
            if (showRefreshIndicator) _isRefreshing.value = true
            try {
                // Pre-populate with the room's known photos from the DTO so the UI shows
                // something immediately (and as a fallback if the API returns empty for
                // viewer-only rooms or due to a network issue).
                val currentRoom = _selectedRoom.value?.takeIf { it.id == currentRoomId }
                if (currentRoom != null && currentRoom.photos.isNotEmpty() && _roomMedia.value[currentRoomId].isNullOrEmpty()) {
                    val previewUris = currentRoom.photos.mapNotNull { preview ->
                        preview.url.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    }
                    if (previewUris.isNotEmpty()) {
                        _roomMedia.value = _roomMedia.value + (currentRoomId to previewUris)
                    }
                }

                val memoriesUrls = mutableListOf<String>()
                val memoryItemMap = mutableMapOf<String, MemoryItem>()
                val memoriesResult = roomRepository.getMemoriesByRoom(currentRoomId, limit = limit)
                memoriesResult.onSuccess { memories ->
                    memories.forEach { memory ->
                        val memoryRoomId = extractMemoryRoomId(memory)
                        if (memoryRoomId.isNotEmpty() && memoryRoomId != currentRoomId) return@forEach

                        val content = memory["content"] as? String
                        val type = memory["type"] as? String
                        val title = memory["title"] as? String
                        @Suppress("UNCHECKED_CAST")
                        val metadata = memory["metadata"] as? Map<String, Any?>
                        val mimeType = metadata?.get("mimeType") as? String

                        // Extract MongoDB _id and authorId (may come as { "$oid": "..." } or plain string)
                        val memoryId = extractMongoId(memory["_id"])
                        val authorId = extractMongoId(memory["authorId"])

                        if (content != null) {
                            val uriStr = when {
                                type == "audio" -> "data:${mimeType ?: "audio/mp4"};base64,$content"
                                type == "video" -> "data:${mimeType ?: "video/mp4"};base64,$content"
                                type == "note" -> "NOTE:${title ?: ""}:$content"
                                content.startsWith("http") -> content
                                // Already a full data URI — normalise non-image types to image/jpeg
                                // so AsyncBase64Image can decode them (e.g. data:application/octet-stream)
                                content.startsWith("data:image") -> content
                                content.startsWith("data:") && content.contains("base64,") ->
                                    "data:image/jpeg;base64," + content.substringAfter("base64,")
                                // Explicit photo type OR long raw base64
                                type == "photo" || content.length > 100 ->
                                    "data:image/${mimeType?.substringAfter("/") ?: "jpeg"};base64,$content"
                                else -> null
                            }
                            if (uriStr != null) {
                                memoriesUrls.add(uriStr)
                                if (memoryId.isNotEmpty() && authorId.isNotEmpty()) {
                                    memoryItemMap[uriStr] = MemoryItem(id = memoryId, authorId = authorId)
                                }
                            }
                        }
                    }
                }
                memoriesResult.onSuccess {
                    val fallbackUris = currentRoom?.photos.orEmpty()
                        .mapNotNull { preview -> preview.url.takeIf { it.isNotBlank() }?.let { Uri.parse(it) } }
                    val loadedUris = memoriesUrls.map { Uri.parse(it) }
                    _roomMedia.value = _roomMedia.value + (currentRoomId to loadedUris.ifEmpty { fallbackUris })
                    _roomMemoryItems.value = _roomMemoryItems.value + (currentRoomId to memoryItemMap)
                }

                // Update room cover with the most-recently-uploaded photo memory so
                // the room detail and Hallway card always reflect what was last dropped.
                val latestPhotoUrl = memoriesUrls.firstOrNull { url ->
                    url.startsWith("http") || url.startsWith("data:image")
                }
                if (latestPhotoUrl != null) {
                    val coverPreview = MemoryPreview("latest", latestPhotoUrl)
                    _selectedRoom.value = _selectedRoom.value?.let { selected ->
                        if (selected.id != currentRoomId) selected else {
                            val existing = selected.photos.filter { it.id != "latest" }
                            selected.copy(photos = listOf(coverPreview) + existing)
                        }
                    }
                    _createdRooms.value = _createdRooms.value.map { r ->
                        if (r.id == currentRoomId) {
                            val existing = r.photos.filter { it.id != "latest" }
                            r.copy(photos = listOf(coverPreview) + existing)
                        } else r
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "refreshRoomMemories failed", e)
            } finally {
                if (showRefreshIndicator) _isRefreshing.value = false
            }
        }
    }

    private fun extractMongoId(value: Any?): String {
        return when (value) {
            is String -> value
            is Map<*, *> -> (value["\$oid"] ?: value["oid"])?.toString() ?: ""
            else -> ""
        }
    }

    private fun extractMemoryRoomId(memory: Map<String, Any>): String {
        return extractMongoId(memory["roomId"])
            .ifEmpty { extractMongoId(memory["room"]) }
            .ifEmpty { extractMongoId(memory["room_id"]) }
            .ifEmpty { extractMongoId(memory["timeCapsuleRoomId"]) }
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

    fun sendHandshakeInvite(context: Context, email: String, role: String = "collaborator") {
        val roomId = _selectedRoom.value?.id ?: return
        viewModelScope.launch {
            try {
                val result = roomRepository.createHandshakeInvite(roomId, email, role)
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
            audioRecorderHelper = AudioRecorderHelper(context)
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
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
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

                        refreshRoomMemories(showRefreshIndicator = false)
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

    private var mediaPlayer: MediaPlayer? = null

    fun playBase64Audio(context: Context, dataUri: String) {
        try {
            mediaPlayer?.release()

            val base64String = dataUri.substringAfter("base64,")
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)

            val extension = when {
                dataUri.startsWith("data:audio/mpeg") -> ".mp3"
                dataUri.startsWith("data:audio/wav") -> ".wav"
                dataUri.startsWith("data:audio/ogg") -> ".ogg"
                else -> ".m4a"
            }
            val tempFile = File.createTempFile("playing_audio", extension, context.cacheDir)
            tempFile.writeBytes(decodedBytes)

            mediaPlayer = MediaPlayer().apply {
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
            mediaPlayer = MediaPlayer().apply {
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
                val returnTo = previousStepBeforeRoomDetail
                previousStepBeforeRoomDetail = ProfileStep.NONE
                _selectedRoom.value = null
                if (returnTo == ProfileStep.NONE) {
                    _currentStep.value = ProfileStep.NONE
                } else {
                    _currentStep.value = returnTo
                }
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
        _roomDescription.value = ""
        _roomTags.value = emptyList()
        _aiGeneratedDescription.value = null
        _selectedRoom.value = null
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
        _uploadStartDateEnabled.value = false
        _uploadStartDate.value = System.currentTimeMillis() + 86400000L
        _uploadStartHour.value = 0
        _uploadStartMinute.value = 0
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
        previousStepBeforeRoomDetail = _currentStep.value
        _selectedRoom.value = room
        _currentStep.value = ProfileStep.ROOM_DETAIL

        // Fetch memories for the newly selected room without triggering the pull-to-refresh UI
        refreshRoomMemories(showRefreshIndicator = false)
        // Fire-and-forget VIEW signal for AI preference learning
        trackViewForRoom(room)
        // If no description exists, ask the AI service to generate one
        if (room.description.isNullOrBlank()) {
            generateAndSaveRoomDescription(room)
        }

        // check for unlock
        checkRoomUnlockStatus(room)
    }

    /** Opens a room from the Hallway card stack. Finds the matching TimeCapsuleRoom from createdRooms
     *  or creates a lightweight placeholder so the detail screen can load memories from the backend. */
    fun selectRoomFromHallway(card: HallwayCard) {
        val existing = _createdRooms.value.find { it.id == card.id }
        val cardPhotos = card.photos.ifEmpty {
            card.imageUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { listOf(MemoryPreview("cover:${card.id}", it)) }
                .orEmpty()
        }
        val room = existing?.copy(
            photos = existing.photos.ifEmpty { cardPhotos },
            description = card.description.ifBlank { existing.description },
            tags = card.tags.ifEmpty { existing.tags },
            music = card.backgroundMusic ?: existing.music,
            isViewerOnly = card.isViewerOnly,
            isOwnedByMe = card.isOwnedByMe,
            isCollaborator = card.isCollaborator,
            ownerUserType = card.ownerUserType ?: existing.ownerUserType
        ) ?: TimeCapsuleRoom(
            id = card.id,
            roomName = card.title,
            capsuleDays = card.timeCapsuleDays,
            capsuleHours = 0,
            capsuleMinutes = 0,
            notificationDays = 0,
            notificationHours = 0,
            isPublic = true,
            description = card.description,
            photos = cardPhotos,
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
        val localRoom = _createdRooms.value.find { it.id == roomId }
        if (localRoom != null) {
            selectRoom(localRoom)
        } else {
             viewModelScope.launch {
                 _isLoading.value = true
                 try {
                     val remoteRoom = getRoomByIdFromRemote(roomId)
                     if (remoteRoom != null) {
                         selectRoom(remoteRoom)
                     }
                 } catch (e: Exception) {
                     Log.e("ProfileViewModel", "Failed to load deep link room: $roomId", e)
                 } finally {
                     _isLoading.value = false
                 }
             }
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
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _targetTime.value
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
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
        val cal = Calendar.getInstance()
        cal.timeInMillis = _scheduledClosureTime.value
        cal.set(Calendar.HOUR_OF_DAY, h)
        _scheduledClosureTime.value = cal.timeInMillis
    }
    fun updateScheduledClosureMinute(m: Int) {
        _scheduledClosureMinute.value = m
        val cal = Calendar.getInstance()
        cal.timeInMillis = _scheduledClosureTime.value
        cal.set(Calendar.MINUTE, m)
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
    fun updateUploadStartDateEnabled(enabled: Boolean) { _uploadStartDateEnabled.value = enabled }
    fun updateUploadStartDate(millis: Long) { _uploadStartDate.value = millis }
    fun updateUploadStartHour(h: Int) {
        _uploadStartHour.value = h
        val cal = Calendar.getInstance()
        cal.timeInMillis = _uploadStartDate.value
        cal.set(Calendar.HOUR_OF_DAY, h)
        _uploadStartDate.value = cal.timeInMillis
    }
    fun updateUploadStartMinute(m: Int) {
        _uploadStartMinute.value = m
        val cal = Calendar.getInstance()
        cal.timeInMillis = _uploadStartDate.value
        cal.set(Calendar.MINUTE, m)
        _uploadStartDate.value = cal.timeInMillis
    }
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
        _uploadStartDateEnabled.value = room.uploadStartDate > 0
        _uploadStartDate.value = if (room.uploadStartDate > 0) room.uploadStartDate
            else System.currentTimeMillis() + 86400000L
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
            val sessionManager = SessionManager(context)
            sessionManager.saveProfilePhotoUri(localUri.toString())
            
            // Upload to backend for global visibility
            viewModelScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val dataUri = "data:image/jpeg;base64,$base64"
                        val authRepo = AuthRepository(context)
                        authRepo.updateMe(UpdateMeRequest(profileImageUrl = dataUri))
                        Log.d("ProfileViewModel", "Profile photo synced to backend")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Failed to sync profile photo", e)
                }
            }
            
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

    // ── Connect Rooms Actions ─────────────────────────────────────────────────

    /** Opens the Connect Rooms screen and fetches AI suggestions for [roomId]. */
    // '+' butonuna basıldığında çalışan fonksiyon
    fun openConnectRooms(roomId: String) {
        _showConnectRooms.value = true
        _isLoadingConnectionSuggestions.value = true
        _connectionSuggestions.value = emptyList()

        viewModelScope.launch {
            try {
                val sourceRoom = _createdRooms.value.find { it.id == roomId } ?: return@launch
                val sourceRoomDto = com.dmb.bestbefore.data.api.models.RoomDto(
                    id = sourceRoom.id,
                    name = sourceRoom.roomName,
                    tags = sourceRoom.tags,
                    isPrivate = !sourceRoom.isPublic,
                    isTimeCapsule = sourceRoom.isCollaboration,
                    description = sourceRoom.description ?: "",
                    ownerEmail = "",
                    createdAt = ""
                )

                // 1. KENDİ ODALARINI ÇEVİR
                val myCandidateRooms = _createdRooms.value.filter {
                    it.id != roomId && !sourceRoom.connectedRooms.contains(it.id)
                }.map { room ->
                    com.dmb.bestbefore.data.api.models.RoomDto(
                        id = room.id,
                        name = room.roomName,
                        tags = room.tags,
                        isPrivate = !room.isPublic,
                        isTimeCapsule = room.isCollaboration,
                        description = room.description ?: "",
                        ownerEmail = "",
                        createdAt = ""
                    )
                }

                // 2. HERKESE AÇIK (PUBLIC) ODALARI ÇEK VE ÇEVİR
                val publicRooms = try {
                    roomRepository.getDiscoverRooms().getOrNull() ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                val publicCandidateRooms = publicRooms.mapNotNull { room ->
                    if (room.id == roomId || sourceRoom.connectedRooms.contains(room.id)) return@mapNotNull null

                    com.dmb.bestbefore.data.api.models.RoomDto(
                        id = room.id,
                        name = room.name, // HATANIN ÇÖZÜLDÜĞÜ YER: roomName yerine name yazıldı
                        tags = room.tags ?: emptyList(),
                        isPrivate = false,
                        isTimeCapsule = false,
                        description = room.description ?: "",
                        ownerEmail = "",
                        createdAt = ""
                    )
                }

                // 3. İKİ LİSTEYİ BİRLEŞTİR (Senin odaların + Başkalarının odaları)
                val candidateRooms = (myCandidateRooms + publicCandidateRooms).distinctBy { it.id }

                if (candidateRooms.isNotEmpty()) {
                    val userDto = authRepository?.getMe()?.getOrNull()
                        ?: com.dmb.bestbefore.data.api.models.UserDto(id="", email="", name="")

                    val result = aiRepository.getPersonalisedSuggestions(
                        user = userDto,
                        candidateRooms = candidateRooms,
                        sourceRoomId = roomId,
                        sourceRoom = sourceRoomDto
                    )

                    result.onSuccess { response ->
                        _connectionSuggestions.value = response.suggestions.map { aiSuggestion ->
                            com.dmb.bestbefore.data.api.models.RoomSuggestionDto(
                                targetRoomId = aiSuggestion.targetRoomId,
                                targetRoomName = aiSuggestion.targetRoomName,
                                score = aiSuggestion.score,
                                category = aiSuggestion.category,
                                reasoning = aiSuggestion.reasoning
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Bağlantı önerisi hatası", e)
            } finally {
                _isLoadingConnectionSuggestions.value = false
            }
        }
    }

    // Ekranı kapatma fonksiyonun (zaten varsa dokunmana gerek yok)
    fun closeConnectRooms() {
        _showConnectRooms.value = false
        _connectionSuggestions.value = emptyList()
    }

    /**
     * Accept a suggested connection. Adds the targetRoomId to the current room's
     * connectedRooms locally and removes the suggestion from the list.
     */
    fun acceptConnectionSuggestion(sourceRoomId: String, suggestion: RoomSuggestionDto) {
        viewModelScope.launch {
            val result = roomRepository.acceptSuggestion(sourceRoomId, suggestion.targetRoomId)
            result.onSuccess {
                // Remove from suggestions list
                _connectionSuggestions.value = _connectionSuggestions.value.filter {
                    it.targetRoomId != suggestion.targetRoomId
                }
                // Update local room state so Connected Rooms section refreshes immediately
                val current = _selectedRoom.value ?: return@onSuccess
                if (!current.connectedRooms.contains(suggestion.targetRoomId)) {
                    _selectedRoom.value = current.copy(
                        connectedRooms = current.connectedRooms + suggestion.targetRoomId
                    )
                }
                // Mirror in createdRooms list
                _createdRooms.value = _createdRooms.value.map { r ->
                    if (r.id == sourceRoomId && !r.connectedRooms.contains(suggestion.targetRoomId))
                        r.copy(connectedRooms = r.connectedRooms + suggestion.targetRoomId)
                    else r
                }
                Log.d("ProfileViewModel", "Connected ${suggestion.targetRoomName} to room $sourceRoomId")
            }.onFailure { e ->
                Log.e("ProfileViewModel", "Failed to accept suggestion: ${e.message}")
            }
        }
    }

    /** Reject a suggested connection. Removes from the local list. */
    fun rejectConnectionSuggestion(sourceRoomId: String, suggestion: RoomSuggestionDto) {
        _connectionSuggestions.value = _connectionSuggestions.value.filter {
            it.targetRoomId != suggestion.targetRoomId
        }
        viewModelScope.launch {
            roomRepository.rejectSuggestion(sourceRoomId, suggestion.targetRoomId)
                .onFailure { e ->
                    Log.w("ProfileViewModel", "Failed to persist rejection: ${e.message}")
                }
        }
    }

    // ── Memory Deletion ───────────────────────────────────────────────────────

    /**
     * Returns true when [uri] has a known server-side memory ID, meaning it was synced
     * from the backend and can be deleted via the API.
     *
     * Owner/collaborator gating is done at the call site (canContribute check in the UI).
     * The backend independently validates per-memory ownership on DELETE.
     *
     * Previously this also compared authorId against _cachedUserDto.id, which silently
     * returned false whenever _cachedUserDto was null (e.g. when entering via Hallway
     * before initDatabase has run) — causing long-press to do nothing.
     */
    fun isMyMemory(roomId: String, uri: Uri): Boolean {
        val item = _roomMemoryItems.value[roomId]?.get(uri.toString()) ?: return false
        return item.id.isNotEmpty()
    }

    /** Delete a memory the current user uploaded. Updates local state on success. */
    fun deleteMemory(roomId: String, uri: Uri) {
        val uriStr = uri.toString()
        val item = _roomMemoryItems.value[roomId]?.get(uriStr) ?: return
        if (item.id.isEmpty()) return
        viewModelScope.launch {
            val result = roomRepository?.deleteMemory(roomId, item.id)
                ?: Result.failure(Exception("Repository not ready"))
            result.onSuccess {
                val updatedUris = _roomMedia.value[roomId]?.filter { it.toString() != uriStr }
                if (updatedUris != null) {
                    _roomMedia.value = _roomMedia.value + (roomId to updatedUris)
                }
                val updatedItems = _roomMemoryItems.value[roomId]?.toMutableMap()
                    ?.also { it.remove(uriStr) }
                if (updatedItems != null) {
                    _roomMemoryItems.value = _roomMemoryItems.value + (roomId to updatedItems)
                }
                _totalMemories.value = (_totalMemories.value - 1).coerceAtLeast(0)
                Log.d("ProfileViewModel", "Memory deleted: $uriStr")
            }.onFailure { e ->
                Log.e("ProfileViewModel", "Failed to delete memory: ${e.message}")
            }
        }
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
                _totalMemories.value = _createdRooms.value.sumOf { r -> 
                    val media = _roomMedia.value[r.id]
                    media?.size ?: r.photos.size 
                }
                
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
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val closureIso: String? = if (_scheduledClosureEnabled.value) isoFmt.format(Date(_scheduledClosureTime.value)) else null
        val uploadStartIso: String? = if (_uploadStartDateEnabled.value) isoFmt.format(Date(_uploadStartDate.value)) else null

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
            "expirationDate" to closureIso,
            "uploadStartDate" to uploadStartIso,
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
                    uploadStartDate = if (_uploadStartDateEnabled.value) _uploadStartDate.value else 0L,
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
                val authRepo = AuthRepository(context)
                val firebaseToken = authRepo.getFirebaseIdToken(false)
                if (firebaseToken != null) {
                    val sessionManager = SessionManager(context)
                    val manualProfileTags = _preferredTags.value
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinctBy { it.lowercase(Locale.US) }
                    val rawProfileImageUri = _profileImageUri.value?.toString()
                    val profileImageUrlForBackend = if (
                        rawProfileImageUri != null &&
                        (rawProfileImageUri.startsWith("http://") ||
                            rawProfileImageUri.startsWith("https://") ||
                            rawProfileImageUri.startsWith("data:image"))
                    ) rawProfileImageUri else null
                    
                    val profileImageBase64 = _profileImageUri.value?.let { uri ->
                        if (profileImageUrlForBackend == null && uri is Uri) encodeProfileImageBase64(context, uri) else null
                    }
                    
                    val updateResult = authRepo.updateMe(
                        UpdateMeRequest(
                            name = _userName.value,
                            bio = _bio.value,
                            profileImageUrl = profileImageUrlForBackend,
                            profileImageBase64 = profileImageBase64,
                            profileImageData = _cachedUserDto?.profileImageData,
                            preferredTags = manualProfileTags, // send empty list instead of null to be safe
                            theme = _selectedTheme.value.name,
                            accentColor = colorToHex(_accentColor.value),
                            savedRoomIds = sessionManager.getSavedRoomIds(),
                            ignoredRoomIds = sessionManager.getIgnoredRoomIds(),
                            // Include AI Preference fields to ensure they are preserved on update
                            preferenceTagWeights = _cachedUserDto?.preferenceTagWeights,
                            preferenceRoomTypes = _cachedUserDto?.preferenceRoomTypes,
                            preferenceEmbedding = _cachedUserDto?.preferenceEmbedding,
                            preferenceInteractions = _cachedUserDto?.preferenceInteractions,
                            preferenceUpdatedAt = _cachedUserDto?.preferenceUpdatedAt,
                            lastLat = _cachedUserDto?.lastLat,
                            lastLon = _cachedUserDto?.lastLon
                        )
                    )
                    if (updateResult.isSuccess) {
                        // Reflect what the backend actually saved back into VM state so the
                        // UI stays consistent (e.g. backend may transform profileImageBase64 → URL).
                        val saved = updateResult.getOrNull()
                        if (saved != null) {
                            val savedWithManualTags = saved.copy(preferredTags = manualProfileTags)
                            if (!saved.name.isNullOrBlank()) _userName.value = saved.name
                            if (saved.bio != null) _bio.value = saved.bio
                            if (!saved.profileImageUrl.isNullOrBlank()) _profileImageUri.value = saved.profileImageUrl
                            _preferredTags.value = manualProfileTags
                            _cachedUserDto = savedWithManualTags
                            sessionManager.saveManualProfileTags(manualProfileTags)
                            sessionManager.saveUser(savedWithManualTags)
                            if (saved.theme.isNotBlank()) {
                                val t = AppThemes.getThemeByName(saved.theme)
                                _selectedTheme.value = t
                                ThemeState.selectTheme(context, t)
                            }
                            if (saved.accentColor.isNotBlank()) {
                                runCatching {
                                    val c = Color(android.graphics.Color.parseColor(saved.accentColor))
                                    _accentColor.value = c
                                    ThemeState.selectAccent(context, c)
                                }
                            }
                        }
                        Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = updateResult.exceptionOrNull()?.message ?: "Unknown error"
                        Log.w("ProfileViewModel", "Update failed: $errorMsg")
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

    private suspend fun encodeProfileImageBase64(context: Context, uri: Uri): String? = withContext(
        Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            val raw = stream.use { it.readBytes() }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(raw, 0, raw.size, options)
            var inSampleSize = 1
            while (options.outWidth / inSampleSize > 512 || options.outHeight / inSampleSize > 512) {
                inSampleSize *= 2
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size, options) ?: return@runCatching null
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, bos)
            Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
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
        ThemeState.init(context)
        _selectedTheme.value = ThemeState.currentTheme
        _accentColor.value = ThemeState.currentAccent
        _applyAccentToAll.value = ThemeState.applyAccentToAll
        _syncAccentWithRoom.value = ThemeState.syncAccentWithRoom
    }
    
    fun selectTheme(context: Context, theme: AppTheme) {
        ThemeState.selectTheme(context, theme)
        _selectedTheme.value = theme
    }
    
    fun selectAccentColor(context: Context, color: Color) {
        ThemeState.selectAccent(context, color)
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
                                        val authRepo = AuthRepository(context)
                                        val firebaseToken = authRepo.getFirebaseIdToken(true) // force refresh after email change
                                        if (firebaseToken != null) {
                                            val updateResult = authRepo.updateMe(
                                                UpdateMeRequest(email = newEmail)
                                            )
                                            if (updateResult.isSuccess) {
                                                _credentialUpdateSuccess.value = "Verification email sent to $newEmail. Please verify to complete the change."
                                                val sessionManager = SessionManager(context)
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
        val sessionManager = SessionManager(context)
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
        SessionManager(context).saveRoomEmotions(uid, current)

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
        val authRepo = AuthRepository(context)
        return authRepo.getFirebaseIdToken(false)
    }
    private fun isCollaborator(room: RoomDto, currentUserEmail: String): Boolean {
        if (currentUserEmail.isBlank()) return false
        return room.collaborators?.any { element ->
            if (element.isJsonPrimitive) {
                element.asString.equals(currentUserEmail, ignoreCase = true)
            } else if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                element.asJsonObject.get("email").asString.equals(currentUserEmail, ignoreCase = true)
            } else {
                false
            }
        } == true
    }

    private fun isViewer(room: RoomDto, currentUserEmail: String): Boolean {
        if (currentUserEmail.isBlank()) return false
        return room.viewers?.any { element ->
            if (element.isJsonPrimitive) {
                element.asString.equals(currentUserEmail, ignoreCase = true)
            } else if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                element.asJsonObject.get("email").asString.equals(currentUserEmail, ignoreCase = true)
            } else {
                false
            }
        } == true
    }

    private fun parseISO8601(dateString: String?): Long = parseIso8601(dateString)
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
