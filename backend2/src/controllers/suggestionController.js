import { getCollections } from '../config/db.js'
import { ObjectId } from 'mongodb'
import { updateUserPreferenceFromRoomInteraction, isPreferenceTrackingEnabled } from '../services/userPreferenceService.js'

const AI_SERVICE_URL = process.env.AI_SERVICE_URL || 'http://localhost:8000'
const SUGGESTION_CACHE_TTL_MS = 3 * 60 * 1000
const SUGGESTION_CACHE_MAX = 300
const AI_SUGGESTION_TIMEOUT_MS = 2200
const suggestionCache = new Map()
const SUGGESTION_RATE_WINDOW_MS = 10 * 1000
const SUGGESTION_RATE_MAX = 12
const suggestionRateMap = new Map()

function normalizeTags(tags = []) {
    return (tags || []).map(t => String(t).trim().toLowerCase()).filter(Boolean)
}

function haversineDistance(lat1, lon1, lat2, lon2) {
    const R = 6371
    const dLat = ((lat2 - lat1) * Math.PI) / 180
    const dLon = ((lon2 - lon1) * Math.PI) / 180
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos((lat1 * Math.PI) / 180) *
            Math.cos((lat2 * Math.PI) / 180) *
            Math.sin(dLon / 2) *
            Math.sin(dLon / 2)
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return R * c
}

function buildFallbackSuggestions(sourceRoom, candidateRooms = []) {
    const sourceTags = new Set(normalizeTags(sourceRoom.tags || []))
    const scored = candidateRooms
        .map(room => {
            const roomTags = normalizeTags(room.tags || [])
            const commonTagCount = roomTags.filter(tag => sourceTags.has(tag)).length
            const tagScore = Math.min(commonTagCount * 12, 60)
            const distanceScore =
                sourceRoom.lat !== undefined &&
                sourceRoom.lon !== undefined &&
                room.lat !== undefined &&
                room.lon !== undefined
                    ? Math.max(0, 30 - Math.min(30, haversineDistance(sourceRoom.lat, sourceRoom.lon, room.lat, room.lon)))
                    : 10
            const score = Math.round(Math.max(0, Math.min(100, tagScore + distanceScore)))
            if (score < 25) return null
            return {
                targetRoomId: room._id.toString(),
                targetRoomName: room.name,
                score,
                category: score >= 80 ? 'direct_match' : score >= 40 ? 'critical_zone' : 'reject',
                reasoning: 'Fast fallback suggestion (tag + distance).',
                similarity: score / 100,
            }
        })
        .filter(Boolean)
        .sort((a, b) => (b.score - a.score) || (b.similarity - a.similarity))
        .slice(0, 10)

    return {
        sourceRoomId: sourceRoom._id.toString(),
        suggestions: scored,
        count: scored.length,
        strategy: 'fallback',
    }
}

function isRateLimited(mapRef, key, windowMs, maxInWindow) {
    const now = Date.now()
    const entry = mapRef.get(key)
    if (!entry || now >= entry.resetAt) {
        mapRef.set(key, { count: 1, resetAt: now + windowMs })
        return false
    }
    if (entry.count >= maxInWindow) return true
    entry.count += 1
    return false
}

function cleanupExpiredEntries() {
    const now = Date.now()
    for (const [key, value] of suggestionCache.entries()) {
        if (!value || value.expiresAt <= now) suggestionCache.delete(key)
    }
    for (const [key, value] of suggestionRateMap.entries()) {
        if (!value || value.resetAt <= now) suggestionRateMap.delete(key)
    }
}

export const getRoomSuggestions = async (req, res) => {
    try {
        const { roomId } = req.params
        const { userId, email } = req.user
        const { rooms, users } = getCollections()
        cleanupExpiredEntries()

        if (isRateLimited(suggestionRateMap, userId, SUGGESTION_RATE_WINDOW_MS, SUGGESTION_RATE_MAX)) {
            return res.status(429).json({ error: 'Too many suggestion requests, please retry shortly.' })
        }

        if (!ObjectId.isValid(roomId)) {
            return res.status(400).json({ error: 'Invalid room ID' })
        }

        const sourceRoom = await rooms.findOne({ _id: new ObjectId(roomId) })
        if (!sourceRoom) {
            return res.status(404).json({ error: 'Room not found' })
        }

        const normalEmail = email.toLowerCase()
        const isOwner = sourceRoom.ownerEmail === normalEmail
        const isCollaborator = sourceRoom.collaborators?.some(c => (typeof c === 'string' ? c : c.email)?.toLowerCase() === normalEmail)

        if (sourceRoom.isPrivate && !isOwner && !isCollaborator) {
            return res.status(403).json({ error: 'Unauthorized to view suggestions for this room' })
        }

        const sourceTags = normalizeTags(sourceRoom.tags || [])
        const prioritizedCandidates = sourceTags.length > 0
            ? await rooms
                .find({
                    _id: { $ne: new ObjectId(roomId) },
                    isPrivate: false,
                    embedding: { $exists: true, $type: 'array', $ne: [] },
                    tags: { $in: sourceTags },
                })
                .limit(40)
                .toArray()
            : []
        const prioritizedIds = new Set(prioritizedCandidates.map(room => room._id.toString()))
        const additionalCandidates = await rooms
            .find({
                _id: { $ne: new ObjectId(roomId) },
                isPrivate: false,
                embedding: { $exists: true, $type: 'array', $ne: [] },
            })
            .limit(60)
            .toArray()
        const candidateRooms = [...prioritizedCandidates]
        for (const room of additionalCandidates) {
            if (candidateRooms.length >= 60) break
            const id = room._id.toString()
            if (!prioritizedIds.has(id)) {
                prioritizedIds.add(id)
                candidateRooms.push(room)
            }
        }

        if (candidateRooms.length === 0) {
            return res.json({ sourceRoomId: roomId, suggestions: [], count: 0 })
        }

        const user = await users.findOne({ _id: new ObjectId(userId) })

        const cacheKey = JSON.stringify({
            roomId,
            profileTags: (user?.preferredTags || sourceRoom.tags || []).slice(0, 10),
            profileUpdatedAt: user?.preferenceUpdatedAt || null,
        })
        const cached = suggestionCache.get(cacheKey)
        if (cached && cached.expiresAt > Date.now()) {
            return res.json(cached.payload)
        }

        const payload = {
            sourceRoomId: roomId,
            sourceRoom: {
                id: sourceRoom._id.toString(),
                name: sourceRoom.name,
                tags: sourceRoom.tags || [],
                isPrivate: sourceRoom.isPrivate,
                isTimeCapsule: sourceRoom.isTimeCapsule,
                lat: sourceRoom.lat,
                lon: sourceRoom.lon,
                description: sourceRoom.description,
                dwellTime: sourceRoom.dwellTime || 0,
                embedding: sourceRoom.embedding || [],
            },
            candidateRooms: candidateRooms.map(room => ({
                id: room._id.toString(),
                name: room.name,
                tags: room.tags || [],
                isPrivate: room.isPrivate,
                isTimeCapsule: room.isTimeCapsule,
                lat: room.lat,
                lon: room.lon,
                description: room.description,
                dwellTime: room.dwellTime || 0,
                embedding: room.embedding || [],
            })),
            userLat: sourceRoom.lat,
            userLon: sourceRoom.lon,
            userProfile: {
                preferredTags: user?.preferredTags || sourceRoom.tags || [],
                lastLat: user?.lastLat ?? sourceRoom.lat ?? null,
                lastLon: user?.lastLon ?? sourceRoom.lon ?? null,
                interactionRoomTypes: user?.preferenceRoomTypes || [],
                preferenceEmbedding: user?.preferenceEmbedding || [],
            },
        }

        let suggestions = null
        try {
            const controller = new AbortController()
            let timeout = null
            try {
                timeout = setTimeout(() => controller.abort(), AI_SUGGESTION_TIMEOUT_MS)
                const response = await fetch(`${AI_SERVICE_URL}/v1/suggestions`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload),
                    signal: controller.signal,
                })

                if (response.ok) {
                    suggestions = await response.json()
                    suggestions.strategy = suggestions.strategy || 'ai'
                }
            } finally {
                if (timeout) clearTimeout(timeout)
            }
        } catch (err) {
            suggestions = null
        }

        if (!suggestions) {
            suggestions = buildFallbackSuggestions(sourceRoom, candidateRooms)
        }

        if (suggestionCache.size >= SUGGESTION_CACHE_MAX) {
            const firstKey = suggestionCache.keys().next().value
            if (firstKey) suggestionCache.delete(firstKey)
        }
        suggestionCache.set(cacheKey, {
            payload: suggestions,
            expiresAt: Date.now() + SUGGESTION_CACHE_TTL_MS,
        })

        res.json(suggestions)
    } catch (error) {
        console.error('Error generating suggestions:', error)
        res.status(500).json({ error: 'Failed to generate suggestions' })
    }
}

export const acceptSuggestion = async (req, res) => {
    try {
        const { roomId, suggestionId } = req.params
        const { userId, email } = req.user

        if (!ObjectId.isValid(roomId) || !ObjectId.isValid(suggestionId)) {
            return res.status(400).json({ error: 'Invalid room IDs' })
        }

        const { rooms } = getCollections()

        const sourceRoom = await rooms.findOne({ _id: new ObjectId(roomId) })
        if (!sourceRoom) {
            return res.status(404).json({ error: 'Room not found' })
        }

        const targetRoom = await rooms.findOne({ _id: new ObjectId(suggestionId) })
        if (!targetRoom) {
            return res.status(404).json({ error: 'Suggested room not found' })
        }

        const normalEmail = email.toLowerCase()
        const isSourceOwner = sourceRoom.ownerEmail === normalEmail
        if (!isSourceOwner) {
            return res.status(403).json({ error: 'Only room owner can accept suggestions' })
        }

        if (!sourceRoom.connectedRooms) sourceRoom.connectedRooms = []
        if (!sourceRoom.connectedRooms.includes(targetRoom._id.toString())) {
            sourceRoom.connectedRooms.push(targetRoom._id.toString())
            await rooms.updateOne(
                { _id: new ObjectId(roomId) },
                { $set: { connectedRooms: sourceRoom.connectedRooms } }
            )
        }

        if (!targetRoom.connectedRooms) targetRoom.connectedRooms = []
        if (!targetRoom.connectedRooms.includes(sourceRoom._id.toString())) {
            targetRoom.connectedRooms.push(sourceRoom._id.toString())
            await rooms.updateOne(
                { _id: new ObjectId(suggestionId) },
                { $set: { connectedRooms: targetRoom.connectedRooms } }
            )
        }

        if (isPreferenceTrackingEnabled()) {
            updateUserPreferenceFromRoomInteraction({
                userId,
                room: targetRoom,
                interactionType: 'SUGGESTION_ACCEPT',
            }).catch(err => console.error('Failed to update SUGGESTION_ACCEPT preference signal:', err))
        }

        res.json({ ok: true, message: 'Suggestion accepted and connection created' })
    } catch (error) {
        console.error('Error accepting suggestion:', error)
        res.status(500).json({ error: 'Failed to accept suggestion' })
    }
}

export const rejectSuggestion = async (req, res) => {
    try {
        const { roomId, suggestionId } = req.params
        const { userId, email } = req.user

        if (!ObjectId.isValid(roomId) || !ObjectId.isValid(suggestionId)) {
            return res.status(400).json({ error: 'Invalid room IDs' })
        }

        const { rooms } = getCollections()

        const sourceRoom = await rooms.findOne({ _id: new ObjectId(roomId) })
        if (!sourceRoom) {
            return res.status(404).json({ error: 'Room not found' })
        }

        const normalEmail = email.toLowerCase()
        const isSourceOwner = sourceRoom.ownerEmail === normalEmail
        if (!isSourceOwner) {
            return res.status(403).json({ error: 'Only room owner can reject suggestions' })
        }

        if (!sourceRoom.rejectedSuggestions) sourceRoom.rejectedSuggestions = []
        if (!sourceRoom.rejectedSuggestions.includes(suggestionId)) {
            sourceRoom.rejectedSuggestions.push(suggestionId)
            await rooms.updateOne(
                { _id: new ObjectId(roomId) },
                { $set: { rejectedSuggestions: sourceRoom.rejectedSuggestions } }
            )
        }

        const targetRoom = await rooms.findOne({ _id: new ObjectId(suggestionId) })
        if (targetRoom && isPreferenceTrackingEnabled()) {
            updateUserPreferenceFromRoomInteraction({
                userId,
                room: targetRoom,
                interactionType: 'SUGGESTION_REJECT',
            }).catch(err => console.error('Failed to update SUGGESTION_REJECT preference signal:', err))
        }

        res.json({ ok: true, message: 'Suggestion rejected' })
    } catch (error) {
        console.error('Error rejecting suggestion:', error)
        res.status(500).json({ error: 'Failed to reject suggestion' })
    }
}
