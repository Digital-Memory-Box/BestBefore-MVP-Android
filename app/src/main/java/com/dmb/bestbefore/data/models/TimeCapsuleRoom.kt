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
    val isCollaboration: Boolean = false,     // stores isTimeCapsule flag from backend
    val photos: List<com.dmb.bestbefore.data.api.models.MemoryPreview> = emptyList(),
    val dateCreated: Long = System.currentTimeMillis(),
    val unlockTime: Long = 0L,
    val scheduledClosureTime: Long = 0L,      // 0 = no scheduled closure
    val uploadStartDate: Long = 0L,           // 0 = uploads allowed immediately
    val isSaved: Boolean = false,
    val theme: String = "Default",
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val music: String = "None",
    val rollingExpiration: String = "Never",
    val isViewerOnly: Boolean = false,
    val connectedRooms: List<String> = emptyList(),
    val isOwnedByMe: Boolean = false,
    val isCollaborator: Boolean = false,
    val ownerUserType: String? = null
)
