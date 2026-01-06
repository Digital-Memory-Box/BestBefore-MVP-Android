package com.ozang.bestbefore_mvp.ui.screens.hallway

import androidx.lifecycle.ViewModel
import com.ozang.bestbefore_mvp.data.models.HallwayCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HallwayViewModel : ViewModel() {

    private val _cards = MutableStateFlow(
        listOf(
            HallwayCard(title = "NYC Trip", timeCapsuleDays = 21),
            HallwayCard(title = "Daily Trip", timeCapsuleDays = 7),
            HallwayCard(title = "21 Days", timeCapsuleDays = 21),
            HallwayCard(title = "Foreign", timeCapsuleDays = 30),
            HallwayCard(title = "Travel", timeCapsuleDays = 14),
            HallwayCard(title = "Nature", timeCapsuleDays = 10)
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
