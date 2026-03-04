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
import kotlinx.coroutines.launch

class HallwayViewModel : ViewModel() {

    private val roomRepository = RoomRepository()

    private val _cards = MutableStateFlow<List<HallwayCard>>(emptyList())
    val cards: StateFlow<List<HallwayCard>> = _cards.asStateFlow()

    private val _selectedCardIndex = MutableStateFlow(0)
    val selectedCardIndex: StateFlow<Int> = _selectedCardIndex.asStateFlow()

    private val _currentTab = MutableStateFlow(BottomTab.EVERYONE)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

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
                imageUrl = room.photos?.firstOrNull()
            )
        }
        _cards.value = mappedCards
        
        if (mappedCards.isNotEmpty() && _selectedCardIndex.value >= mappedCards.size) {
             _selectedCardIndex.value = mappedCards.size - 1
        }
    }

    fun selectCard(index: Int) {
        if (index >= 0 && index < _cards.value.size) {
            _selectedCardIndex.value = index
        }
    }

    fun selectTab(tab: BottomTab) {
        _currentTab.value = tab
        _selectedCardIndex.value = 0
        filterCards(tab)
    }
}

enum class BottomTab {
    ROOMING, EVERYONE, ARTISTS
}
