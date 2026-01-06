package com.ozang.bestbefore_mvp.data.models

import android.net.Uri

data class MemoryItem(
    val uri: Uri?,
    val date: String,
    val time: String,
    var caption: String = "Edit Caption"
)
