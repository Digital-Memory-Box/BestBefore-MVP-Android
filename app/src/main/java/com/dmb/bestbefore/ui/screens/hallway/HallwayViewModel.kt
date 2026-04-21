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

    private var myRoomsList: List<com.dmb.bestbefore.data.api.models.RoomDto> = emptyList()
    private var discoverRoomsList: List<com.dmb.bestbefore.data.api.models.RoomDto> = emptyList()

    init {
        fetchRooms()
    }

    private fun fetchRooms() {
        viewModelScope.launch {
            try {
                val myDeferred = async { roomRepository.getRooms() }
                val discoverDeferred = async { roomRepository.getDiscoverRooms() }
                
                val myResult = myDeferred.await()
                val discoverResult = discoverDeferred.await()
                
                myResult.onSuccess { rooms ->
                    Log.d("HallwayViewModel", "getRooms: fetched ${rooms.size} rooms")
                    myRoomsList = rooms
                }
                myResult.onFailure { e ->
                    Log.e("HallwayViewModel", "getRooms failed: ${e.message}")
                }
                discoverResult.onSuccess { rooms ->
                    Log.d("HallwayViewModel", "getDiscoverRooms: fetched ${rooms.size} rooms")
                    discoverRoomsList = rooms
                }
                discoverResult.onFailure { e ->
                    Log.e("HallwayViewModel", "getDiscoverRooms failed: ${e.message}")
                }
                
                filterCards(_currentTab.value)
            } catch (e: Exception) {
                Log.e("HallwayViewModel", "Error fetching rooms", e)
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
                Log.e("HallwayViewModel", "Error fetching tags", e)
            }
        }
    }

    private fun isRoomPublic(room: com.dmb.bestbefore.data.api.models.RoomDto): Boolean {
        // isPublic may be null if not explicitly set; fall back to !isPrivate (false = public)
        return room.isPublic == true || !room.isPrivate
    }

    private fun filterCards(tab: BottomTab) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val allAvailableRooms = (myRoomsList + discoverRoomsList).distinctBy { it.id }

        val filteredRooms = when (tab) {
            BottomTab.ROOMING -> {
                myRoomsList.filter { room ->
                    val isOwner = room.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true
                    val isCollaborator = room.collaborators?.any { element ->
                        if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                            element.asJsonObject.get("email").asString.equals(currentUserEmail, ignoreCase = true)
                        } else false
                    } == true
                    val isViewer = room.viewers?.any { element ->
                        if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                            element.asJsonObject.get("email").asString.equals(currentUserEmail, ignoreCase = true)
                        } else false
                    } == true
                    val isPrivate = room.isPrivate
                    !isOwner && (isCollaborator || isViewer) && isPrivate
                }
            }
            BottomTab.EVERYONE -> {
                // Show ALL public rooms in the Hallway tab
                allAvailableRooms.filter { isRoomPublic(it) }
            }
            BottomTab.ARTISTS -> {
                // Show public rooms where owner is explicitly an artist
                // Rooms with ownerUserType = "artist" appear here (and also in EVERYONE above)
                allAvailableRooms.filter {
                    it.ownerUserType?.equals("artist", ignoreCase = true) == true && isRoomPublic(it)
                }
            }
        }

        val mappedCards = filteredRooms.map { room ->
            HallwayCard(
                id = room.id,
                title = room.name,
                timeCapsuleDays = room.capsuleDurationDays,
                description = room.description ?: "",
                imageUrl = room.photos?.firstOrNull(),
                photos = room.photos ?: emptyList(),
                themeColorHex = room.theme,
                tags = room.tags ?: emptyList(),
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
                backgroundMusic = room.backgroundMusic
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
