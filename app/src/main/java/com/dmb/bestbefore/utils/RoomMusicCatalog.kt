package com.dmb.bestbefore.utils

import com.dmb.bestbefore.data.api.models.SoundCloudTrack
import kotlin.math.absoluteValue

object RoomMusicCatalog {
    const val NONE = "None"
    private const val PRESET_PREFIX = "preset:"
    private const val CUSTOM_PREFIX = "custom:"

    data class Preset(
        val key: String,
        val title: String,
        val artist: String,
        val streamUrl: String
    )

    val presets = listOf(
        Preset(
            key = "dreamy_synth",
            title = "Dreamy Synth",
            artist = "SoundHelix",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        ),
        Preset(
            key = "chill_cafe",
            title = "Chill Cafe",
            artist = "SoundHelix",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        ),
        Preset(
            key = "minimal_piano",
            title = "Minimal Piano",
            artist = "SoundHelix",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
        ),
        Preset(
            key = "vaporwave",
            title = "Vaporwave",
            artist = "SoundHelix",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
        )
    )

    fun presetValue(key: String): String = "$PRESET_PREFIX$key"

    fun customValue(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.isBlank()) NONE else "$CUSTOM_PREFIX$trimmed"
    }

    fun customUrl(value: String): String? {
        return value.removePrefix(CUSTOM_PREFIX).takeIf { value.startsWith(CUSTOM_PREFIX) && it.isNotBlank() }
    }

    fun isNone(value: String?): Boolean {
        return value.isNullOrBlank() || value == NONE
    }

    fun isSoundCloudUrl(value: String?): Boolean {
        val url = customUrl(value.orEmpty()) ?: value.orEmpty()
        return url.startsWith("https://soundcloud.com/", ignoreCase = true)
    }

    fun displayName(value: String?): String {
        if (isNone(value)) return NONE
        val raw = value.orEmpty()
        presetFromValue(raw)?.let { return it.title }
        customUrl(raw)?.let { return it }
        presets.firstOrNull { it.title.equals(raw, ignoreCase = true) }?.let { return it.title }
        return raw
    }

    fun trackForValue(value: String?): SoundCloudTrack? {
        if (isNone(value) || isSoundCloudUrl(value)) return null

        val raw = value.orEmpty()
        val preset = presetFromValue(raw) ?: presets.firstOrNull { it.title.equals(raw, ignoreCase = true) }
        if (preset != null) {
            return SoundCloudTrack(
                id = preset.key.hashCode().toLong().absoluteValue,
                title = preset.title,
                artist = preset.artist,
                duration = 0L,
                streamUrl = preset.streamUrl,
                artworkUrl = null
            )
        }

        val custom = customUrl(raw) ?: raw.takeIf { it.startsWith("http", ignoreCase = true) }
        return custom?.let { url ->
            SoundCloudTrack(
                id = url.hashCode().toLong().absoluteValue,
                title = "Custom Room Music",
                artist = "BestBefore",
                duration = 0L,
                streamUrl = url,
                artworkUrl = null
            )
        }
    }

    private fun presetFromValue(value: String): Preset? {
        val key = value.removePrefix(PRESET_PREFIX)
        return if (value.startsWith(PRESET_PREFIX)) presets.firstOrNull { it.key == key } else null
    }
}
