import { ObjectId } from 'mongodb'
import { getCollections } from '../config/db.js'
import admin from '../config/firebase.js'
import { updateRoomEmbedding } from './semanticSearchController.js'
import { generateRoomDescription } from '../services/generativeService.js'
import { updateUserPreferenceFromRoomInteraction, isPreferenceTrackingEnabled } from '../services/userPreferenceService.js'

const AI_SERVICE_URL = process.env.AI_SERVICE_URL || 'http://localhost:8000'
const DISCOVERY_CACHE_TTL_MS = 5 * 60 * 1000
const DISCOVERY_CACHE_MAX = 300
const AI_DISCOVERY_TIMEOUT_MS = 2200
const DISCOVERY_RATE_WINDOW_MS = 10 * 1000
const DISCOVERY_RATE_MAX = 10
const discoveryCache = new Map()
const discoveryRateMap = new Map()

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

function cleanupDiscoveryState() {
    const now = Date.now()
    for (const [key, value] of discoveryCache.entries()) {
        if (!value || value.expiresAt <= now) discoveryCache.delete(key)
    }
    for (const [key, value] of discoveryRateMap.entries()) {
        if (!value || value.resetAt <= now) discoveryRateMap.delete(key)
    }
}

export const getDiscoverRooms = async (req, res) => {
    try {
        const { rooms } = getCollections()
        const docs = await rooms.aggregate([
            { $match: { isPrivate: false } },
            { $sort: { createdAt: -1 } },
            {
                $lookup: {
                    from: process.env.COLLECTION_USERS || 'Users',
                    localField: 'ownerId',
                    foreignField: '_id',
                    as: '_ownerDoc'
                }
            },
            {
                $addFields: {
                    ownerUserType: { $ifNull: [{ $arrayElemAt: ['$_ownerDoc.userType', 0] }, 'normal'] }
                }
            },
            { $project: { _ownerDoc: 0 } }
        ]).toArray()
        res.json(docs)
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to discover rooms' })
    }
}

export const getRooms = async (req, res) => {
    try {
        const { rooms } = getCollections()
        const { userId, email } = req.user

        const docs = await rooms.aggregate([
            {
                $match: {
                    $or: [
                        { ownerId: new ObjectId(userId) },
                        { "collaborators.email": email },
                        { "pendingCollaborators": email },
                        { "viewers": email },
                        { "pendingViewers": email }
                    ]
                }
            },
            { $sort: { createdAt: -1 } },
            {
                $lookup: {
                    from: process.env.COLLECTION_USERS || 'Users',
                    localField: 'ownerId',
                    foreignField: '_id',
                    as: '_ownerDoc'
                }
            },
            {
                $addFields: {
                    ownerUserType: { $ifNull: [{ $arrayElemAt: ['$_ownerDoc.userType', 0] }, 'normal'] }
                }
            },
            { $project: { _ownerDoc: 0 } }
        ]).toArray()

        res.json(docs)
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to fetch rooms' })
    }
}

export const getRoomById = async (req, res) => {
    try {
        const { rooms } = getCollections()
        const { id } = req.params
        const { email } = req.user

        if (!ObjectId.isValid(id)) {
            return res.status(400).json({ error: 'Invalid room ID' })
        }

        const room = await rooms.findOne({ _id: new ObjectId(id) })
        if (!room) {
            return res.status(404).json({ error: 'Room not found' })
        }

        const normalEmail = email.toLowerCase()
        const isOwner = room.ownerEmail === normalEmail
        const isCollaborator = room.collaborators?.some(c => (typeof c === 'string' ? c : c.email)?.toLowerCase() === normalEmail)
        const isViewer = room.viewers?.some(v => (typeof v === 'string' ? v : v.email)?.toLowerCase() === normalEmail)
        
        // If private, only allow owner, collaborator, or viewer
        if (room.isPrivate && !isOwner && !isCollaborator && !isViewer) {
            return res.status(403).json({ error: 'Unauthorized to view this room' })
        }

        if (isPreferenceTrackingEnabled()) {
            updateUserPreferenceFromRoomInteraction({
                userId: req.user.userId,
                room,
                interactionType: 'VIEW',
            }).catch(err => console.error('Failed to update VIEW preference signal:', err))
        }

        res.json(room)
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to fetch room' })
    }
}

export const getInitialDiscoveryRooms = async (req, res) => {
    try {
        const { rooms } = getCollections()
        cleanupDiscoveryState()
        if (isRateLimited(discoveryRateMap, req.user.userId, DISCOVERY_RATE_WINDOW_MS, DISCOVERY_RATE_MAX)) {
            return res.status(429).json({ error: 'Too many discovery requests, please retry shortly.' })
        }
        const { preferredTags = [], userLat, userLon, userName = '' } = req.body || {}
        const normalizedTags = Array.isArray(preferredTags)
            ? preferredTags.map(t => String(t).trim().toLowerCase()).filter(Boolean)
            : []
        const normalizedName = String(userName || '').trim().toLowerCase()
        const queryParts = [...normalizedTags]
        if (normalizedName) queryParts.push(normalizedName)
        const queryText = queryParts.join(' ').trim()

        const cacheKey = JSON.stringify({
            preferredTags: normalizedTags.slice(0, 10),
            userName: normalizedName,
            userLat: userLat ?? null,
            userLon: userLon ?? null,
        })
        const cached = discoveryCache.get(cacheKey)
        if (cached && cached.expiresAt > Date.now()) {
            return res.json(cached.payload)
        }

        const publicRooms = await rooms
            .find({
                isPrivate: false,
                embedding: { $exists: true, $type: 'array', $ne: [] },
            })
            .project({ name: 1, tags: 1, lat: 1, lon: 1, description: 1, embedding: 1, isTimeCapsule: 1 })
            .limit(120)
            .toArray()

        if (publicRooms.length === 0) {
            return res.json({ results: [], count: 0, strategy: 'empty' })
        }

        let resultPayload = null
        if (queryText.length > 0) {
            const controller = new AbortController()
            let timeout = null
            let response = null
            try {
                timeout = setTimeout(() => controller.abort(), AI_DISCOVERY_TIMEOUT_MS)
                response = await fetch(`${AI_SERVICE_URL}/v1/semantic-search`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        queryText,
                        candidateEmbeddings: publicRooms.map(room => ({
                            roomId: room._id.toString(),
                            id: room._id.toString(),
                            name: room.name,
                            embedding: room.embedding,
                        })),
                        topK: 10,
                    }),
                    signal: controller.signal,
                })
            } catch (err) {
                response = null
            } finally {
                if (timeout) clearTimeout(timeout)
            }

            if (response && response.ok) {
                const semantic = await response.json()
                const roomById = new Map(publicRooms.map(r => [r._id.toString(), r]))
                const enriched = (semantic.results || [])
                    .map(item => {
                        const room = roomById.get(item.roomId)
                        if (!room) return null
                        const distance =
                            userLat !== undefined && userLon !== undefined && room.lat !== undefined && room.lon !== undefined
                                ? haversineDistance(Number(userLat), Number(userLon), Number(room.lat), Number(room.lon))
                                : null
                        return {
                            roomId: room._id.toString(),
                            name: room.name,
                            tags: room.tags || [],
                            description: room.description || null,
                            lat: room.lat ?? null,
                            lon: room.lon ?? null,
                            isTimeCapsule: room.isTimeCapsule === true,
                            similarity: item.similarity,
                            distance,
                        }
                    })
                    .filter(Boolean)
                    .sort((a, b) => {
                        if (a.distance === null || b.distance === null) return b.similarity - a.similarity
                        return a.distance - b.distance
                    })

                resultPayload = {
                    results: enriched,
                    count: enriched.length,
                    strategy: 'semantic',
                }
            }
        }

        if (!resultPayload) {
            const sampled = publicRooms
                .slice(0, 30)
                .map(room => {
                    const distance =
                        userLat !== undefined && userLon !== undefined && room.lat !== undefined && room.lon !== undefined
                            ? haversineDistance(Number(userLat), Number(userLon), Number(room.lat), Number(room.lon))
                            : null
                    return {
                        roomId: room._id.toString(),
                        name: room.name,
                        tags: room.tags || [],
                        description: room.description || null,
                        lat: room.lat ?? null,
                        lon: room.lon ?? null,
                        isTimeCapsule: room.isTimeCapsule === true,
                        similarity: null,
                        distance,
                    }
                })
                .sort((a, b) => {
                    if (a.distance === null || b.distance === null) return 0
                    return a.distance - b.distance
                })
                .slice(0, 10)

            resultPayload = {
                results: sampled,
                count: sampled.length,
                strategy: 'fallback',
            }
        }

        if (discoveryCache.size >= DISCOVERY_CACHE_MAX) {
            const firstKey = discoveryCache.keys().next().value
            if (firstKey) discoveryCache.delete(firstKey)
        }
        discoveryCache.set(cacheKey, {
            payload: resultPayload,
            expiresAt: Date.now() + DISCOVERY_CACHE_TTL_MS,
        })

        return res.json(resultPayload)
    } catch (e) {
        console.error(e)
        return res.status(500).json({ error: 'Failed to get initial discovery rooms' })
    }
}

export const createRoom = async (req, res) => {
    console.log(`[DEBUG] POST /rooms hit - user: ${req.user.userId}`)
    try {
        const { rooms } = getCollections()
        const { userId, email } = req.user
        console.log(`[DEBUG] Room creation body:`, JSON.stringify(req.body, null, 2))
        const { name, isPrivate, isTimeCapsule, capsuleDurationDays } = req.body
        if (!name || typeof name !== 'string') {
            return res.status(400).json({ error: 'name is required' })
        }
        const doc = {
            name,
            ownerId: new ObjectId(userId),
            ownerEmail: email ?? null,
            isPrivate: isPrivate === true,
            isTimeCapsule: isTimeCapsule === true,
            capsuleDurationDays: (req.body.capsuleDurationDays !== undefined) ? Number(req.body.capsuleDurationDays) : 21,
            capsuleDurationHours: (req.body.capsuleDurationHours !== undefined) ? Number(req.body.capsuleDurationHours) : 0,
            capsuleDurationMinutes: (req.body.capsuleDurationMinutes !== undefined) ? Number(req.body.capsuleDurationMinutes) : 0,
            unlockDate: req.body.unlockDate ? new Date(req.body.unlockDate) : null,
            backgroundMusic: req.body.backgroundMusic ?? null,
            theme: req.body.theme ?? 'default',
            expirationDate: req.body.expirationDate ? new Date(req.body.expirationDate) : null,
            rollingExpiryDays: (req.body.rollingExpiryDays !== undefined) ? Number(req.body.rollingExpiryDays) : 0,
            tags: Array.isArray(req.body.tags) ? req.body.tags.map(t => t.replace(/^#+/, '').toLowerCase().trim()) : [],
            collaborators: [],
            pendingCollaborators: Array.isArray(req.body.collaborators) ? req.body.collaborators.map(e => e.toLowerCase().trim()) : [],
            viewers: [],
            pendingViewers: Array.isArray(req.body.viewers) ? req.body.viewers.map(e => e.toLowerCase().trim()) : [],
            createdAt: new Date()
        }
        const result = await rooms.insertOne(doc)

        // Generate embedding asynchronously for semantic search (FR-012)
        if (result.insertedId) {
            updateRoomEmbedding(
                result.insertedId.toString(),
                name,
                req.body.tags || [],
                req.body.generatedDescription || null
            ).catch(err => console.error('Failed to generate room embedding:', err))
        }

        // Generate nostalgic description asynchronously (Generative AI)
        if (result.insertedId) {
            const imageUrls = req.body.imageUrls || []
            const imageBase64 = req.body.imageBase64 || []
            generateRoomDescription(name, req.body.tags || [], imageUrls, imageBase64)
                .then((description) => {
                    if (description) {
                        rooms.updateOne(
                            { _id: result.insertedId },
                            { $set: { generatedDescription: description } }
                        )
                            .then(() => updateRoomEmbedding(
                                result.insertedId.toString(),
                                name,
                                req.body.tags || [],
                                description
                            ))
                            .catch(err => console.error('Failed to save generated description:', err))
                    }
                })
                .catch(err => console.error('Failed to generate room description:', err))
        }

        // Notify pending collaborators and viewers
        const allPending = [...doc.pendingCollaborators, ...doc.pendingViewers]
        if (allPending.length > 0 && admin) {
            try {
                const { users } = getCollections()
                // Exclude the creator's own email so they don't notify themselves
                const targetEmails = allPending.filter(e => e !== (email || '').toLowerCase())
                console.log(`[DEBUG] Creator email: ${email}, target emails (excluding self): ${targetEmails.join(', ')}`)
                const targetUsers = await users.find({ email: { $in: targetEmails }, fcmToken: { $exists: true, $ne: null } }).toArray()
                console.log(`[DEBUG] Found ${targetUsers.length} target users with FCM tokens`)
                const tokens = targetUsers.map(u => u.fcmToken).filter(Boolean)
                console.log(`[DEBUG] FCM tokens to send to: ${tokens.length}`)
                if (tokens.length > 0) {
                    const message = {
                        notification: {
                            title: 'Room Invitation',
                            body: `${req.user.name || email} invited you to join "${name}"`
                        },
                        data: {
                            type: 'INVITATION',
                            roomId: result.insertedId.toString(),
                            roomName: name
                        },
                        tokens: tokens
                    }
                    await admin.messaging().sendEachForMulticast(message)
                }
            } catch (err) {
                console.error('Failed to send FCM notifications:', err)
            }
        }

        res.json({ id: result.insertedId.toString() })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to create room' })
    }
}

export const updateRoom = async (req, res) => {
    try {
        const { rooms } = getCollections()
        const { id } = req.params
        const { userId } = req.user
        const { name, isPrivate, isTimeCapsule, capsuleDurationDays } = req.body

        const update = {}
        if (name !== undefined) update.name = name
        if (isPrivate !== undefined) update.isPrivate = isPrivate === true
        if (isTimeCapsule !== undefined) update.isTimeCapsule = isTimeCapsule === true
        if (capsuleDurationDays !== undefined) update.capsuleDurationDays = Number(capsuleDurationDays)
        if (req.body.capsuleDurationHours !== undefined) update.capsuleDurationHours = Number(req.body.capsuleDurationHours)
        if (req.body.capsuleDurationMinutes !== undefined) update.capsuleDurationMinutes = Number(req.body.capsuleDurationMinutes)
        if (req.body.unlockDate !== undefined) update.unlockDate = req.body.unlockDate ? new Date(req.body.unlockDate) : null
        if (req.body.backgroundMusic !== undefined) update.backgroundMusic = req.body.backgroundMusic
        if (req.body.theme !== undefined) update.theme = req.body.theme
        if (req.body.expirationDate !== undefined) update.expirationDate = req.body.expirationDate ? new Date(req.body.expirationDate) : null
        if (req.body.rollingExpiryDays !== undefined) update.rollingExpiryDays = Number(req.body.rollingExpiryDays)
        if (req.body.collaborators !== undefined) {
            update.collaborators = Array.isArray(req.body.collaborators) ? req.body.collaborators : []
        }
        if (req.body.tags !== undefined) {
            update.tags = Array.isArray(req.body.tags) ? req.body.tags.map(t => t.replace(/^#+/, '').toLowerCase().trim()) : []
        }

        console.log(`[DEBUG] Received PATCH /rooms/${id} from user ${userId}`)
        console.log(`[DEBUG] Update body:`, JSON.stringify(update, null, 2))

        const result = await rooms.updateOne(
            { _id: new ObjectId(id), ownerId: new ObjectId(userId) },
            { $set: update }
        )

        if (result.matchedCount === 0) {
            console.warn(`[DEBUG] Room ${id} NOT FOUND or NOT OWNED by ${userId}`)
            return res.status(404).json({ error: 'Room not found or unauthorized' })
        }

        // Re-generate embedding if name or tags changed (FR-012)
        if (update.name || update.tags) {
            const updatedRoom = await rooms.findOne({ _id: new ObjectId(id) })
            if (updatedRoom) {
                updateRoomEmbedding(
                    id,
                    updatedRoom.name,
                    updatedRoom.tags || [],
                    updatedRoom.generatedDescription || null
                ).catch(err => console.error('Failed to regenerate room embedding:', err))

                const imageUrls = req.body.imageUrls || []
                const imageBase64 = req.body.imageBase64 || []
                generateRoomDescription(updatedRoom.name, updatedRoom.tags || [], imageUrls, imageBase64)
                    .then((description) => {
                        if (description) {
                            rooms.updateOne(
                                { _id: new ObjectId(id) },
                                { $set: { generatedDescription: description } }
                            )
                                .then(() => updateRoomEmbedding(
                                    id,
                                    updatedRoom.name,
                                    updatedRoom.tags || [],
                                    description
                                ))
                                .catch(err => console.error('Failed to save generated description:', err))
                        }
                    })
                    .catch(err => console.error('Failed to regenerate description:', err))
            }
        }

        res.json({ ok: true })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to update room' })
    }
}

export const deleteRoom = async (req, res) => {
    try {
        const { rooms, memories } = getCollections()
        const { id } = req.params
        const { userId } = req.user

        const result = await rooms.deleteOne({ _id: new ObjectId(id), ownerId: new ObjectId(userId) })
        if (result.deletedCount === 0) return res.status(404).json({ error: 'Room not found or unauthorized' })

        // Also delete memories for this room
        await memories.deleteMany({ roomId: new ObjectId(id) })

        res.json({ ok: true })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to delete room' })
    }
}

export const acceptInvite = async (req, res) => {
    try {
        const { rooms, users } = getCollections()
        const { id } = req.params
        const normalEmail = req.user.email.toLowerCase()

        // Check if user is pending collaborator or viewer
        const room = await rooms.findOne({ _id: new ObjectId(id) })
        if (!room) return res.status(404).json({ error: 'Room not found' })

        let result
        if (room.pendingCollaborators && room.pendingCollaborators.includes(normalEmail)) {
            result = await rooms.updateOne(
                { _id: new ObjectId(id) },
                {
                    $pull: { pendingCollaborators: normalEmail },
                    $addToSet: { collaborators: normalEmail }
                }
            )
        } else if (room.pendingViewers && room.pendingViewers.includes(normalEmail)) {
            result = await rooms.updateOne(
                { _id: new ObjectId(id) },
                {
                    $pull: { pendingViewers: normalEmail },
                    $addToSet: { viewers: normalEmail }
                }
            )
        } else {
             return res.status(404).json({ error: 'Invite not found' })
        }

        // Notify the room owner that this user has joined
        if (admin) {
            try {
                const room = await rooms.findOne({ _id: new ObjectId(id) })
                if (room && room.ownerId) {
                    const owner = await users.findOne({ _id: room.ownerId, fcmToken: { $exists: true, $ne: null } })
                    if (owner && owner.fcmToken) {
                        const joinerName = normalEmail.split('@')[0]
                        const message = {
                            notification: {
                                title: 'Invite Accepted',
                                body: `${joinerName} has joined the room "${room.name}"`
                            },
                            data: {
                                type: 'INVITE_ACCEPTED',
                                roomId: id,
                                roomName: room.name
                            },
                            token: owner.fcmToken
                        }
                        await admin.messaging().send(message)
                        console.log(`[DEBUG] Sent 'invite accepted' notification to owner ${owner.email}`)
                    }
                }
            } catch (err) {
                console.error('Failed to send invite-accepted notification:', err)
            }
        }

        res.json({ ok: true })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to accept invite' })
    }
}

export const declineInvite = async (req, res) => {
    try {
        const { rooms } = getCollections()
        const { id } = req.params
        const normalEmail = req.user.email.toLowerCase()

        const result = await rooms.updateOne(
            { _id: new ObjectId(id) },
            { 
                $pull: { 
                    pendingCollaborators: normalEmail,
                    pendingViewers: normalEmail 
                } 
            }
        )
        if (result.matchedCount === 0) return res.status(404).json({ error: 'Invite not found' })
        res.json({ ok: true })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to decline invite' })
    }
}

// POST /rooms/:id/approve-join — Owner approves a pending join request
export const approveJoinRequest = async (req, res) => {
    try {
        const { rooms, users } = getCollections()
        const { id } = req.params
        const { userId, email } = req.user
        const { requesterEmail } = req.body

        if (!requesterEmail) {
            return res.status(400).json({ error: 'requesterEmail is required' })
        }

        const room = await rooms.findOne({ _id: new ObjectId(id) })
        if (!room) return res.status(404).json({ error: 'Room not found' })

        // Only the owner can approve
        if (room.ownerId.toString() !== userId.toString()) {
            return res.status(403).json({ error: 'Only the room owner can approve join requests' })
        }

        const normalRequesterEmail = requesterEmail.toLowerCase()

        const result = await rooms.updateOne(
            { _id: new ObjectId(id), pendingCollaborators: normalRequesterEmail },
            {
                $pull: { pendingCollaborators: normalRequesterEmail },
                $addToSet: { collaborators: normalRequesterEmail }
            }
        )

        if (result.matchedCount === 0) {
            return res.status(404).json({ error: 'No pending join request found for this user' })
        }

        // Notify the requester that they were approved
        if (admin) {
            try {
                const requester = await users.findOne({ email: normalRequesterEmail, fcmToken: { $exists: true, $ne: null } })
                if (requester?.fcmToken) {
                    await admin.messaging().send({
                        notification: {
                            title: 'Join Request Approved! 🎉',
                            body: `You can now collaborate on "${room.name}"`
                        },
                        data: {
                            type: 'JOIN_APPROVED',
                            roomId: id,
                            roomName: room.name
                        },
                        token: requester.fcmToken
                    })
                }
            } catch (err) {
                console.error('Failed to notify requester of approval:', err)
            }
        }

        res.json({ ok: true })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to approve join request' })
    }
}

// POST /rooms/:id/deny-join — Owner denies a pending join request
export const denyJoinRequest = async (req, res) => {
    try {
        const { rooms, users } = getCollections()
        const { id } = req.params
        const { userId } = req.user
        const { requesterEmail } = req.body

        if (!requesterEmail) {
            return res.status(400).json({ error: 'requesterEmail is required' })
        }

        const room = await rooms.findOne({ _id: new ObjectId(id) })
        if (!room) return res.status(404).json({ error: 'Room not found' })

        // Only the owner can deny
        if (room.ownerId.toString() !== userId.toString()) {
            return res.status(403).json({ error: 'Only the room owner can deny join requests' })
        }

        const normalRequesterEmail = requesterEmail.toLowerCase()

        const result = await rooms.updateOne(
            { _id: new ObjectId(id), pendingCollaborators: normalRequesterEmail },
            { $pull: { pendingCollaborators: normalRequesterEmail } }
        )

        if (result.matchedCount === 0) {
            return res.status(404).json({ error: 'No pending join request found for this user' })
        }

        // Notify the requester that they were denied
        if (admin) {
            try {
                const requester = await users.findOne({ email: normalRequesterEmail, fcmToken: { $exists: true, $ne: null } })
                if (requester?.fcmToken) {
                    await admin.messaging().send({
                        notification: {
                            title: 'Join Request Declined',
                            body: `Your request to join "${room.name}" was not approved`
                        },
                        data: {
                            type: 'JOIN_DENIED',
                            roomId: id,
                            roomName: room.name
                        },
                        token: requester.fcmToken
                    })
                }
            } catch (err) {
                console.error('Failed to notify requester of denial:', err)
            }
        }

        res.json({ ok: true })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to deny join request' })
    }
}
