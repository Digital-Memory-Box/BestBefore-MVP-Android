package com.dmb.bestbefore.ui.screens.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmb.bestbefore.data.models.AppNotification
import com.dmb.bestbefore.data.repository.NotificationRepository
import com.dmb.bestbefore.data.repository.RoomRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationRepository = NotificationRepository(application)
    private val roomRepository = RoomRepository()

    val notifications: StateFlow<List<AppNotification>> = notificationRepository.notifications

    init {
        refresh()
    }

    fun removeNotification(id: String) {
        notificationRepository.removeNotification(id)
    }

    fun clearAll() {
        notificationRepository.clearAll()
    }

    fun acceptInvite(roomId: String, notificationId: String) {
        viewModelScope.launch {
            val result = roomRepository.acceptInvite(roomId)
            if (result.isSuccess) {
                removeNotification(notificationId)
            }
        }
    }

    fun declineInvite(roomId: String, notificationId: String) {
        viewModelScope.launch {
            val result = roomRepository.declineInvite(roomId)
            if (result.isSuccess) {
                removeNotification(notificationId)
            }
        }
    }

    fun approveJoinRequest(roomId: String, requesterEmail: String, notificationId: String) {
        viewModelScope.launch {
            val result = roomRepository.approveJoinRequest(roomId, requesterEmail)
            if (result.isSuccess) {
                android.widget.Toast.makeText(getApplication(), "Join request approved!", android.widget.Toast.LENGTH_SHORT).show()
                removeNotification(notificationId)
            } else {
                android.widget.Toast.makeText(getApplication(), "Failed to approve: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun denyJoinRequest(roomId: String, requesterEmail: String, notificationId: String) {
        viewModelScope.launch {
            val result = roomRepository.denyJoinRequest(roomId, requesterEmail)
            if (result.isSuccess) {
                android.widget.Toast.makeText(getApplication(), "Join request denied.", android.widget.Toast.LENGTH_SHORT).show()
                removeNotification(notificationId)
            } else {
                android.widget.Toast.makeText(getApplication(), "Failed to deny: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val result = notificationRepository.getNotifications()
            result.onSuccess { remote ->
                notificationRepository.mergeNotifications(remote)
            }
        }
    }
}
