package com.dmb.bestbefore.ui.screens.profile

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.api.RetrofitClient
import com.dmb.bestbefore.data.api.models.MemoryPreview
import com.dmb.bestbefore.data.api.models.PublicProfileDto
import com.dmb.bestbefore.data.api.models.PublicRoomDto
import com.dmb.bestbefore.data.models.HallwayCard
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.utils.AppErrorUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CreatorProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _profileState = MutableStateFlow<PublicProfileDto?>(null)
    val profileState: StateFlow<PublicProfileDto?> = _profileState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadProfile(context: Context, userId: String) {
        if (userId.isBlank()) {
            _error.value = "No user ID provided"
            _profileState.value = null
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            val fallbackProfile = cachedProfileFromHallway(userId)
            if (fallbackProfile != null) {
                _profileState.value = fallbackProfile
            } else {
                _profileState.value = null
            }

            _isLoading.value = fallbackProfile == null
            _error.value = null

            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                val token = firebaseUser?.getIdToken(false)?.await()?.token ?: ""
                Log.d("CreatorProfileVM", "loadProfile userId=$userId, hasToken=${token.isNotEmpty()}")

                var lastError: String? = null
                repeat(2) { attempt ->
                    val response = RetrofitClient.apiService.getPublicProfile("Bearer $token", userId)
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("CreatorProfileVM", "Profile loaded successfully for $userId")
                        if (body != null) {
                            _profileState.value = mergeWithCachedRoomPhotos(body, userId)
                            _error.value = null
                        } else if (_profileState.value == null) {
                            _error.value = "Server returned empty profile body"
                        }
                        return@launch
                    }

                    val errBody = response.errorBody()?.string()
                    lastError = if (response.code() in 500..599) AppErrorUtils.LOADING_ERROR else "Failed to load profile"
                    if (response.code() in 500..599 && attempt == 0) {
                        Log.w("CreatorProfileVM", "HTTP ${response.code()} for $userId, retrying")
                        delay(900)
                    } else {
                        Log.w("CreatorProfileVM", "HTTP Error ${response.code()} for $userId: $errBody")
                    }
                }

                if (_profileState.value == null) {
                    _error.value = lastError ?: "Failed to load profile"
                }
            } catch (e: IOException) {
                Log.w("CreatorProfileVM", "Network issue loading profile for $userId: ${e.message}")
                if (_profileState.value == null) {
                    _error.value = AppErrorUtils.userMessage(e)
                }
            } catch (e: CancellationException) {
                Log.d("CreatorProfileVM", "Profile load cancelled for $userId")
                throw e
            } catch (e: Exception) {
                Log.e("CreatorProfileVM", "Exception loading profile", e)
                if (_profileState.value == null) {
                    _error.value = AppErrorUtils.userMessage(e)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun cachedProfileFromHallway(userId: String): PublicProfileDto? {
        val cards = cachedCardsForUser(userId)

        if (cards.isEmpty()) return null

        val first = cards.first()
        val publicRooms = cards.map { card ->
            val photos = photosFromCard(card)
            PublicRoomDto(
                id = card.id,
                name = card.title,
                theme = card.themeColorHex,
                tags = card.tags,
                description = card.description.takeIf { it.isNotBlank() },
                photos = photos
            )
        }

        return PublicProfileDto(
            id = userId,
            name = first.ownerName ?: first.ownerEmail?.substringBefore("@"),
            userType = first.ownerUserType,
            bio = null,
            profileImageUrl = first.ownerProfilePic,
            profileImageData = null,
            roomingCount = 0,
            roomersCount = 0,
            memoriesCount = publicRooms.sumOf { it.photos.size },
            publicRooms = publicRooms
        )
    }

    private fun mergeWithCachedRoomPhotos(profile: PublicProfileDto, userId: String): PublicProfileDto {
        val cachedById = cachedCardsForUser(userId).associateBy { it.id }
        if (cachedById.isEmpty()) return profile

        return profile.copy(
            profileImageUrl = profile.profileImageUrl ?: cachedById.values.firstOrNull()?.ownerProfilePic,
            publicRooms = profile.publicRooms.map { room ->
                val cached = cachedById[room.id] ?: return@map room
                val cachedPhotos = photosFromCard(cached)
                room.copy(
                    photos = if (room.photos.isNotEmpty()) room.photos else cachedPhotos,
                    description = room.description ?: cached.description.takeIf { it.isNotBlank() },
                    tags = if (room.tags.isNotEmpty()) room.tags else cached.tags,
                    theme = room.theme ?: cached.themeColorHex
                )
            }
        )
    }

    private fun cachedCardsForUser(userId: String): List<HallwayCard> {
        return SessionManager(getApplication()).getHallwayCards()
            .filter { it.ownerId == userId }
            .distinctBy { it.id }
    }

    private fun photosFromCard(card: HallwayCard): List<MemoryPreview> {
        return when {
            card.photos.isNotEmpty() -> card.photos
            !card.imageUrl.isNullOrBlank() -> listOf(MemoryPreview(card.id, card.imageUrl))
            else -> emptyList()
        }
    }
}
