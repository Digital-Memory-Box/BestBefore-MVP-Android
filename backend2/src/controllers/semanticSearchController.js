import { getCollections } from '../config/db.js'
import { ObjectId } from 'mongodb'

const AI_SERVICE_URL = process.env.AI_SERVICE_URL || 'http://localhost:8000'

function buildEmbeddingText(roomName, tags = [], generatedDescription = null) {
    const namePart = String(roomName || '').trim()
    const tagsPart = Array.isArray(tags) ? tags.filter(Boolean).join(' ') : ''
    const descriptionPart = generatedDescription ? String(generatedDescription).trim() : ''
    return [namePart, tagsPart, descriptionPart].filter(Boolean).join(' ').trim()
}

export const embedRoomContent = async (roomName, tags = [], generatedDescription = null) => {
    try {
        const text = buildEmbeddingText(roomName, tags, generatedDescription)
        const response = await fetch(`${AI_SERVICE_URL}/v1/embed`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text,
            }),
        })
        if (!response.ok) {
            throw new Error(`AI service returned ${response.status}`)
        }
        const data = await response.json()
        return data.embedding
    } catch (error) {
        console.error('Error generating embedding:', error)
        return null
    }
}

export const semanticSearchRooms = async (req, res) => {
    try {
        const { query, userLat, userLon, maxDistance = 50 } = req.body
        if (!query || query.trim() === '') {
            return res.status(400).json({ error: 'Query is required' })
        }

        const { rooms } = getCollections()

        const allRooms = await rooms
            .find({
                isPrivate: false,
                embedding: { $exists: true, $type: 'array', $ne: [] },
            })
            .toArray()

        if (allRooms.length === 0) {
            return res.json({ query, results: [], count: 0 })
        }

        const candidateRooms = allRooms.map((room) => ({
            roomId: room._id.toString(),
            id: room._id.toString(),
            name: room.name,
            embedding: room.embedding,
        }))

        const response = await fetch(`${AI_SERVICE_URL}/v1/semantic-search`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                queryText: query,
                candidateEmbeddings: candidateRooms,
                topK: 10,
            }),
        })

        if (!response.ok) {
            throw new Error(`AI service returned ${response.status}`)
        }

        const searchResults = await response.json()

        let filteredResults = searchResults.results || []

        if (userLat !== undefined && userLon !== undefined && maxDistance) {
            filteredResults = filteredResults
                .map((result) => {
                    const room = allRooms.find((r) => r._id.toString() === result.roomId)
                    if (!room) return null
                    const distance = haversineDistance(userLat, userLon, room.lat, room.lon)
                    return { ...result, distance }
                })
                .filter((r) => r && r.distance <= maxDistance)
                .sort((a, b) => a.distance - b.distance)
        }

        const enrichedResults = filteredResults.map((result) => {
            const room = allRooms.find((r) => r._id.toString() === result.roomId)
            return {
                ...result,
                description: room?.description,
                lat: room?.lat,
                lon: room?.lon,
                tags: room?.tags,
                distance: result.distance || null,
            }
        })

        res.json({
            query,
            results: enrichedResults,
            count: enrichedResults.length,
        })
    } catch (error) {
        console.error('Semantic search error:', error)
        res.status(500).json({ error: 'Semantic search failed' })
    }
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

export const updateRoomEmbedding = async (roomId, roomName, tags = [], generatedDescription = null) => {
    try {
        const embedding = await embedRoomContent(roomName, tags, generatedDescription)
        if (!embedding) {
            console.warn(`Failed to generate embedding for room ${roomId}`)
            return false
        }

        const { rooms } = getCollections()
        const result = await rooms.updateOne(
            { _id: new ObjectId(roomId) },
            { $set: { embedding, embeddingUpdatedAt: new Date() } }
        )

        return result.modifiedCount > 0
    } catch (error) {
        console.error(`Error updating embedding for room ${roomId}:`, error)
        return false
    }
}
