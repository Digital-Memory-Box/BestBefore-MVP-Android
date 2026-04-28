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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

class HallwayViewModel(application: Application) : AndroidViewModel(application) {

    private val roomRepository = RoomRepository()
    private val aiRepository = com.dmb.bestbefore.data.ai.AiRepository()
    private val authRepository: com.dmb.bestbefore.data.repository.AuthRepository by lazy {
        com.dmb.bestbefore.data.repository.AuthRepository(getApplication())
    }

    // ── Server warm-up status ─────────────────────────────────────────────────
    // Railway free tier cold-starts take 20–60 s. We ping /health first so the
    // user sees a meaningful message instead of a blank spinner.
    enum class ServerStatus { CONNECTING, WARMING_UP, READY }
    private val _serverStatus = MutableStateFlow(ServerStatus.CONNECTING)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

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

    // Populated by the backend semantic search when searchQuery >= 3 chars.
    private val _semanticSearchCards = MutableStateFlow<List<HallwayCard>>(emptyList())
    val semanticSearchCards: StateFlow<List<HallwayCard>> = _semanticSearchCards.asStateFlow()

    private val _isSemanticSearching = MutableStateFlow(false)
    val isSemanticSearching: StateFlow<Boolean> = _isSemanticSearching.asStateFlow()

    val filteredCards: StateFlow<List<HallwayCard>> = combine(
        _cards, _selectedFilterTag, _searchQuery, _semanticSearchCards
    ) { cards, tag, query, semanticCards ->
        val normalizedQuery = query.trim()
        // Use backend semantic results when available, else fall back to local title filter.
        val sourceCards = if (normalizedQuery.length >= 3 && semanticCards.isNotEmpty()) {
            semanticCards
        } else {
            cards
        }
        sourceCards.filter { card ->
            val matchesTagFilter = tag.isNullOrBlank() || card.tags.any { it.equals(tag, ignoreCase = true) }
            val matchesSearch = if (normalizedQuery.isBlank() || (normalizedQuery.length >= 3 && semanticCards.isNotEmpty())) {
                true
            } else {
                card.title.contains(normalizedQuery, ignoreCase = true) ||
                        card.description.contains(normalizedQuery, ignoreCase = true) ||
                        card.tags.any { it.contains(normalizedQuery, ignoreCase = true) }
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
    // AI-ranked room IDs from /rooms/discover/initial, in descending similarity order.
    private var aiDiscoveryRankedIds: List<String> = emptyList()

    // ── Similar Rooms Mode ──────────────────────────────────────────────
    private val _similarModeSource = MutableStateFlow<HallwayCard?>(null)
    val similarModeSource: StateFlow<HallwayCard?> = _similarModeSource.asStateFlow()

    private val _similarityScores = MutableStateFlow<Map<String, Int>>(emptyMap())

    fun enterSimilarMode(card: HallwayCard) {
        _similarModeSource.value = card
        _selectedCardIndex.value = 0
        _activePagerPage.value = 0

        viewModelScope.launch {
            // Ana Backend'i aradan çıkarıyoruz!

            // AI için profil bilgisini hızlıca çekiyoruz (Giriş yapılmamışsa boş yollarız)
            val userDto = authRepository.getMe().getOrNull() ?: com.dmb.bestbefore.data.api.models.UserDto(
                id = "",
                email = "",
                name = ""
            )
            // Aranacak tüm aday odalar
            val allAvailable = (myRoomsList + discoverRoomsList).distinctBy { it.id }
            val fallbackScores = buildLocalSimilarityScores(card, allAvailable)

            // Eski sorunlu kod: val result = roomRepository.getRoomSuggestions(card.id)
            // YENİ KOD: Doğrudan kendi bağladığımız AI servisine soruyoruz:
            val result = withTimeoutOrNull(8_000L) {
                aiRepository.getPersonalisedSuggestions(
                    user = userDto,
                    candidateRooms = allAvailable,
                    sourceRoomId = card.id,
                    sourceRoom = com.dmb.bestbefore.data.api.models.RoomDto(
                        id = card.id,
                        name = card.title,
                        tags = card.tags,
                        isPrivate = false,
                        isTimeCapsule = false,
                        description = card.description,
                        ownerEmail = card.ownerEmail,
                        createdAt = null
                    )
                )
            } ?: Result.failure(Exception("Suggestions timed out"))

            result.onSuccess { response ->
                // Skoru 0'dan büyük olan, AI'ın "benzer" bulduğu odaları filtrele
                val aiScores = response.suggestions
                    .associate { it.targetRoomId to it.score }
                    .filterValues { it > 0 }
                _similarityScores.value = aiScores.ifEmpty { fallbackScores }
                filterCards(_currentTab.value)
                if (aiScores.isEmpty()) {
                    Log.w("HallwayViewModel", "Similar rooms using local fallback: empty AI response")
                }
            }.onFailure {
                _similarityScores.value = fallbackScores
                filterCards(_currentTab.value)
                Log.w("HallwayViewModel", "Similar rooms using local fallback: ${it.message}")
            }
        }
    }

    fun exitSimilarMode() {
        _similarModeSource.value = null
        _similarityScores.value = emptyMap()
        filterCards(_currentTab.value)
    }

    private fun buildLocalSimilarityScores(
        source: HallwayCard,
        rooms: List<com.dmb.bestbefore.data.api.models.RoomDto>
    ): Map<String, Int> {
        val sourceTags = source.tags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        val sourceWords = tokenize("${source.title} ${source.description} ${source.tags.joinToString(" ")}")
        val sourceTheme = source.themeColorHex?.trim()?.lowercase()
        val sourceMusic = source.backgroundMusic?.trim()?.lowercase()

        return rooms
            .asSequence()
            .filter { it.id != source.id && !_ignoredRoomIds.value.contains(it.id) }
            .map { room ->
                val roomTags = room.tags.orEmpty().map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
                val roomWords = tokenize("${room.name} ${room.description.orEmpty()} ${room.generatedDescription.orEmpty()} ${roomTags.joinToString(" ")}")
                val sharedTags = sourceTags.intersect(roomTags).size
                val sharedWords = sourceWords.intersect(roomWords).size
                val themeScore = if (!sourceTheme.isNullOrBlank() && sourceTheme == room.theme?.trim()?.lowercase()) 12 else 0
                val musicScore = if (!sourceMusic.isNullOrBlank() && sourceMusic == room.backgroundMusic?.trim()?.lowercase()) 6 else 0
                val textScore = (sharedWords * 4).coerceAtMost(28)
                val tagScore = (sharedTags * 18).coerceAtMost(54)
                val score = (18 + tagScore + textScore + themeScore + musicScore).coerceIn(1, 100)
                room.id to score
            }
            .sortedByDescending { it.second }
            .take(24)
            .toMap()
    }

    private fun tokenize(text: String): Set<String> {
        return text
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .toSet()
    }

    fun connectRoom(targetCard: HallwayCard) {
        val sourceCard = _similarModeSource.value ?: return
        viewModelScope.launch {
            val result = roomRepository.acceptSuggestion(sourceCard.id, targetCard.id)
            result.onSuccess {
                Log.d("HallwayViewModel", "Connected ${sourceCard.title} to ${targetCard.title}")
                exitSimilarMode()
                android.widget.Toast.makeText(getApplication(), "Rooms connected!", android.widget.Toast.LENGTH_SHORT).show()
            }
            result.onFailure { e ->
                Log.e("HallwayViewModel", "connectRoom failed: ${e.message}")
                android.widget.Toast.makeText(getApplication(), "Failed to connect rooms", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    init {
        loadCachedData()
        fetchRooms()
        watchSearchQueryForSemanticSearch()
    }

    private fun loadCachedData() {
        val sessionManager = com.dmb.bestbefore.data.local.SessionManager(getApplication())
        val cachedCards = sessionManager.getHallwayCards()
        if (cachedCards.isNotEmpty()) {
            _cards.value = cachedCards
            // We still show initial loading if the cache is empty
            _isInitialLoading.value = false
        }
    }

    private fun fetchRooms() {
        viewModelScope.launch {
            _isInitialLoading.value = true
            val t0 = System.currentTimeMillis()
            fun ms() = System.currentTimeMillis() - t0
            Log.i(TAG_PERF, "━━━ fetchRooms START ━━━")

            // ── Server warm-up ping ───────────────────────────────────────────
            // Railway free tier cold-starts in 20–60 s. Fire /health first.
            // After 2 s with no response, flip to WARMING_UP so the UI can tell
            // the user the server is starting rather than showing a blank screen.
            val warmupJob = launch {
                kotlinx.coroutines.delay(2_000)
                if (_serverStatus.value == ServerStatus.CONNECTING) {
                    _serverStatus.value = ServerStatus.WARMING_UP
                    Log.i(TAG_PERF, "[${ms()}ms] server still cold — showing warm-up message")
                }
            }
            val pingT0 = System.currentTimeMillis()
            try {
                val pingResult = withTimeoutOrNull(90_000L) {
                    com.dmb.bestbefore.data.api.RetrofitClient.apiService.health()
                }
                val pingMs = System.currentTimeMillis() - pingT0
                if (pingResult?.isSuccessful == true) {
                    Log.i(TAG_PERF, "[${ms()}ms] /health OK in ${pingMs}ms — server is warm ✅")
                } else {
                    Log.w(TAG_PERF, "[${ms()}ms] /health ${pingResult?.code()} in ${pingMs}ms — proceeding anyway")
                }
            } catch (e: Exception) {
                Log.e(TAG_PERF, "[${ms()}ms] /health exception in ${System.currentTimeMillis() - pingT0}ms: ${e.message}")
            } finally {
                warmupJob.cancel()
                _serverStatus.value = ServerStatus.READY
            }
            Log.i(TAG_PERF, "[${ms()}ms] warm-up done — launching data fetches")

            try {
                // Launch all three in parallel and record when each fires
                Log.i(TAG_PERF, "[${ms()}ms] launching getRooms + getDiscoverRooms + getMe in parallel")
                val myDeferred      = async { roomRepository.getRooms() }
                val discoverDeferred = async { roomRepository.getDiscoverRooms() }
                val meDeferred      = async { authRepository.getMe() }

                val myResult      = myDeferred.await()
                Log.i(TAG_PERF, "[${ms()}ms] getRooms DONE")
                val discoverResult = discoverDeferred.await()
                Log.i(TAG_PERF, "[${ms()}ms] getDiscoverRooms DONE")
                val meResult      = meDeferred.await()
                Log.i(TAG_PERF, "[${ms()}ms] getMe DONE  — all 3 parallel calls complete")

                if (myResult.isSuccess) {
                    val rooms = myResult.getOrThrow()
                    val photoStats = rooms.photoStats()
                    Log.i(TAG_PERF, "[${ms()}ms] /rooms → ${rooms.size} rooms | $photoStats")
                    myRoomsList = rooms
                } else {
                    Log.e(TAG_PERF, "[${ms()}ms] /rooms FAILED: ${myResult.exceptionOrNull()?.message}")
                }

                if (discoverResult.isSuccess) {
                    val rooms = discoverResult.getOrThrow()
                    val photoStats = rooms.photoStats()
                    Log.i(TAG_PERF, "[${ms()}ms] /rooms/discover → ${rooms.size} rooms | $photoStats")
                    discoverRoomsList = rooms
                } else {
                    Log.e(TAG_PERF, "[${ms()}ms] /rooms/discover FAILED: ${discoverResult.exceptionOrNull()?.message}")
                }

                // Sync ignored/saved rooms from profile + kick off AI discovery
                if (meResult.isSuccess) {
                    val userDto = meResult.getOrThrow()
                    _ignoredRoomIds.value = userDto.ignoredRoomIds?.toSet() ?: emptySet()

                    // AI discovery: fire-and-forget so cards render immediately at default order.
                    // When the AI result arrives it silently re-sorts the EVERYONE tab.
                    val hasTags = !userDto.preferredTags.isNullOrEmpty()
                    val hasBio = !userDto.bio.isNullOrBlank()
                    val hasName = !userDto.name.isNullOrBlank()
                    if (hasTags || hasBio || hasName) {
                        launch {
                            roomRepository.getInitialDiscoveryRooms(
                                preferredTags = userDto.preferredTags ?: emptyList(),
                                userLat = userDto.lastLat,
                                userLon = userDto.lastLon,
                                userName = userDto.name ?: "",
                                bio = userDto.bio ?: ""
                            ).onSuccess { response ->
                                aiDiscoveryRankedIds = response.results.map { it.roomId }
                                filterCards(_currentTab.value)
                                Log.d("HallwayViewModel", "AI discovery: ${response.count} rooms re-ranked (strategy=${response.strategy})")
                            }.onFailure { e ->
                                Log.w("HallwayViewModel", "AI discovery failed, default order kept: ${e.message}")
                            }
                        }
                    }
                    
                    // We need to fetch the actual cards for these IDs to populate _ignoredRoomCards
                    val allRooms = (myRoomsList + discoverRoomsList).distinctBy { it.id }
                    _ignoredRoomCards.value = allRooms.filter { _ignoredRoomIds.value.contains(it.id) }.map { room ->
                        HallwayCard(
                            id = room.id,
                            title = room.name,
                            timeCapsuleDays = room.capsuleDurationDays,
                            description = room.description ?: room.generatedDescription ?: "",
                            imageUrl = room.photos?.firstOrNull()?.url,
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
                            imageUrl = room.photos?.firstOrNull()?.url,
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
                
                Log.i(TAG_PERF, "[${ms()}ms] calling filterCards")
                filterCards(_currentTab.value)
                
                // Persist the filtered cards to cache for the next app launch
                val sessionManager = com.dmb.bestbefore.data.local.SessionManager(getApplication())
                sessionManager.saveHallwayCards(_cards.value)
                
                Log.i(TAG_PERF, "[${ms()}ms] filterCards done → ${_cards.value.size} cards visible")
            } catch (e: Exception) {
                Log.e(TAG_PERF, "[${ms()}ms] fetchRooms EXCEPTION: ${e.message}", e)
            } finally {
                Log.i(TAG_PERF, "━━━ fetchRooms END  total=${ms()}ms ━━━")
                _isInitialLoading.value = false
            }
            
            // Tags fetch — sequential after cards are visible (non-blocking for UI)
            try {
                val tagsT0 = System.currentTimeMillis()
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
                        Log.i(TAG_PERF, "/tags → ${parsedTags.size} tags in ${System.currentTimeMillis() - tagsT0}ms")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG_PERF, "tags fetch unavailable: ${e.message}")
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
        val accepted = room.collaborators?.any { element ->
            if (element.isJsonPrimitive) {
                element.asString.equals(currentUserEmail, ignoreCase = true)
            } else if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                element.asJsonObject.get("email").asString.equals(currentUserEmail, ignoreCase = true)
            } else {
                false
            }
        } == true
        val pending = room.pendingCollaborators?.any { it.equals(currentUserEmail, ignoreCase = true) } == true
        return accepted || pending
    }

    private fun isViewer(room: com.dmb.bestbefore.data.api.models.RoomDto, currentUserEmail: String): Boolean {
        val accepted = room.viewers?.any { element ->
            if (element.isJsonPrimitive) {
                element.asString.equals(currentUserEmail, ignoreCase = true)
            } else if (element.isJsonObject && element.asJsonObject.has("email") && !element.asJsonObject.get("email").isJsonNull) {
                element.asJsonObject.get("email").asString.equals(currentUserEmail, ignoreCase = true)
            } else {
                false
            }
        } == true
        val pending = room.pendingViewers?.any { it.equals(currentUserEmail, ignoreCase = true) } == true
        return accepted || pending
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
                    allAvailableRooms.filter { room ->
                        val isOwner = room.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true
                        val isCollaborator = isCollaborator(room, currentUserEmail)
                        val isViewer = isViewer(room, currentUserEmail)
                        !isOwner && (isCollaborator || isViewer)
                    }
                }
                BottomTab.EVERYONE -> {
                    val base = allAvailableRooms.filter {
                        val isOwner = it.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true
                        !isOwner &&
                        isRoomPublic(it) &&
                        !isArtistRoom(it) &&
                        !_ignoredRoomIds.value.contains(it.id)
                    }
                    if (aiDiscoveryRankedIds.isNotEmpty()) {
                        val rankMap = aiDiscoveryRankedIds.withIndex().associate { (i, id) -> id to i }
                        base.sortedBy { rankMap[it.id] ?: Int.MAX_VALUE }
                    } else base
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
                imageUrl = room.photos?.firstOrNull()?.url,
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
                        } catch (_: Exception) { null }
                    } else null
                } ?: emptyList(),
                location = null,
                backgroundMusic = room.backgroundMusic,
                isViewerOnly = isViewerOnly,
                isOwnedByMe = isOwner,
                isCollaborator = isCollaborator,
                unlockDate = room.unlockDate
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

    // Debounced semantic search: fires when query reaches 3+ chars, clears on shorter input.
    // Debounced semantic search: fires when query reaches 3+ chars, clears on shorter input.
    private fun watchSearchQueryForSemanticSearch() {
        viewModelScope.launch {
            @Suppress("OPT_IN_USAGE")
            searchQuery.debounce(500L).collect { query ->
                if (query.length >= 3) {
                    _isSemanticSearching.value = true
                    val allAvailable = (myRoomsList + discoverRoomsList).distinctBy { it.id }

                    // DEĞİŞEN KISIM: Ana backend'i atlayıp doğrudan AI sunucusuna (Railway) soruyoruz!
                    // topK = 10 diyerek yapay zekadan en alakalı 10 odayı getirmesini istiyoruz.
                    val result = aiRepository.semanticSearch(query, allAvailable, topK = 10)

                    result.onSuccess { response ->
                        val ranked = response.results.map { searchResult ->
                            allAvailable.find { it.id == searchResult.roomId }?.let { room ->
                                val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
                                val isOwner = room.ownerEmail?.equals(currentUserEmail, ignoreCase = true) == true
                                val isCollabFlag = isCollaborator(room, currentUserEmail)
                                val isViewerFlag = isViewer(room, currentUserEmail)
                                HallwayCard(
                                    id = room.id,
                                    title = room.name,
                                    timeCapsuleDays = room.capsuleDurationDays,
                                    description = if (!room.description.isNullOrBlank()) room.description
                                    else (room.generatedDescription ?: ""),
                                    imageUrl = room.photos?.firstOrNull()?.url,
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
                                    backgroundMusic = room.backgroundMusic,
                                    isViewerOnly = !isOwner && !isCollabFlag && (isViewerFlag || isRoomPublic(room)),
                                    isOwnedByMe = isOwner,
                                    isCollaborator = isCollabFlag
                                )
                            }
                        }.filterNotNull()
                        _semanticSearchCards.value = ranked
                        Log.d("HallwayViewModel", "Semantic search: ${ranked.size} results for '$query'")
                    }.onFailure { e ->
                        Log.w("HallwayViewModel", "Semantic search failed, keeping local filter: ${e.message}")
                        _semanticSearchCards.value = emptyList()
                    }
                    _isSemanticSearching.value = false
                } else {
                    _semanticSearchCards.value = emptyList()
                }
            }
        }
    }

    fun selectCard(index: Int) {
        if (index >= 0 && index < _cards.value.size) {
            _selectedCardIndex.value = index
            _activePagerPage.value = index
            _areCollaboratorsExpanded.value = false
            trackViewInteraction(index)
        }
    }

    private fun trackViewInteraction(index: Int) {
        val card = _cards.value.getOrNull(index) ?: return
        viewModelScope.launch {
            roomRepository.trackInteraction(
                roomId = card.id,
                dwellTimeSeconds = 0L,
                type = "VIEW"
            )
        }
    }

    fun selectTab(tab: BottomTab) {
        _currentTab.value = tab
        _selectedCardIndex.value = 0
        _activePagerPage.value = 0
        _selectedFilterTag.value = null
        _searchQuery.value = ""
        _semanticSearchCards.value = emptyList()
        _areCollaboratorsExpanded.value = false
        filterCards(tab)
    }

    fun setSelectedFilterTag(tag: String?) {
        _selectedFilterTag.value = tag
        _searchQuery.value = tag.orEmpty()
        _selectedCardIndex.value = 0
        _activePagerPage.value = 0
        _areCollaboratorsExpanded.value = false
        if (tag.isNullOrBlank()) _semanticSearchCards.value = emptyList()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _selectedCardIndex.value = 0
        _activePagerPage.value = 0
        _areCollaboratorsExpanded.value = false
        if (query.length < 3) _semanticSearchCards.value = emptyList()
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

    /** Called by ProfileScreen after a room's memories load and a cover photo is found. */
    fun updateCardCover(roomId: String, photoUrl: String) {
        if (photoUrl.isBlank()) return
        val preview = com.dmb.bestbefore.data.api.models.MemoryPreview("cover", photoUrl)
        fun patch(cards: List<HallwayCard>): List<HallwayCard> = cards.map { card ->
            if (card.id != roomId) card
            else card.copy(
                imageUrl = photoUrl,
                photos = listOf(preview) + card.photos.filter { it.id != "cover" }
            )
        }
        _cards.value = patch(_cards.value)
        _savedRoomCards.value = patch(_savedRoomCards.value)
        com.dmb.bestbefore.data.local.SessionManager(getApplication()).saveHallwayCards(_cards.value)
    }

    // Pull-to-refresh support
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun deleteMemory(roomId: String, memoryId: String) {
        viewModelScope.launch {
            val result = roomRepository.deleteMemory(roomId, memoryId)
            if (result.isSuccess) {
                Log.d("HallwayViewModel", "Successfully deleted memory $memoryId")
                refreshRooms() // Trigger full refresh
            } else {
                Log.e("HallwayViewModel", "Failed to delete memory: ${result.exceptionOrNull()?.message}")
            }
        }
    }

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

// ── Perf helpers ──────────────────────────────────────────────────────────────

private const val TAG_PERF = "BB_PERF"

/**
 * Summarises the photo payload of a list of RoomDto objects so we can immediately
 * see whether photos are stored as raw base64 (large) or as CDN URLs (small).
 *
 * Example output:
 *   "12 rooms, 47 photos — 43 base64 ⚠ (avg ~38 KB each), 4 URLs"
 */
private fun List<com.dmb.bestbefore.data.api.models.RoomDto>.photoStats(): String {
    val allUrls = flatMap { it.photos ?: emptyList() }.map { it.url }
    if (allUrls.isEmpty()) return "no photos"

    val base64Photos = allUrls.filter { it.startsWith("data:") }
    val urlPhotos    = allUrls.filter { it.startsWith("http") }
    val otherPhotos  = allUrls.size - base64Photos.size - urlPhotos.size

    val avgBase64Kb = if (base64Photos.isNotEmpty()) {
        base64Photos.sumOf { it.length }.toLong() / base64Photos.size / 1024
    } else {
        0L
    }

    val b64Warn = if (base64Photos.isNotEmpty()) " ⚠ BASE64" else ""
    return buildString {
        append("${allUrls.size} photos")
        if (base64Photos.isNotEmpty()) append(" | ${base64Photos.size} base64$b64Warn ~${avgBase64Kb}KB avg")
        if (urlPhotos.isNotEmpty())    append(" | ${urlPhotos.size} URLs")
        if (otherPhotos > 0)           append(" | $otherPhotos other")
    }
}
