import { getCollections } from '../config/db.js'
import admin from '../config/firebase.js'

/**
 * Check for rooms whose unlockDate has just passed (within the last check window)
 * and send FCM notifications to the owner and collaborators.
 * Call this periodically via setInterval in server.js.
 */
export const checkExpiredRooms = async () => {
    try {
        const { rooms, users } = getCollections()
        const now = new Date()
        // Find rooms that unlocked in the last 5 minutes and haven't been notified yet
        const fiveMinAgo = new Date(now.getTime() - 5 * 60 * 1000)

        const expiredRooms = await rooms.find({
            unlockDate: { $lte: now, $gte: fiveMinAgo },
            unlockNotified: { $ne: true }
        }).toArray()

        if (expiredRooms.length === 0) return

        console.log(`[CRON] Found ${expiredRooms.length} newly unlocked rooms`)

        for (const room of expiredRooms) {
            try {
                // Collect all emails: owner + collaborators
                const emails = []
                if (room.ownerEmail) emails.push(room.ownerEmail)
                if (room.collaborators?.length) emails.push(...room.collaborators)

                // Dedupe
                const uniqueEmails = [...new Set(emails.map(e => e.toLowerCase()))]

                // Find users with FCM tokens
                const targetUsers = await users.find({
                    email: { $in: uniqueEmails },
                    fcmToken: { $exists: true, $ne: null }
                }).toArray()

                const tokens = targetUsers.map(u => u.fcmToken).filter(Boolean)

                if (tokens.length > 0 && admin) {
                    const message = {
                        notification: {
                            title: '🔓 Room Unlocked!',
                            body: `"${room.name}" is now unlocked. View your memories!`
                        },
                        data: {
                            type: 'ROOM_UNLOCKED',
                            roomId: room._id.toString(),
                            roomName: room.name
                        },
                        tokens: tokens
                    }
                    await admin.messaging().sendEachForMulticast(message)
                    console.log(`[CRON] Sent unlock notification for room "${room.name}" to ${tokens.length} users`)
                }

                // Mark as notified so we don't send again
                await rooms.updateOne(
                    { _id: room._id },
                    { $set: { unlockNotified: true } }
                )
            } catch (roomErr) {
                console.error(`[CRON] Error processing room ${room._id}:`, roomErr)
            }
        }
    } catch (e) {
        console.error('[CRON] Error checking expired rooms:', e)
    }
}
