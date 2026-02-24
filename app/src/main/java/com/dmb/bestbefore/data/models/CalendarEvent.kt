package com.dmb.bestbefore.data.models

import java.util.Date

data class CalendarEvent(
    val id: String,
    val title: String,
    val startTime: Date,
    val endTime: Date,
    val location: String?,
    val description: String?
)
