package com.dmb.bestbefore.utils

import android.net.Uri

object JoinLinkParser {
    private const val APP_LINK_HOST = "bestbefore.up.railway.app"

    data class ParsedJoinLink(
        val roomId: String? = null,
        val inviteToken: String? = null
    )

    fun parseIntentData(uri: Uri): ParsedJoinLink? = parseUri(uri)

    fun parseScannedText(raw: String): ParsedJoinLink? {
        val value = raw.trim()
        if (value.isEmpty()) return null

        if (value.startsWith("bestbefore:room:", ignoreCase = true)) {
            val roomId = value.substringAfterLast(':').trim('/').trim()
            return if (roomId.isNotEmpty()) ParsedJoinLink(roomId = roomId) else null
        }

        return runCatching { Uri.parse(value) }
            .getOrNull()
            ?.let { parseUri(it) }
    }

    private fun parseUri(uri: Uri): ParsedJoinLink? {
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase().orEmpty()
        val segments = uri.pathSegments.orEmpty()

        if (scheme == "https" && host == APP_LINK_HOST) {
            val route = segments.firstOrNull()?.lowercase().orEmpty()
            val value = segments.getOrNull(1)?.trim('/').orEmpty()
            if (value.isNotEmpty()) {
                return when (route) {
                    "join", "room" -> ParsedJoinLink(roomId = value)
                    "invite-join", "invite" -> ParsedJoinLink(inviteToken = value)
                    else -> null
                }
            }

            val roomFromQuery = uri.getQueryParameter("roomId")?.trim().orEmpty()
            if (roomFromQuery.isNotEmpty()) return ParsedJoinLink(roomId = roomFromQuery)

            val tokenFromQuery = uri.getQueryParameter("token")?.trim().orEmpty()
            if (tokenFromQuery.isNotEmpty()) return ParsedJoinLink(inviteToken = tokenFromQuery)
        }

        if (scheme == "bestbefore") {
            val value = segments.firstOrNull()?.trim('/').orEmpty()
            if (value.isNotEmpty()) {
                return when (host) {
                    "room", "join" -> ParsedJoinLink(roomId = value)
                    "invite" -> ParsedJoinLink(inviteToken = value)
                    else -> null
                }
            }
        }

        return null
    }
}

