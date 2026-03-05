package com.dmb.bestbefore.ui.screens.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.models.AppNotification
import com.dmb.bestbefore.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationRepository = NotificationRepository(application)

    val notifications: StateFlow<List<AppNotification>> = notificationRepository.notifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeNotification(id: String) {
        notificationRepository.removeNotification(id)
    }

    fun clearAll() {
        notificationRepository.clearAll()
    }

    fun acceptInvite(roomId: String, notificationId: String) {
        viewModelScope.launch {
            val result = com.dmb.bestbefore.data.repository.RoomRepository().acceptInvite(roomId)
            if (result.isSuccess) {
                removeNotification(notificationId)
            }
        }
    }

    fun declineInvite(roomId: String, notificationId: String) {
        viewModelScope.launch {
            val result = com.dmb.bestbefore.data.repository.RoomRepository().declineInvite(roomId)
            if (result.isSuccess) {
                removeNotification(notificationId)
            }
        }
    }

    fun refresh() {
        // Notifications are stored locally via SharedPreferences and already reactive via StateFlow.
        // A pull-to-refresh simply triggers a no-op read; the flow will emit the latest value.
    }
}
