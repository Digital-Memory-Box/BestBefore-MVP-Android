package com.dmb.bestbefore.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.dmb.bestbefore.data.models.AppNotification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("bestbefore_notifications", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        val json = prefs.getString("notifications_list", null)
        if (json != null) {
            val type = object : TypeToken<List<AppNotification>>() {}.type
            val list: List<AppNotification> = gson.fromJson(json, type)
            _notifications.value = list.sortedByDescending { it.timestamp }
        } else {
            _notifications.value = emptyList()
        }
        _unreadCount.value = prefs.getInt("unread_count", 0)
    }

    private fun saveNotifications(list: List<AppNotification>) {
        val json = gson.toJson(list)
        prefs.edit().putString("notifications_list", json).apply()
        _notifications.value = list.sortedByDescending { it.timestamp }
    }

    fun addNotification(notification: AppNotification) {
        val current = _notifications.value.toMutableList()
        current.add(notification)
        saveNotifications(current)
        val newCount = _unreadCount.value + 1
        _unreadCount.value = newCount
        prefs.edit().putInt("unread_count", newCount).apply()
    }

    fun removeNotification(notificationId: String) {
        val current = _notifications.value.toMutableList()
        current.removeAll { it.id == notificationId }
        saveNotifications(current)
    }

    fun clearAll() {
        saveNotifications(emptyList())
        _unreadCount.value = 0
        prefs.edit().putInt("unread_count", 0).apply()
    }

    fun markAllRead() {
        _unreadCount.value = 0
        prefs.edit().putInt("unread_count", 0).apply()
    }
}
