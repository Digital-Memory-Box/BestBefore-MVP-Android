package com.dmb.bestbefore.data.models

import android.net.Uri

data class MemoryItem(
    val uri: Uri?,
    val date: String,
    val time: String,
    var caption: String = "Edit Caption"
)
