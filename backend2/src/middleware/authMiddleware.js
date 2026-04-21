import admin from '../config/firebase.js'
import { getCollections } from '../config/db.js'

export const authMiddleware = async (req, res, next) => {
    try {
        const authHeader = req.headers.authorization || ''
        if (!authHeader.startsWith('Bearer ')) {
            return res.status(401).json({ error: 'Missing Bearer token' })
        }
        const idToken = authHeader.split('Bearer ')[1]

        // 1. Verify Token with Firebase
        const decodedToken = await admin.auth().verifyIdToken(idToken)
        const { uid, email } = decodedToken

        const { users } = getCollections()

        // 2. Find or Create User in MongoDB
        let user = await users.findOne({ firebaseUid: uid })

        if (!user && email) {
            // Fallback: Check if user exists by email (OLD SYSTEM USER)
            user = await users.findOne({ email })
            if (user) {
                // Link existing user to this Firebase UID
                await users.updateOne({ _id: user._id }, { $set: { firebaseUid: uid } })
            }
        }

        if (!user) {
            // Create new user if not found at all
            console.log(`[AUTH] Creating new MongoDB user for ${email} (${uid})`)
            const normalizedEmail = email?.toLowerCase() || null
            const requestedUserType = req.body?.userType
            const userType = (requestedUserType === 'artist') ? 'artist' : 'normal'
            const newUser = {
                firebaseUid: uid,
                email: normalizedEmail,
                name: decodedToken.name || email?.split('@')[0] || 'User',
                userType,
                theme: 'Default',
                accentColor: '#007AFF',
                createdAt: new Date()
            }
            const result = await users.insertOne(newUser)
            user = { ...newUser, _id: result.insertedId }
        }

        // Attach to req
        req.user = {
            userId: user._id.toString(),
            firebaseUid: uid,
            email: user.email
        }

        next()
    } catch (error) {
        console.error('Auth Error:', error.message)
        return res.status(401).json({ error: 'Unauthorized: ' + error.message })
    }
}
