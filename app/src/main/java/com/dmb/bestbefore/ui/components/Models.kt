package com.dmb.bestbefore.ui.components

import androidx.compose.ui.graphics.Color

enum class CollaboratorRole { VIEWER, CONTRIBUTOR }

data class CollaboratorX(
    val email: String,
    var role: CollaboratorRole
)

data class LinkedRoom(
    val roomId: String,
    val type: String
)

data class RoomObject(
    val id: String = "uuid",
    val name: String,
    val ownerEmail: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val imageName: String? = null,
    val isPrivate: Boolean = false,
    val isTimeCapsule: Boolean = false,
    val capsuleDurationDays: Int = 21,
    val capsuleDurationHours: Int = 0,
    val capsuleDurationMinutes: Int = 0,
    var unlockDate: Long? = null,
    var backgroundMusic: String? = null,
    val theme: String = "default",
    var expirationDate: Long? = null,
    var uploadStartDate: Long? = null,
    var rollingExpiryDays: Int = 0,
    var collaborators: MutableList<CollaboratorX> = mutableListOf(),
    val linkedRooms: List<LinkedRoom> = emptyList(),
    val previewImageQueries: List<String>? = null,
    val location: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val themeColor: Color
        get() = when (theme.lowercase()) {
            "ocean" -> Color(0xFF009688)
            "sunset" -> Color(0xFFFF9800)
            "forest" -> Color(0xFF4CAF50)
            "cyberpunk" -> Color(0xFF9C27B0)
            "default" -> Color(0xFF2196F3)
            else -> {
                try {
                    Color(android.graphics.Color.parseColor(if (theme.startsWith("#")) theme else "#$theme"))
                } catch (e: Exception) {
                    Color(0xFF2196F3)
                }
            }
        }

    val isLocked: Boolean
        get() = isTimeCapsule
}

enum class MemoryType(val icon: String) {
    PHOTO("photo.fill"),
    NOTE("doc.text.fill"),
    AUDIO("mic.fill"),
    VIDEO("film.fill"),
    MUSIC("music.note")
}
