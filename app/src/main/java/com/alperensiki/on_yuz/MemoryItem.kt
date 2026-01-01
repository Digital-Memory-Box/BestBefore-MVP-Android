package com.alperensiki.on_yuz

data class MemoryItem(
    val uri: android.net.Uri?,
    val date: String,
    val time: String,
    var caption: String = "Edit Caption"
)
