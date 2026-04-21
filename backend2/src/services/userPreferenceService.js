import { ObjectId } from 'mongodb'
import { getCollections } from '../config/db.js'

const AI_SERVICE_URL = process.env.AI_SERVICE_URL || 'http://localhost:8000'
const ENABLE_PREFERENCE_TRACKING = process.env.ENABLE_PREFERENCE_TRACKING === 'true'

function toRoomType(room) {
    if (!room) return 'unknown'
    if (room.isTimeCapsule) return 'time_capsule'
    if (room.isPrivate) return 'private'
    return 'public'
}

async function askAiToUpdatePreference(profile, event) {
    const response = await fetch(`${AI_SERVICE_URL}/v1/user-preference/update`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            profile,
            event,
            topKTags: 30,
        }),
    })

    if (!response.ok) {
        throw new Error(`AI service returned ${response.status}`)
    }

    return response.json()
}

export async function updateUserPreferenceFromRoomInteraction({
    userId,
    room,
    interactionType,
    userLat,
    userLon,
}) {
    if (!ENABLE_PREFERENCE_TRACKING) return false
    if (!userId || !room || !interactionType) return false

    const { users } = getCollections()
    const userObjectId = new ObjectId(userId)
    const user = await users.findOne({ _id: userObjectId })
    if (!user) return false

    let updated = null
    try {
        updated = await askAiToUpdatePreference(
            {
                preferredTags: user.preferredTags || [],
                preferenceTagWeights: user.preferenceTagWeights || {},
                preferenceRoomTypes: user.preferenceRoomTypes || [],
                preferenceEmbedding: user.preferenceEmbedding || [],
                lastLat: user.lastLat ?? null,
                lastLon: user.lastLon ?? null,
            },
            {
                tags: room.tags || [],
                isPrivate: room.isPrivate === true,
                isTimeCapsule: room.isTimeCapsule === true,
                lat:
                    userLat !== undefined && userLat !== null
                        ? Number(userLat)
                        : room.lat !== undefined && room.lat !== null
                          ? Number(room.lat)
                          : null,
                lon:
                    userLon !== undefined && userLon !== null
                        ? Number(userLon)
                        : room.lon !== undefined && room.lon !== null
                          ? Number(room.lon)
                          : null,
                interactionType,
            }
        )
    } catch (error) {
        console.error('Failed to update preference via AI service:', error)
        return false
    }

    await users.updateOne(
        { _id: userObjectId },
        {
            $set: {
                preferenceTagWeights: updated.preferenceTagWeights || {},
                preferredTags: updated.preferredTags || [],
                preferenceRoomTypes: updated.preferenceRoomTypes || [toRoomType(room)],
                preferenceEmbedding: updated.preferenceEmbedding || [],
                preferenceUpdatedAt: new Date(),
                lastLat: updated.lastLat ?? user.lastLat ?? null,
                lastLon: updated.lastLon ?? user.lastLon ?? null,
            },
            $push: {
                preferenceInteractions: {
                    type: interactionType,
                    roomId: room._id ? room._id.toString() : room.id || null,
                    at: new Date(),
                },
            },
        }
    )

    return true
}

export function isPreferenceTrackingEnabled() {
    return ENABLE_PREFERENCE_TRACKING
}
