package com.dmb.bestbefore.ui.screens.hallway

import android.util.Log
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.models.HallwayCard
import com.dmb.bestbefore.data.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

class HallwayViewModel(application: Application) : AndroidViewModel(application) {

    private val roomRepository = RoomRepository()
    private val authRepository: com.dmb.bestbefore.data.repository.AuthRepository by lazy {
        com.dmb.bestbefore.data.repository.AuthRepository(getApplication())
    }

    private val _cards = MutableStateFlow<List<HallwayCard>>(emptyList())
    val cards: StateFlow<List<HallwayCard>> = _cards.asStateFlow()

    private val _selectedCardIndex = MutableStateFlow(0)
    val selectedCardIndex: StateFlow<Int> = _selectedCardIndex.asStateFlow()

    private val _currentTab = MutableStateFlow(BottomTab.EVERYONE)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

    private val _selectedFilterTag = MutableStateFlow<String?>(null)
    val selectedFilterTag: StateFlow<String?> = _selectedFilterTag.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Rooming tab filter: NONE (All), SAVED_ONLY, COLLABORATED_ONLY
    enum class RoomingFilter { NONE, SAVED_ONLY, COLLABORATED_ONLY }
    private val _roomingFilter = MutableStateFlow(RoomingFilter.NONE)
    val roomingFilter: StateFlow<RoomingFilter> = _roomingFilter.asStateFlow()

    fun setRoomingFilter(filter: RoomingFilter) {
        _roomingFilter.value = filter
    }

    val filteredCards: StateFlow<List<HallwayCard>> = combine(_cards, _selectedFilterTag, _searchQuery) { cards, tag, query ->
        val normalizedQuery = query.trim()
        cards.filter { card ->
            val matchesTagFilter = tag.isNullOrBlank() || card.tags.any { it.equals(tag, ignoreCase = true) }
            val matchesSearch = if (normalizedQuery.isBlank()) {
                true
            } else {
                card.title.contains(normalizedQuery, ignoreCase = true)
            }
            matchesTagFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isOrbMenuVisible = MutableStateFlow(true)
    val isOrbMenuVisible: StateFlow<Boolean> = _isOrbMenuVisible.asStateFlow()

    private val _showingSoundCloudModal = MutableStateFlow(false)
    val showingSoundCloudModal: StateFlow<Boolean> = _showingSoundCloudModal.asStateFlow()

    private val _isDescriptionExpanded = MutableStateFlow(false)
    val isDescriptionExpanded: StateFlow<Boolean> = _isDescriptionExpanded.asStateFlow()

    private val _activePagerPage = MutableStateFlow(0)
    val activePagerPage: StateFlow<Int> = _activePagerPage.asStateFlow()

    private val _areCollaboratorsExpanded = MutableStateFlow(false)
    val areCollaboratorsExpanded: StateFlow<Boolean> = _areCollaboratorsExpanded.asStateFlow()

    private val _cardImageIndices = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cardImageIndices: StateFlow<Map<String, Int>> = _cardImageIndices.asStateFlow()

    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags: StateFlow<List<String>> = _availableTags.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    // ── Saved Rooms (Added to Rooming via "Add to Rooming" button) ─────────
    private val _savedRoomCards = MutableStateFlow<List<HallwayCard>>(emptyList())
    val savedRoomCards: StateFlow<List<HallwayCard>> = _savedRoomCards.asStateFlow()

    fun saveRoomToRooming(card: HallwayCard) {
        if (isRoomIgnored(card.id)) return
        if (_savedRoomCards.value.none { it.id == card.id }) {
            _savedRoomCards.value = _savedRoomCards.value + card
            persistSavedRooms()
        }
    }

    fun removeSavedRoom(cardId: String) {
        _savedRoomCards.value = _savedRoomCards.value.filter { it.id != cardId }
        persistSavedRooms()
    }

    fun isRoomSaved(cardId: String): Boolean {
        return _savedRoomCards.value.any { it.id == cardId }
    }

    private fun persistSavedRooms() {
        viewModelScope.launch {
            val ids = _savedRoomCards.value.map { it.id }
            val result = authRepository.updateMe(com.dmb.bestbefore.data.api.models.UpdateMeRequest(savedRoomIds = ids))
            if (result.isFailure) {
                Log.e("HallwayViewModel", "Failed to persist saved rooms: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // ── Ignored Rooms (hidden from Hallway & Artists) ────────────────────
    private val _ignoredRoomIds = MutableStateFlow<Set<String>>(emptySet())
    private val _ignoredRoomCards = MutableStateFlow<List<HallwayCard>>(emptyList())
    val ignoredRoomCards: StateFlow<List<HallwayCard>> = _ignoredRoomCards.asStateFlow()

    fun ignoreRoom(card: HallwayCard) {
        if (!_ignoredRoomIds.value.contains(card.id)) {
            _ignoredRoomIds.value = _ignoredRoomIds.value + card.id
            _ignoredRoomCards.value = _ignoredRoomCards.value + card
            // Ignored rooms must not stay in Saved Rooming (Rooms tab)
            if (_savedRoomCards.value.any { it.id == card.id }) {
                _savedRoomCards.value = _savedRoomCards.value.filter { it.id != card.id }
            }
            // One PATCH keeps ignored + saved lists consistent on the server
            viewModelScope.launch {
                val result = authRepository.updateMe(
                    com.dmb.bestbefore.data.api.models.UpdateMeRequest(
                        ignoredRoomIds = _ignoredRoomIds.value.toList(),
                        savedRoomIds = _savedRoomCards.value.map { it.id }
                    )
                )
                if (result.isFailure) {
                    Log.e("HallwayViewModel", "Failed to sync ignore/save state: ${result.exceptionOrNull()?.message}")
                }
            }
            filterCards(_currentTab.value)
        }
    }

    fun unignoreRoom(cardId: String) {
        _ignoredRoomIds.value = _ignoredRoomIds.value - cardId
        _ignoredRoomCards.value = _ignoredRoomCards.value.filter { it.id != cardId }
        persistIgnoredRooms()
        filterCards(_currentTab.value)
    }

    private fun persistIgnoredRooms() {
        viewModelScope.launch {
            val result = authRepository.updateMe(com.dmb.bestbefore.data.api.models.UpdateMeRequest(ignoredRoomIds = _ignoredRoomIds.value.toList()))
            if (result.isFailure) {
                Log.e("HallwayViewModel", "Failed to persist ignored rooms: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun isRoomIgnored(cardId: String): Boolean = _ignoredRoomIds.value.contains(cardId)

    private var myRoomsList: List<com.dmb.bestbefore.data.api.models.RoomDto> = emptyList()
    private var discoverRoomsList: List<com.dmb.bestbefore.data.api.models.RoomDto> = emptyList()

    // ── Similar Rooms Mode ──────────────────────────────────────────────
    private val _similarModeSource = MutableStateFlow<HallwayCard?>(null)
    val similarModeSource: StateFlow<HallwayCard?> = _similarModeSource.asStateFlow()

    private val _similarityScores = MutableStateFlow<Map<String, Int>>(emptyMap())

    fun enterSimilarMode(card: HallwayCard) {
        _similarModeSource.value = card
        _selectedCardIndex.value = 0
        _activePagerPage.value = 0
        
        viewModelScope.launch {
            val result = roomRepository.getRoomSuggestions(card.id)
            result.onSuccess { response ->
                _similarityScores.value = response.suggestions.associate { it.targetRoomId to it.score }
                filterCards(_currentTab.value)
            }
        }
    }

    fun exitSimilarMode() {
        _similarModeSource.value = null
        _similarityScores.value = emptyMap()
        filterCards(_currentTab.value)
    }

    fun connectRoom(targetCard: HallwayCard) {
        val sourceCard = _similarModeSource.value ?: return
        viewModelScope.launch {
            val result = roomRepository.acceptSuggestion(sourceCard.id, targetCard.id)
            result.onSuccess {
                // Success! Maybe show a toast or update local state
                Log.d("HallwayViewModel", "Connected ${sourceCard.title} to ${targetCard.title}")
            }
        }
    }

    init {
        fetchRooms()
    }

    private fun fetchRooms() {
        viewModelScope.launch {
            _isInitialLoading.value = true
            try {
                val myDeferred = async { roomRepository.getRooms() }
                val discoverDeferred = async { roomRepository.getDiscoverRooms() }
                val meDeferred = async { authRepository.getMe() }
                
                val myResult = myDeferred.await()
                val discoverResult = discoverDeferred.await()
                val meResult = meDeferred.await()
                
                if (myResult.isSuccess) {
                    val rooms = myResult.getOrThrow()
                    Log.d("HallwayViewModel", "getRooms: fetched ${rooms.size} rooms")
                    myRoomsList = rooms
                } else {
                    val e = myResult.exceptionOrNull()
                    Log.e("HallwayViewModel", "getRooms failed: ${e?.message}")
                }

                if (discoverResult.isSuccess) {
                    val rooms = discoverResult.getOrThrow()
                    Log.d("HallwayViewModel", "getDiscoverRooms: fetched ${rooms.size} rooms")
                    discoverRoomsList = rooms
                } else {
                    val e = discoverResult.exceptionOrNull()
                    Log.e("HallwayViewModel", "getDiscoverRooms failed: ${e?.message}")
                }

                // Sync ignored/saved rooms from profile
                if (meResult.isSuccess) {
                    val userDto = meResult.getOrThrow()
                    _ignoredRoomIds.value = userDto.ignoredRoomIds?.toSet() ?: emptySet()
                    
                    // We need to fetch the actual cards for these IDs to populate _ignoredRoomCards
                    val allRooms = (myRoomsList + discoverRoomsList).distinctBy { it.id }
                    _ignoredRoomCards.value = allRooms.filter { _ignoredRoomIds.value.contains(it.id) }.map { room ->
                        HallwayCard(
                            id = room.id,
                            title = room.name,
                            timeCapsuleDays = room.capsuleDurationDays,
                            description = room.description ?: room.generatedDescription ?: "",
                            imageUrl = room.photos?.firstOrNull(),
                            photos = room.photos ?: emptyList(),
                            themeColorHex = room.theme,
                            tags = room.tags ?: emptyList(),
                            ownerId = room.ownerId,
                            ownerEmail = room.ownerEmail,
                            ownerName = room.ownerName,
                            ownerUserType = room.ownerUserType,
                            ownerProfilePic = room.ownerProfilePic,
                            collaboratorCount = room.collaborators?.size ?: 0,
                            collaborators = emptyList(), // minimal for settings view
                            location = null,
                            backgroundMusic = room.backgroundMusic
                        )
                    }

                    val savedIds = userDto.savedRoomIds?.toSet() ?: emptySet()
                    // Do not show ignored rooms under Saved (even if backend still lists them in savedRoomIds)
                    _savedRoomCards.value = allRooms.filter { savedIds.contains(it.id) && !_ignoredRoomIds.value.contains(it.id) }.map { room ->
                        HallwayCard(
                            id = room.id,
                            title = room.name,
                            timeCapsuleDays = room.capsuleDurationDays,
                            description = room.description ?: room.generatedDescription ?: "",
                            imageUrl = room.photos?.firstOrNull(),
                            photos = room.photos ?: emptyList(),
                            themeColorHex = room.theme,
                            tags = room.tags ?: emptyList(),
                            ownerId = room.ownerId,
                            ownerEmail = room.ownerEmail,
                            ownerName = room.ownerName,
                            ownerUserType = room.ownerUserType,
                            ownerProfilePic = room.ownerProfilePic,
                            collaboratorCount = room.collaborators?.size ?: 0,
                            collaborators = emptyList(),
                            location = null,
                            backgroundMusic = room.backgroundMusic
                        )
                    }
                }
                
                filterCards(_currentTab.value)
            } catch (e: Exception) {
                Log.e("HallwayViewModel", "Error fetching rooms", e)
            } finally {
                _isInitialLoading.value = false
            }
            
            // Try fetching tags
            try {
                val token = com.dmb.bestbefore.data.repository.AuthRepository(getApplication()).getFirebaseIdToken(false)
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
                        _availableTags.value = parsedTags
                    }
                }
            } catch (e: Exception) {
                Log.e("HallwayViewModel", "Error fetching tags", e)
            }
        }
    }

    private fun isRoomPublic(room: com.dmb.bestbefore.data.api.models.RoomDto): Boolean {
        // isPublic may be null if not explicitly set; fall back to !isPrivate (false = public)
        return room.isPublic == true || !room.isPrivate
    }

    private fun isArtistRoom(room: com.dmb.bestbefore.data.api.models.RoomDto): Boolean {
        val type = room.ownerUserType?.trim()?.lowercase() ?: return false
        return type == "artist" || type.contains("artist")
    }

    private fun isCollaborator(room: com.dmb.bestbefore.data.api.models.RoomDto, currentUserEmail: String): Boolean {
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

    private fun isViewer(room: com.dmb.bestbefore.data.api.models.RoomDto, currentUserEmail: String): Boolean {
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

    private fun filterCards(tab: BottomTab) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val allAvailableRooms = (myRoomsList + discoverRoomsList).distinctBy { it.id }

        val filteredRooms = if (_similarModeSource.value != null) {
            val scores = _similarityScores.value
            val sourceId = _similarModeSource.value?.id ?: ""
            
            // In similar mode, we only show rooms that have a suggestion score
            val relevantRooms = allAvailableRooms.filter { room ->
                room.id != sourceId && scores.containsKey(room.id) && !_ignoredRoomIds.value.contains(room.id)
            }
            
            // Group 1: User's rooms sorted by score
            val myRelevant = relevantRooms.filter { it.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true }
                .sortedByDescending { scores[it.id] ?: 0 }
            
            // Group 2: Other rooms sorted by score
            val otherRelevant = relevantRooms.filter { it.ownerEmail?.equals(currentUserEmail, ignoreCase = true) != true }
                .sortedByDescending { scores[it.id] ?: 0 }
            
            myRelevant + otherRelevant
        } else {
            when (tab) {
                BottomTab.ROOMING -> {
                    myRoomsList.filter { room ->
                        val isOwner = room.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true
                        val isCollaborator = isCollaborator(room, currentUserEmail)
                        val isViewer = isViewer(room, currentUserEmail)
                        val isPrivate = room.isPrivate
                        !isOwner && (isCollaborator || isViewer) && isPrivate
                    }
                }
                BottomTab.EVERYONE -> {
                    allAvailableRooms.filter {
                        val isOwner = it.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true
                        !isOwner &&
                        isRoomPublic(it) &&
                        !isArtistRoom(it) &&
                        !_ignoredRoomIds.value.contains(it.id)
                    }
                }
                BottomTab.ARTISTS -> {
                    allAvailableRooms.filter {
                        val isOwner = it.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true
                        !isOwner &&
                        isArtistRoom(it) &&
                        isRoomPublic(it) &&
                        !_ignoredRoomIds.value.contains(it.id)
                    }
                }
            }
        }

        val mappedCards = filteredRooms.map { room ->
            val currentUserEmail2 = FirebaseAuth.getInstance().currentUser?.email ?: ""
            val isOwner = room.ownerEmail?.equals(currentUserEmail2, ignoreCase = true) == true
            val isCollaborator = isCollaborator(room, currentUserEmail2)
            val isViewer = isViewer(room, currentUserEmail2)
            val isViewerOnly = !isOwner && !isCollaborator && (isViewer || isRoomPublic(room))
            HallwayCard(
                id = room.id,
                title = room.name,
                timeCapsuleDays = room.capsuleDurationDays,
                description = if (!room.description.isNullOrBlank()) room.description else (room.generatedDescription ?: ""),
                imageUrl = room.photos?.firstOrNull(),
                photos = room.photos ?: emptyList(),
                themeColorHex = room.theme,
                tags = room.tags ?: emptyList(),
                ownerId = room.ownerId,
                ownerEmail = room.ownerEmail,
                ownerName = room.ownerName,
                ownerUserType = room.ownerUserType,
                ownerProfilePic = room.ownerProfilePic,
                collaboratorCount = room.collaborators?.size ?: 0,
                collaborators = room.collaborators?.mapNotNull { element ->
                    if (element.isJsonObject) {
                        try {
                            com.google.gson.Gson().fromJson(element, com.dmb.bestbefore.data.api.models.UserDto::class.java)
                        } catch (e: Exception) { null }
                    } else null
                } ?: emptyList(),
                location = null, // Will fetch from DB if location is added to RoomDto later
                backgroundMusic = room.backgroundMusic,
                isViewerOnly = isViewerOnly,
                isOwnedByMe = isOwner,
                isCollaborator = isCollaborator
            )
        }
        _cards.value = mappedCards

        if (mappedCards.isNotEmpty() && _selectedCardIndex.value >= mappedCards.size) {
            _selectedCardIndex.value = mappedCards.size - 1
        }
        if (mappedCards.isEmpty()) {
            _selectedCardIndex.value = 0
            _activePagerPage.value = 0
        }
    }

    fun selectCard(index: Int) {
        if (index >= 0 && index < _cards.value.size) {
            _selectedCardIndex.value = index
            _activePagerPage.value = index
            _areCollaboratorsExpanded.value = false
        }
    }

    fun selectTab(tab: BottomTab) {
        _currentTab.value = tab
        _selectedCardIndex.value = 0
        _activePagerPage.value = 0
        _selectedFilterTag.value = null
        _searchQuery.value = ""
        _areCollaboratorsExpanded.value = false
        filterCards(tab)
    }

    fun setSelectedFilterTag(tag: String?) {
        _selectedFilterTag.value = tag
        _selectedCardIndex.value = 0
        _activePagerPage.value = 0
        _areCollaboratorsExpanded.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _selectedCardIndex.value = 0
        _activePagerPage.value = 0
        _areCollaboratorsExpanded.value = false
    }

    fun setOrbMenuVisible(isVisible: Boolean) {
        _isOrbMenuVisible.value = isVisible
    }

    fun setSoundCloudModalVisible(isVisible: Boolean) {
        _showingSoundCloudModal.value = isVisible
    }

    fun setDescriptionExpanded(isExpanded: Boolean) {
        _isDescriptionExpanded.value = isExpanded
    }

    fun setActivePagerPage(index: Int) {
        _activePagerPage.value = index
        _selectedCardIndex.value = index
        _areCollaboratorsExpanded.value = false
    }

    fun toggleCollaboratorsExpanded() {
        _areCollaboratorsExpanded.value = !_areCollaboratorsExpanded.value
    }

    fun collapseCollaborators() {
        _areCollaboratorsExpanded.value = false
    }

    fun setCardImageIndex(cardId: String, index: Int) {
        _cardImageIndices.value = _cardImageIndices.value.toMutableMap().apply {
            this[cardId] = index
        }
    }

    // Pull-to-refresh support
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshRooms() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val myDeferred = async { roomRepository.getRooms() }
                val discoverDeferred = async { roomRepository.getDiscoverRooms() }
                
                val myResult = myDeferred.await()
                val discoverResult = discoverDeferred.await()
                
                myResult.onSuccess { rooms -> myRoomsList = rooms }
                discoverResult.onSuccess { rooms -> discoverRoomsList = rooms }
                
                filterCards(_currentTab.value)
            } catch (e: Exception) {
                Log.e("HallwayViewModel", "Error refreshing rooms", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

enum class BottomTab {
    ROOMING, EVERYONE, ARTISTS
}
