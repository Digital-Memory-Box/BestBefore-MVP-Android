import { ObjectId } from 'mongodb'
import { getCollections } from '../config/db.js'

export const syncAuth = async (req, res) => {
    try {
        const { users } = getCollections()
        const user = await users.findOne({ _id: new ObjectId(req.user.userId) })
        res.json({
            user: {
                id: user._id.toString(),
                name: user.name ?? null,
                email: user.email,
                userType: user.userType ?? 'normal',
                bio: user.bio ?? null,
                theme: user.theme ?? 'Default',
                accentColor: user.accentColor ?? '#007AFF',
                profileMusic: user.profileMusic ?? null
            }
        })
    } catch (e) {
        res.status(500).json({ error: 'Sync failed' })
    }
}

export const getMe = async (req, res) => {
    try {
        const { users } = getCollections()
        const { userId } = req.user
        const user = await users.findOne({ _id: new ObjectId(userId) })
        if (!user) return res.status(404).json({ error: 'not found' })
        res.json({
            user: {
                id: user._id.toString(),
                name: user.name ?? null,
                email: user.email,
                userType: user.userType ?? 'normal',
                bio: user.bio ?? null,
                theme: user.theme ?? 'Default',
                accentColor: user.accentColor ?? '#007AFF',
                profileMusic: user.profileMusic ?? null,
                createdAt: user.createdAt
            }
        })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to fetch user' })
    }
}

export const updateMe = async (req, res) => {
    console.log(`[DEBUG] PATCH /me hit for user: ${req.user.userId}`)
    try {
        const { users } = getCollections()
        const { userId } = req.user
        const { name, theme, accentColor, profileMusic, email, fcmToken, userType, bio } = req.body

        const update = {}
        if (name !== undefined) update.name = name
        if (theme !== undefined) update.theme = theme
        if (accentColor !== undefined) update.accentColor = accentColor
        if (profileMusic !== undefined) update.profileMusic = profileMusic
        if (fcmToken !== undefined) update.fcmToken = fcmToken
        if (userType !== undefined && ['normal', 'artist'].includes(userType)) update.userType = userType
        if (bio !== undefined) update.bio = bio

        if (email !== undefined) {
            const existing = await users.findOne({ email, _id: { $ne: new ObjectId(userId) } })
            if (existing) return res.status(409).json({ error: 'Email already in use' })
            update.email = email
        }

        if (Object.keys(update).length === 0) {
            return res.status(400).json({ error: 'Nothing to update' })
        }

        const result = await users.updateOne(
            { _id: new ObjectId(userId) },
            { $set: update }
        )

        if (result.matchedCount === 0) {
            console.warn(`[DEBUG] PATCH /me: User ${userId} NOT FOUND in DB`)
            return res.status(404).json({ error: 'User not found' })
        }

        const updatedUser = await users.findOne({ _id: new ObjectId(userId) }, { projection: { passwordHash: 0 } })
        res.json({
            user: {
                id: updatedUser._id.toString(),
                name: updatedUser.name ?? null,
                email: updatedUser.email,
                userType: updatedUser.userType ?? 'normal',
                bio: updatedUser.bio ?? null,
                theme: updatedUser.theme ?? 'Default',
                accentColor: updatedUser.accentColor ?? '#007AFF',
                profileMusic: updatedUser.profileMusic ?? null
            },
            // Removed token refresh logic here since signToken was undeclared in the original
        })
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to update user' })
    }
}

/**
 * GET /users/search?q=<name_or_email>
 * Returns up to 10 matching users (id, name, email) for invite search.
 * Auth required. The requesting user is excluded from results.
 */
export const searchUsers = async (req, res) => {
    try {
        const { users } = getCollections()
        const { userId } = req.user
        const q = String(req.query.q || '').trim()

        if (q.length < 2) {
            return res.status(400).json({ error: 'Query must be at least 2 characters' })
        }

        const regex = new RegExp(q, 'i')
        const results = await users
            .find(
                {
                    _id: { $ne: new ObjectId(userId) }, // exclude self
                    $or: [
                        { name: regex },
                        { email: regex }
                    ]
                },
                { projection: { _id: 1, name: 1, email: 1, profileImageUrl: 1 } }
            )
            .limit(10)
            .toArray()

        res.json(
            results.map(u => ({
                id: u._id.toString(),
                name: u.name ?? null,
                email: u.email,
                profileImageUrl: u.profileImageUrl ?? null
            }))
        )
    } catch (e) {
        console.error(e)
        res.status(500).json({ error: 'Failed to search users' })
    }
}
