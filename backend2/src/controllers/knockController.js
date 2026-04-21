import { ObjectId } from 'mongodb'
import { getCollections } from '../config/db.js'
import admin from '../config/firebase.js'

// POST /users/:email/knock
export const sendKnock = async (req, res) => {
    try {
        const { users } = getCollections()
        const { email: senderEmail, userId } = req.user
        const targetEmail = decodeURIComponent(req.params.email).toLowerCase()

        if (targetEmail === senderEmail?.toLowerCase()) {
            return res.status(400).json({ error: 'Cannot knock yourself' })
        }

        const target = await users.findOne({ email: targetEmail })
        if (!target) return res.status(404).json({ error: 'User not found' })

        if (!target.fcmToken) {
            return res.status(200).json({ ok: true, delivered: false, reason: 'User has no FCM token' })
        }

        const sender = await users.findOne({ _id: new ObjectId(userId) })
        const senderName = sender?.name || senderEmail?.split('@')[0] || 'Someone'

        await admin.messaging().send({
            notification: {
                title: '👋 Knock Knock',
                body: `${senderName} is knocking on your door`
            },
            data: {
                type: 'KNOCK',
                senderEmail: senderEmail || '',
                senderName
            },
            token: target.fcmToken
        })

        res.json({ ok: true, delivered: true })
    } catch (e) {
        console.error('[Knock] Failed to send knock:', e)
        res.status(500).json({ error: 'Failed to send knock' })
    }
}
