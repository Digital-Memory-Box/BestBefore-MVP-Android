package com.dmb.bestbefore.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.ui.components.RoomObject
import com.dmb.bestbefore.data.api.models.CreateRoomRequest
import com.dmb.bestbefore.data.api.models.RoomDto
import com.dmb.bestbefore.data.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoomViewModel(private val repository: RoomRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _rooms = MutableStateFlow<List<RoomObject>>(emptyList())
    val rooms: StateFlow<List<RoomObject>> = _rooms.asStateFlow()

    fun fetchRooms() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.getRooms()
            if (result.isSuccess) {
                val dtos = result.getOrDefault(emptyList())
                _rooms.value = dtos.map { dtoToObj(it) }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to fetch rooms"
            }
            _isLoading.value = false
        }
    }

    private fun dtoToObj(dto: RoomDto): RoomObject {
        return RoomObject(
            id = dto.id,
            name = dto.name,
            ownerEmail = dto.ownerEmail,
            description = dto.description,
            tags = dto.tags ?: emptyList(),
            isPrivate = dto.isPrivate,
            isTimeCapsule = dto.isTimeCapsule,
            capsuleDurationDays = dto.capsuleDurationDays,
            capsuleDurationHours = dto.capsuleDurationHours,
            capsuleDurationMinutes = dto.capsuleDurationMinutes,
            backgroundMusic = dto.backgroundMusic,
            theme = dto.theme ?: "default",
            rollingExpiryDays = dto.rollingExpiryDays,
            // Add basic mapping logic if needed
        )
    }

    fun createRoom(request: CreateRoomRequest, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val roomId = repository.createRoom(request)
                fetchRooms() // Refresh the list
                onSuccess(roomId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to create room"
            }
            _isLoading.value = false
        }
    }
}

class RoomViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoomViewModel(RoomRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
