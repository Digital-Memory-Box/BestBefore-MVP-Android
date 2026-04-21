const AI_SERVICE_URL = process.env.AI_SERVICE_URL || 'http://localhost:8000'

export const generateRoomDescription = async (roomName, tags = [], imageUrls = [], imageBase64List = []) => {
    try {
        const payload = {
            roomName,
            tags: tags.filter(t => t && typeof t === 'string'),
            imageUrls: imageUrls.filter(url => url && typeof url === 'string').slice(0, 4),
            imageBase64: imageBase64List.filter(b64 => b64 && typeof b64 === 'string').slice(0, 4),
        }

        const response = await fetch(`${AI_SERVICE_URL}/v1/generate-description`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        })

        if (!response.ok) {
            console.error(`AI service returned ${response.status}`)
            return null
        }

        const data = await response.json()
        return data.description || null
    } catch (error) {
        console.error('Error generating room description:', error)
        return null
    }
}
