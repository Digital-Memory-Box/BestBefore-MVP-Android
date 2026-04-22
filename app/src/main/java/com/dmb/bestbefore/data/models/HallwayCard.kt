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
    val ownerName: String? = null,
    val ownerUserType: String? = null,
    val ownerProfilePic: String? = null,
    val collaboratorCount: Int = 0,
    val collaborators: List<com.dmb.bestbefore.data.api.models.UserDto> = emptyList(),
    val location: String? = null,
    val backgroundMusic: String? = null,
    val isViewerOnly: Boolean = false,
    val isOwnedByMe: Boolean = false,
    val isCollaborator: Boolean = false
)
