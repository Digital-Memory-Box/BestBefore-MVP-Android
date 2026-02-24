package com.dmb.bestbefore.data.models

data class TimeCapsuleRoom(
    val id: String = java.util.UUID.randomUUID().toString(),
    val roomName: String,
    val capsuleDays: Int,
    val capsuleHours: Int,
    val capsuleMinutes: Int = 0,
    val notificationDays: Int,
    val notificationHours: Int,
    val notificationMinutes: Int = 0,
    val isPublic: Boolean,
    val isCollaboration: Boolean = false,
    val photos: List<String> = emptyList(),
    val dateCreated: Long = System.currentTimeMillis(),
    val unlockTime: Long = 0L, // Added for unlock logic
    val isSaved: Boolean = false // New field for Saved Rooms feature
)
