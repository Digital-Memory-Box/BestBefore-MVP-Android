package com.dmb.bestbefore.data.api.models

import com.google.gson.annotations.SerializedName

data class PublicProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("userType") val userType: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("profileImageUrl") val profileImageUrl: String?,
    @SerializedName("roomingCount") val roomingCount: Int,
    @SerializedName("roomersCount") val roomersCount: Int,
    @SerializedName("memoriesCount") val memoriesCount: Int,
    @SerializedName("publicRooms") val publicRooms: List<PublicRoomDto>
)

data class PublicRoomDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("theme") val theme: String?,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("description") val description: String? = null,
    @SerializedName("photos") val photos: List<String> = emptyList()
)
