import { ObjectId } from 'mongodb'
import { getCollections } from '../config/db.js'
import admin from '../config/firebase.js'

export const getTrendingMemories = async (req, res) => {
    try {
        const { memories } = getCollections()
        const roomsCollectionName = process.env.COLLECTION_ROOMS || 'Rooms'

        // Aggregation to find memories in PUBLIC rooms
        const docs = await memories.aggregate([
            {
                $lookup: {
                    from: roomsCollectionName,
                    localField: 'roomId',
                    foreignField: '_id',
                    as: 'room'
                }
            },
            { $unwind: '$room' },
            { $match: { 'room.isPrivate': false } },
            { $sort: { createdAt: -1 } },
            { $limit: 30 }
        ]).toArray()

        res.json(docs.map(d => ({
            ...d,
            roomName: d.room.name
        })))
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to fetch trending memories' })
    }
}

export const getMemoryCount = async (req, res) => {
    try {
        const { rooms, memories } = getCollections()
        const { userId } = req.user
        // Find all rooms owned by this user
        const userRooms = await rooms.find({ ownerId: new ObjectId(userId) }).toArray()
        const roomIds = userRooms.map(r => r._id)

        // Count memories in those rooms
        const count = await memories.countDocuments({ roomId: { $in: roomIds } })
        res.json({ count })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to fetch memory count' })
    }
}

export const getMemoriesByRoom = async (req, res) => {
    try {
        const { memories, rooms } = getCollections()
        const { roomId } = req.params

        // 1. Fetch Room to check rolling expiry and permissions
        const room = await rooms.findOne({ _id: new ObjectId(roomId) })
        if (!room) return res.status(404).json({ error: 'Room not found' })

        // 1.5 Security: Check if user has access
        const { userId, email } = req.user
        const isOwner = room.ownerId.toString() === userId.toString()
        const isCollaborator = room.collaborators?.some(c => (typeof c === 'string' ? c : c.email)?.toLowerCase() === email.toLowerCase())

        if (!isOwner && !isCollaborator && room.isPrivate) {
            return res.status(403).json({ error: 'Unauthorized access to private room' })
        }

        let query = {
            roomId: new ObjectId(roomId),
            isArchived: { $ne: true } // Exclude manually dumped
        }

        // 2. If rolling expiry is enabled, exclude memories older than X days
        if (room.rollingExpiryDays && room.rollingExpiryDays > 0) {
            const cutoffDate = new Date()
            cutoffDate.setDate(cutoffDate.getDate() - room.rollingExpiryDays)
            query.createdAt = { $gt: cutoffDate }
        }

        const docs = await memories.find(query).sort({ createdAt: -1 }).toArray()
        res.json(docs)
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to fetch memories' })
    }
}

export const addMemoryToRoom = async (req, res) => {
    try {
        const { memories, rooms } = getCollections()
        const { roomId } = req.params
        const { userId, email } = req.user
        const { type, title, content, metadata } = req.body

        // Security: Check if user has permission to add memory
        const room = await rooms.findOne({ _id: new ObjectId(roomId) })
        if (!room) return res.status(404).json({ error: 'Room not found' })

        const isOwner = room.ownerId.toString() === userId.toString()
        const isCollaborator = room.collaborators?.some(c => (typeof c === 'string' ? c : c.email)?.toLowerCase() === email.toLowerCase())
        const canPost = isOwner || isCollaborator

        if (!canPost) {
            return res.status(403).json({ error: 'Unauthorized: Only owners or collaborators can add memories' })
        }

        console.log(`[DEBUG] Adding memory: type=${type}, title=${title}, contentSize=${content?.length ?? 0}`)

        if (!type || !title) {
            return res.status(400).json({ error: 'type and title are required' })
        }

        if (!ObjectId.isValid(roomId)) {
            console.error('Invalid Room ID:', roomId)
            return res.status(400).json({ error: 'invalid room id format' })
        }

        const doc = {
            roomId: new ObjectId(roomId),
            authorId: new ObjectId(req.user.userId),
            type,
            title,
            content: content ?? null,
            metadata: metadata ?? {},
            isArchived: false, // Default false
            createdAt: new Date()
        }

        const result = await memories.insertOne(doc)

        // Notify other collaborators + owner about the new memory
        if (admin) {
            try {
                const { users } = getCollections()
                // Collect all emails that should be notified (owner + collaborators, excluding uploader)
                const allEmails = []
                if (room.ownerEmail) allEmails.push(room.ownerEmail)
                if (room.collaborators) allEmails.push(...room.collaborators)
                const targetEmails = [...new Set(allEmails)].filter(e => e !== email)

                if (targetEmails.length > 0) {
                    const targetUsers = await users.find({ email: { $in: targetEmails }, fcmToken: { $exists: true, $ne: null } }).toArray()
                    const tokens = targetUsers.map(u => u.fcmToken).filter(Boolean)
                    if (tokens.length > 0) {
                        const uploaderName = email.split('@')[0]
                        const memoryType = type === 'audio' ? 'an audio' : type === 'note' ? 'a note' : 'a photo'
                        const message = {
                            notification: {
                                title: 'New Memory Added',
                                body: `${uploaderName} added ${memoryType} to "${room.name}"`
                            },
                            data: {
                                type: 'MEMORY_ADDED',
                                roomId: roomId,
                                roomName: room.name
                            },
                            tokens: tokens
                        }
                        await admin.messaging().sendEachForMulticast(message)
                        console.log(`[DEBUG] Sent memory notification to ${tokens.length} users for room ${room.name}`)
                    }
                }
            } catch (err) {
                console.error('Failed to send memory notification:', err)
            }
        }

        res.json({ id: result.insertedId.toString(), ...doc })
    } catch (e) {
        console.error('Error adding memory:', e)
        res.status(500).json({ error: 'Failed to add memory' })
    }
}

// NEW: Fetch ONLY archived memories for a room
export const getArchivedMemoriesByRoom = async (req, res) => {
    try {
        const { memories, rooms } = getCollections()
        const { roomId } = req.params

        // 1. Fetch Room to find rolled-over memories too
        const room = await rooms.findOne({ _id: new ObjectId(roomId) })
        if (!room) return res.status(404).json({ error: 'Room not found' })

        // Archival conditions: Either explicitly dumped OR past rolling expiry
        let orConditions = [{ isArchived: true }]

        if (room.rollingExpiryDays && room.rollingExpiryDays > 0) {
            const cutoffDate = new Date()
            cutoffDate.setDate(cutoffDate.getDate() - room.rollingExpiryDays)
            orConditions.push({ createdAt: { $lte: cutoffDate } })
        }

        const query = {
            roomId: new ObjectId(roomId),
            $or: orConditions
        }

        const docs = await memories.find(query).sort({ createdAt: -1 }).toArray()
        res.json(docs)
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to fetch archived memories' })
    }
}

// NEW: Manual Bulk Dump (Archive all non-archived memories older than now)
export const dumpMemories = async (req, res) => {
    try {
        const { memories, rooms } = getCollections()
        const { roomId } = req.params
        const { userId } = req.user

        // Security: Ensure user owns the room
        const room = await rooms.findOne({ _id: new ObjectId(roomId), ownerId: new ObjectId(userId) })
        if (!room) return res.status(403).json({ error: 'Unauthorized or room not found' })

        // Update all unarchived memories to true
        const result = await memories.updateMany(
            { roomId: new ObjectId(roomId), isArchived: { $ne: true } },
            { $set: { isArchived: true } }
        )

        res.json({ ok: true, modifiedCount: result.modifiedCount })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to dump memories' })
    }
}
