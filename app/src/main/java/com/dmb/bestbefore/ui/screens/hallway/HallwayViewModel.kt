package com.dmb.bestbefore.ui.screens.hallway

import android.util.Log
import androidx.lifecycle.ViewModel
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

class HallwayViewModel : ViewModel() {

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

    private var allApiRooms: List<com.dmb.bestbefore.data.api.models.RoomDto> = emptyList()

    init {
        fetchRooms()
    }

    private fun fetchRooms() {
        viewModelScope.launch {
            try {
                val result = roomRepository.getRooms()
                result.onSuccess { apiRooms ->
                    allApiRooms = apiRooms
                    filterCards(_currentTab.value)
                }
                result.onFailure {
                    Log.e("HallwayViewModel", "Failed to fetch hallway rooms", it)
                }
            } catch (e: Exception) {
                Log.e("HallwayViewModel", "Error fetching rooms", e)
            }
        }
    }

    private fun filterCards(tab: BottomTab) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

        val filteredRooms = when (tab) {
            BottomTab.ROOMING -> {
                allApiRooms.filter { room ->
                    room.ownerEmail == currentUserEmail
                }
            }
            BottomTab.EVERYONE -> {
                allApiRooms.filter { room -> !room.isPrivate }
            }
            BottomTab.ARTISTS -> emptyList()
        }

        val mappedCards = filteredRooms.map { room ->
            HallwayCard(
                id = room.id,
                title = room.name,
                timeCapsuleDays = room.capsuleDurationDays,
                description = room.description ?: "A room awaiting memories.",
                imageUrl = room.photos?.firstOrNull(),
                photos = room.photos ?: emptyList(),
                themeColorHex = room.theme,
                tags = room.tags ?: emptyList(),
                ownerEmail = room.ownerEmail,
                collaboratorCount = room.collaborators?.size ?: 0,
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
                val result = roomRepository.getRooms()
                result.onSuccess { apiRooms ->
                    allApiRooms = apiRooms
                    filterCards(_currentTab.value)
                }
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
