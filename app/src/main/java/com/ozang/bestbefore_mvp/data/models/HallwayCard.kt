package com.ozang.bestbefore_mvp.data.models

data class HallwayCard(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val timeCapsuleDays: Int = 21,
    val description: String = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor...",
    val imageUrl: String? = null
)
