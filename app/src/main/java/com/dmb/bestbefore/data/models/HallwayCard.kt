package com.dmb.bestbefore.data.models

data class HallwayCard(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val timeCapsuleDays: Int = 21,
    val description: String = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do usermod temper...",
    val imageUrl: String? = null,
    val photos: List<String> = emptyList(),
    val themeColorHex: String? = null,
    val tags: List<String> = emptyList(),
    val ownerEmail: String? = null,
    val collaboratorCount: Int = 0,
    val location: String? = null,
    val backgroundMusic: String? = null
)
