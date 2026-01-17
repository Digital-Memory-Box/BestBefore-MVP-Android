package com.dmb.bestbefore.ui.screens.hallway

import androidx.lifecycle.ViewModel
import com.dmb.bestbefore.data.models.HallwayCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HallwayViewModel : ViewModel() {

    private val _cards = MutableStateFlow(
        listOf(
            HallwayCard(
                 title = "NYC Trip", 
                 timeCapsuleDays = 21,
                 description = "A memorable journey through the concrete jungle where dreams are made of. From Times Square to Central Park.",
                 imageUrl = "https://picsum.photos/seed/nyc/400/600"
            ),
            HallwayCard(
                title = "Daily Trip", 
                timeCapsuleDays = 7,
                description = "Capturing everyday moments that turn into lifetime memories.",
                imageUrl = "https://picsum.photos/seed/daily/400/600"
            ),
            HallwayCard(
                title = "Summer Vibes", 
                timeCapsuleDays = 30,
                description = "Sun, sand, and sea. Reliving the best summer moments.",
                imageUrl = "https://picsum.photos/seed/summer/400/600"
            ),
            HallwayCard(
                title = "Mountain Hike", 
                timeCapsuleDays = 14,
                description = "Scaling new heights and breathing in the fresh mountain air.",
                imageUrl = "https://picsum.photos/seed/mountain/400/600"
            ),
            HallwayCard(
                title = "City Lights", 
                timeCapsuleDays = 10,
                description = "The city never sleeps, and neither do the memories we make.",
                imageUrl = "https://picsum.photos/seed/city/400/600"
            )
        )
    )
    val cards: StateFlow<List<HallwayCard>> = _cards.asStateFlow()

    private val _selectedCardIndex = MutableStateFlow(0)
    val selectedCardIndex: StateFlow<Int> = _selectedCardIndex.asStateFlow()

    private val _currentTab = MutableStateFlow(BottomTab.EVERYONE)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

    fun selectCard(index: Int) {
        _selectedCardIndex.value = index
    }

    fun selectTab(tab: BottomTab) {
        _currentTab.value = tab
    }
}

enum class BottomTab {
    ROOMING, EVERYONE, ARTISTS
}
