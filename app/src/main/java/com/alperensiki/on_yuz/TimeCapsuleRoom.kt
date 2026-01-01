package com.alperensiki.on_yuz

data class TimeCapsuleRoom(
    val roomName: String,
    val capsuleDays: Int,
    val capsuleHours: Int,
    val notificationDays: Int,
    val notificationHours: Int,
    val isPublic: Boolean
)