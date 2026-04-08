package com.dmb.bestbefore.data.api.models

import com.google.gson.annotations.SerializedName

data class SoundCloudPlaylistResponse(
    @SerializedName("title") val title: String,
    @SerializedName("tracks") val tracks: List<SoundCloudTrack>
)

data class SoundCloudTrack(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("duration") val duration: Long,
    @SerializedName("streamUrl") val streamUrl: String,
    @SerializedName("artworkUrl") val artworkUrl: String?
)
