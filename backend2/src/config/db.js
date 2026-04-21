import { MongoClient } from 'mongodb'
import dotenv from 'dotenv'

dotenv.config()

const mongoUri = process.env.MONGODB_URI
const dbName = process.env.DB_NAME || 'bestBefore_TestDB'
const roomsCollectionName = process.env.COLLECTION_ROOMS || 'Rooms'
const usersCollectionName = process.env.COLLECTION_USERS || 'Users'
const memoriesCollectionName = process.env.COLLECTION_MEMORIES || 'Memories'
const inviteTokensCollectionName = process.env.COLLECTION_INVITE_TOKENS || 'InviteTokens'

let client
let db
let collections = {
    rooms: null,
    users: null,
    memories: null,
    inviteTokens: null,
    handshakeInvites: null
}

export const connectDB = async () => {
    if (client) return collections
    try {
        client = new MongoClient(mongoUri)
        await client.connect()
        db = client.db(dbName)

        collections.rooms = db.collection(roomsCollectionName)
        collections.users = db.collection(usersCollectionName)
        collections.memories = db.collection(memoriesCollectionName)
        collections.inviteTokens = db.collection(inviteTokensCollectionName)
        collections.handshakeInvites = db.collection(process.env.COLLECTION_HANDSHAKE_INVITES || 'HandshakeInvites')

        await collections.users.createIndex({ email: 1 }, { unique: true })
        await collections.inviteTokens.createIndex({ token: 1 }, { unique: true })
        try {
            await collections.users.createIndex({ firebaseUid: 1 }, { unique: true, sparse: true })
        } catch (e) {
            console.warn("Notice: Could not create unique firebaseUid index (might already exist or conflict):", e.message)
        }

        console.log('Connected to MongoDB, DB:', dbName)
        return collections
    } catch (error) {
        console.error('MongoDB connection error:', error)
        process.exit(1)
    }
}

export const getCollections = () => {
    if (!client) throw new Error('Database not initialized. Call connectDB first.')
    return collections
}
