package com.dmb.bestbefore.data.api.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

/**
 * Handles backend payload drift where Mongo ObjectIds may arrive as plain strings
 * or as nested JSON objects (for example { "$oid": "..." }).
 */
class RoomDtoJsonDeserializer : JsonDeserializer<RoomDto> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): RoomDto {
        val obj = json.asJsonObject
        val ownerObj = if (obj.has("owner") && obj.get("owner").isJsonObject) obj.getAsJsonObject("owner") else null

        val ownerId = readFlexibleString(obj, "ownerId", "owner_id") 
            ?: (if (ownerObj != null) readFlexibleString(ownerObj, "_id", "id") else null)
            ?: ""
        val ownerName = readFlexibleString(obj, "ownerName", "owner_name")
            ?: (if (ownerObj != null) readFlexibleString(ownerObj, "name", "userName", "username") else null)
        val ownerEmail = readFlexibleString(obj, "ownerEmail", "owner_email")
            ?: (if (ownerObj != null) readFlexibleString(ownerObj, "email") else null)
        val ownerUserType = readFlexibleString(obj, "ownerUserType", "owner_user_type")
            ?: (if (ownerObj != null) readFlexibleString(ownerObj, "userType", "user_type") else null)
        val ownerProfilePic = readFlexibleString(obj, "ownerProfilePic", "owner_profile_pic", "ownerProfileImageUrl", "owner_profile_image_url", "ownerAvatar", "owner_avatar")
            ?: (if (ownerObj != null) readFlexibleString(ownerObj, "profileImageUrl", "profile_image_url", "profilePic", "profile_pic", "profileImageData", "avatar", "image") else null)

        return RoomDto(
            id = readFlexibleString(obj, "_id", "id").orEmpty(),
            name = readFlexibleString(obj, "name").orEmpty(),
            ownerId = ownerId,
            ownerName = ownerName,
            ownerEmail = ownerEmail,
            ownerUserType = ownerUserType,
            ownerProfilePic = ownerProfilePic,
            createdAt = readFlexibleString(obj, "createdAt"),
            photos = readMemoryPreviewList(obj, "photos"),
            capsuleDurationDays = readInt(obj, "capsuleDurationDays"),
            capsuleDurationHours = readInt(obj, "capsuleDurationHours"),
            capsuleDurationMinutes = readInt(obj, "capsuleDurationMinutes"),
            isPrivate = readBoolean(obj, "isPrivate"),
            isPublic = readBooleanNullable(obj, "isPublic"),
            isTimeCapsule = readBoolean(obj, "isTimeCapsule"),
            theme = readFlexibleString(obj, "theme"),
            unlockDate = readFlexibleString(obj, "unlockDate"),
            expirationDate = readFlexibleString(obj, "expirationDate"),
            uploadStartDate = readFlexibleString(obj, "uploadStartDate"),
            collaborators = readJsonElementList(obj, "collaborators"),
            pendingCollaborators = readStringList(obj, "pendingCollaborators"),
            viewers = readJsonElementList(obj, "viewers"),
            pendingViewers = readStringList(obj, "pendingViewers"),
            rollingExpiryDays = readInt(obj, "rollingExpiryDays"),
            description = readFlexibleString(obj, "description"),
            generatedDescription = readFlexibleString(obj, "generatedDescription"),
            tags = readStringList(obj, "tags"),
            backgroundMusic = readFlexibleString(obj, "backgroundMusic"),
            connectedRooms = readStringList(obj, "connectedRooms")
        )
    }

    private fun readFlexibleString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            if (!obj.has(key) || obj.get(key).isJsonNull) continue
            return jsonElementToString(obj.get(key))
        }
        return null
    }

    private fun jsonElementToString(element: JsonElement): String? {
        return when {
            element.isJsonNull -> null
            element.isJsonPrimitive -> element.asString
            element.isJsonObject -> {
                val o = element.asJsonObject
                when {
                    o.has("\$oid") && !o.get("\$oid").isJsonNull -> o.get("\$oid").asString
                    o.has("oid") && !o.get("oid").isJsonNull -> o.get("oid").asString
                    o.has("_id") && !o.get("_id").isJsonNull -> jsonElementToString(o.get("_id"))
                    else -> o.toString()
                }
            }
            else -> element.toString()
        }
    }

    private fun readInt(obj: JsonObject, key: String): Int {
        if (!obj.has(key) || obj.get(key).isJsonNull) return 0
        return runCatching { obj.get(key).asInt }.getOrDefault(0)
    }

    private fun readBoolean(obj: JsonObject, key: String): Boolean {
        if (!obj.has(key) || obj.get(key).isJsonNull) return false
        return runCatching { obj.get(key).asBoolean }.getOrDefault(false)
    }

    private fun readBooleanNullable(obj: JsonObject, key: String): Boolean? {
        if (!obj.has(key) || obj.get(key).isJsonNull) return null
        return runCatching { obj.get(key).asBoolean }.getOrNull()
    }

    // Backend returns photos as [{ id: "...", url: "..." }] — extract id + url fields as MemoryPreview.
    private fun readPhotoUrlList(obj: JsonObject, key: String): List<MemoryPreview>? {
        if (!obj.has(key) || obj.get(key).isJsonNull || !obj.get(key).isJsonArray) return null
        return obj.getAsJsonArray(key).mapNotNull { element ->
            when {
                element.isJsonPrimitive -> MemoryPreview("", element.asString)
                element.isJsonObject -> {
                    val photoObj = element.asJsonObject
                    val id = jsonElementToString(photoObj.get("id") ?: photoObj.get("_id")).orEmpty()
                    val url = photoObj.get("url")
                    val urlStr = if (url != null && !url.isJsonNull) url.asString
                                 else jsonElementToString(element).orEmpty()
                    MemoryPreview(id, urlStr)
                }
                else -> null
            }
        }
    }

    private fun readStringList(obj: JsonObject, key: String): List<String>? {
        if (!obj.has(key) || obj.get(key).isJsonNull || !obj.get(key).isJsonArray) return null
        return obj.getAsJsonArray(key).mapNotNull { jsonElementToString(it) }
    }

    private fun readMemoryPreviewList(obj: JsonObject, key: String): List<MemoryPreview>? {
        if (!obj.has(key) || obj.get(key).isJsonNull || !obj.get(key).isJsonArray) return null
        return obj.getAsJsonArray(key).mapNotNull { element ->
            if (element.isJsonObject) {
                val o = element.asJsonObject
                val id = jsonElementToString(o.get("id") ?: o.get("_id")).orEmpty()
                val url = o.get("url")?.asString.orEmpty()
                MemoryPreview(id, url)
            } else if (element.isJsonPrimitive) {
                MemoryPreview("", element.asString)
            } else null
        }
    }

    private fun readJsonElementList(obj: JsonObject, key: String): List<JsonElement>? {
        if (!obj.has(key) || obj.get(key).isJsonNull || !obj.get(key).isJsonArray) return null
        return obj.getAsJsonArray(key).toList()
    }
}
