package com.ozang.bestbefore_mvp.data.models

data class TimeCapsuleRoom(
    val id: String = java.util.UUID.randomUUID().toString(),
    val roomName: String,
    val capsuleDays: Int,
    val capsuleHours: Int,
    val notificationDays: Int,
    val notificationHours: Int,
    val isPublic: Boolean,
    val isCollaboration: Boolean = false
)
