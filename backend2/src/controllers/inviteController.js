import crypto from 'crypto'
import { ObjectId } from 'mongodb'
import { getCollections } from '../config/db.js'
import admin from '../config/firebase.js'
import QRCode from 'qrcode'

// POST /rooms/:id/invite-token — Generate invite token for a room
export const generateInviteToken = async (req, res) => {
    try {
        const { rooms, inviteTokens } = getCollections()
        const { id } = req.params
        const { userId, email } = req.user
        const { expiresInHours, maxUses } = req.body || {}

        console.log(`[DEBUG] generateInviteToken called. RoomId param: '${id}', User: ${userId}`)

        if (!ObjectId.isValid(id)) {
            console.error(`[DEBUG] generateInviteToken: Invalid room ID format: ${id}`)
            return res.status(400).json({ error: 'Invalid room ID' })
        }

        const room = await rooms.findOne({ _id: new ObjectId(id) })
        if (!room) {
            console.error(`[DEBUG] generateInviteToken: Room not found in DB! ID: ${id}`)
            return res.status(404).json({ error: 'Room not found' })
        }

        console.log(`[DEBUG] generateInviteToken: Room found! Name: ${room.name}, Owner: ${room.ownerId}`)

        // Only owner or collaborator can generate tokens
        const isOwner = room.ownerId?.toString() === userId?.toString()
        const isCollaborator = room.collaborators?.some(c => (typeof c === 'string' ? c : c.email)?.toLowerCase() === email?.toLowerCase())
        if (!isOwner && !isCollaborator) {
            return res.status(403).json({ error: 'Only room members can generate invite tokens' })
        }

        // Generate cryptographically secure token
        const token = crypto.randomBytes(32).toString('hex')

        const doc = {
            token,
            roomId: new ObjectId(id),
            createdBy: new ObjectId(userId),
            createdAt: new Date(),
            expiresAt: expiresInHours ? new Date(Date.now() + expiresInHours * 3600000) : null,
            maxUses: maxUses || null,
            usedCount: 0,
            usedBy: []
        }

        await inviteTokens.insertOne(doc)

        // Use HTTPS link so it can be shared across iOS and Android via any browser/messenger
        const inviteLink = `https://bestbefore.up.railway.app/invite-join/${token}`

        res.json({
            token,
            inviteLink,
            expiresAt: doc.expiresAt,
            maxUses: doc.maxUses
        })
    } catch (e) {
        console.error('Error generating invite token:', e)
        res.status(500).json({ error: 'Failed to generate invite token' })
    }
}

// GET /invite/:token — Public endpoint to validate token
export const validateInviteToken = async (req, res) => {
    try {
        const { inviteTokens, rooms } = getCollections()
        const { token } = req.params

        const invite = await inviteTokens.findOne({ token })
        if (!invite) {
            return res.status(404).json({ error: 'Invalid invite token', code: 'TOKEN_INVALID' })
        }

        // Check expiration
        if (invite.expiresAt && new Date() > invite.expiresAt) {
            return res.status(410).json({ error: 'Invite token has expired', code: 'TOKEN_EXPIRED' })
        }

        // Check usage limit
        if (invite.maxUses && invite.usedCount >= invite.maxUses) {
            return res.status(410).json({ error: 'Invite token has reached its usage limit', code: 'TOKEN_EXHAUSTED' })
        }

        const room = await rooms.findOne({ _id: invite.roomId })
        if (!room) {
            return res.status(404).json({ error: 'Room no longer exists', code: 'ROOM_DELETED' })
        }

        res.json({
            valid: true,
            roomId: room._id.toString(),
            roomName: room.name,
            token,
            deepLink: `bestbefore://invite/${token}`
        })
    } catch (e) {
        console.error('Error validating invite token:', e)
        res.status(500).json({ error: 'Failed to validate invite token' })
    }
}

// POST /invite/:token/join — Authenticated: join room via token
export const joinViaToken = async (req, res) => {
    try {
        const { inviteTokens, rooms, users } = getCollections()
        const { token } = req.params
        const { userId, email } = req.user

        const invite = await inviteTokens.findOne({ token })
        if (!invite) {
            return res.status(404).json({ error: 'Invalid invite token', code: 'TOKEN_INVALID' })
        }

        // Check expiration
        if (invite.expiresAt && new Date() > invite.expiresAt) {
            return res.status(410).json({ error: 'Invite token has expired', code: 'TOKEN_EXPIRED' })
        }

        // Check usage limit
        if (invite.maxUses && invite.usedCount >= invite.maxUses) {
            return res.status(410).json({ error: 'Invite token usage limit reached', code: 'TOKEN_EXHAUSTED' })
        }

        const room = await rooms.findOne({ _id: invite.roomId })
        if (!room) {
            return res.status(404).json({ error: 'Room no longer exists', code: 'ROOM_DELETED' })
        }

        // Check duplicate join
        const normalEmail = email.toLowerCase()
        const isOwner = room.ownerEmail === normalEmail
        const isAlreadyCollaborator = room.collaborators?.includes(normalEmail)

        if (isOwner || isAlreadyCollaborator) {
            return res.json({
                success: true,
                message: 'You are already a member of this room',
                roomId: room._id.toString(),
                roomName: room.name,
                alreadyMember: true
            })
        }

        // Add user as pending collaborator
        await rooms.updateOne(
            { _id: invite.roomId },
            {
                $addToSet: { pendingCollaborators: normalEmail }
            }
        )

        // Update token usage
        await inviteTokens.updateOne(
            { _id: invite._id },
            {
                $inc: { usedCount: 1 },
                $addToSet: { usedBy: normalEmail }
            }
        )

        // Notify room owner
        if (admin && room.ownerEmail && room.ownerEmail !== normalEmail) {
            try {
                const owner = await users.findOne({ email: room.ownerEmail, fcmToken: { $exists: true, $ne: null } })
                if (owner?.fcmToken) {
                    const userName = normalEmail.split('@')[0]
                    await admin.messaging().send({
                        notification: {
                            title: 'New Join Request!',
                            body: `${userName} wants to join "${room.name}" via invite link`
                        },
                        data: {
                            type: 'INVITE_REQUESTED',
                            roomId: room._id.toString(),
                            roomName: room.name,
                            requesterEmail: normalEmail
                        },
                        token: owner.fcmToken
                    })
                }
            } catch (err) {
                console.error('Failed to notify room owner:', err)
            }
        }

        res.json({
            success: true,
            roomId: room._id.toString(),
            roomName: room.name,
            alreadyMember: false
        })
    } catch (e) {
        console.error('Error joining via token:', e)
        res.status(500).json({ error: 'Failed to join room' })
    }
}

// POST /rooms/:id/handshake-invites
export const createHandshakeInvite = async (req, res) => {
    try {
        const { rooms, users, handshakeInvites } = getCollections()
        const { id } = req.params
        const { userId, email } = req.user
        const { inviteeEmail } = req.body

        if (!ObjectId.isValid(id) || !inviteeEmail) {
            return res.status(400).json({ error: 'Invalid input' })
        }

        const room = await rooms.findOne({ _id: new ObjectId(id) })
        if (!room) return res.status(404).json({ error: 'Room not found' })

        // Check if owner or collaborator
        const normalEmail = email.toLowerCase()
        const isOwner = room.ownerEmail === normalEmail
        const isCollaborator = room.collaborators?.some(c => (typeof c === 'string' ? c : c.email)?.toLowerCase() === normalEmail)
        if (!isOwner && !isCollaborator) {
            return res.status(403).json({ error: 'Only room members can send invites' })
        }

        // Generate 2-digit code
        const passcode = Math.floor(10 + Math.random() * 90).toString() // -> "10" to "99"

        const inviteDoc = {
            roomId: new ObjectId(id),
            roomName: room.name,
            inviterEmail: normalEmail,
            inviteeEmail: inviteeEmail.toLowerCase(),
            passcode: passcode,
            status: 'pending', // 'pending' | 'accepted' | 'expired'
            createdAt: new Date(),
            expiresAt: new Date(Date.now() + 10 * 60000) // expires in 10 minutes
        }

        const result = await handshakeInvites.insertOne(inviteDoc)

        return res.json({
            success: true,
            inviteId: result.insertedId.toString(),
            passcode: passcode,
            expiresAt: inviteDoc.expiresAt
        })
    } catch (e) {
        console.error('Error creating handshake invite:', e)
        res.status(500).json({ error: 'Internal server error' })
    }
}

// GET /me/handshake-invites
export const getMyHandshakeInvites = async (req, res) => {
    try {
        const { handshakeInvites } = getCollections()
        const { email } = req.user

        const normalEmail = email.toLowerCase()

        const pendingInvites = await handshakeInvites.find({
            inviteeEmail: normalEmail,
            status: 'pending',
            expiresAt: { $gt: new Date() }
        }).toArray()

        return res.json({ invites: pendingInvites })
    } catch (e) {
        console.error('Error fetching handshake invites:', e)
        res.status(500).json({ error: 'Internal server error' })
    }
}

// POST /handshake-invites/:inviteId/accept
export const acceptHandshakeInvite = async (req, res) => {
    try {
        const { handshakeInvites, rooms } = getCollections()
        const { inviteId } = req.params
        const { email } = req.user
        const { code } = req.body

        if (!ObjectId.isValid(inviteId) || !code) {
            return res.status(400).json({ error: 'Invalid input' })
        }

        const invite = await handshakeInvites.findOne({ _id: new ObjectId(inviteId) })
        if (!invite) return res.status(404).json({ error: 'Invite not found' })

        if (invite.inviteeEmail !== email.toLowerCase()) {
            return res.status(403).json({ error: 'You are not authorized to accept this invite' })
        }

        if (invite.status !== 'pending' || new Date() > invite.expiresAt) {
            return res.status(410).json({ error: 'Invite expired or already processed' })
        }

        if (invite.passcode !== code.toString()) {
            return res.status(401).json({ error: 'Incorrect passcode' })
        }

        // Successfully matched!
        await handshakeInvites.updateOne(
            { _id: invite._id },
            { $set: { status: 'accepted', acceptedAt: new Date() } }
        )

        // Add to collaborators mapping the new struct if required
        await rooms.updateOne(
            { _id: invite.roomId },
            {
                $addToSet: {
                    collaborators: { email: invite.inviteeEmail, role: 'contributor' }
                }
            }
        )

        return res.json({ success: true, roomId: invite.roomId.toString() })

    } catch (e) {
        console.error('Error accepting handshake invite:', e)
        res.status(500).json({ error: 'Internal server error' })
    }
}

// POST /handshake-invites/:inviteId/decline
export const declineHandshakeInvite = async (req, res) => {
    try {
        const { handshakeInvites } = getCollections()
        const { inviteId } = req.params
        const { email } = req.user

        if (!ObjectId.isValid(inviteId)) {
            return res.status(400).json({ error: 'Invalid invite ID' })
        }

        const invite = await handshakeInvites.findOne({ _id: new ObjectId(inviteId) })
        if (!invite) return res.status(404).json({ error: 'Invite not found' })

        if (invite.inviteeEmail !== email.toLowerCase()) {
            return res.status(403).json({ error: 'Not authorized' })
        }

        if (invite.status !== 'pending') {
            return res.status(410).json({ error: 'Invite already processed' })
        }

        await handshakeInvites.updateOne(
            { _id: invite._id },
            { $set: { status: 'declined', declinedAt: new Date() } }
        )

        return res.json({ success: true })
    } catch (e) {
        console.error('Error declining handshake invite:', e)
        res.status(500).json({ error: 'Internal server error' })
    }
}

// GET /rooms/:id/qr
export const getRoomQRCode = async (req, res) => {
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

        // Verify access (owners and collaborators)
        const normalEmail = email.toLowerCase()
        const isOwner = room.ownerEmail === normalEmail
        const isCollaborator = room.collaborators?.some(c => (typeof c === 'string' ? c : c.email)?.toLowerCase() === normalEmail)
        
        // Technically, anyone could view the QR, but we restrict it to room members to be safe
        if (!isOwner && !isCollaborator) {
            return res.status(403).json({ error: 'Only room members can generate a room QR code' })
        }

        // Encode as HTTPS URL — works on any camera app (iOS or Android) without needing
        // the custom bestbefore: URI scheme to be pre-registered. The /join/:id landing page
        // handles the native deep-link redirect for both platforms.
        const qrData = `https://bestbefore.up.railway.app/join/${id}`
        
        // Generate QR code as Base64 image with high quality settings
        const qrCodeDataUrl = await QRCode.toDataURL(qrData, {
            errorCorrectionLevel: 'H',
            margin: 4,
            width: 600
        })
        
        return res.json({ 
            success: true, 
            roomId: id,
            qrCode: qrCodeDataUrl 
        })
    } catch (e) {
        console.error('Error generating QR code:', e)
        res.status(500).json({ error: 'Failed to generate QR code' })
    }
}

// POST /rooms/:id/join-via-qr — Authenticated: join room after scanning QR
export const joinViaQR = async (req, res) => {
    try {
        const { rooms, users } = getCollections()
        const { id } = req.params
        const { userId, email } = req.user

        if (!ObjectId.isValid(id)) {
            return res.status(400).json({ error: 'Invalid room ID' })
        }

        const room = await rooms.findOne({ _id: new ObjectId(id) })
        if (!room) {
            return res.status(404).json({ error: 'Room not found', code: 'ROOM_NOT_FOUND' })
        }

        // Check if already a member
        const normalEmail = email.toLowerCase()
        const isOwner = room.ownerEmail === normalEmail
        const isAlreadyCollaborator = room.collaborators?.some(c => {
            const colEmail = (typeof c === 'string' ? c : c.email)?.toLowerCase()
            return colEmail === normalEmail
        })

        if (isOwner || isAlreadyCollaborator) {
            return res.json({
                success: true,
                message: 'You are already a member of this room',
                roomId: room._id.toString(),
                roomName: room.name,
                alreadyMember: true
            })
        }

        // Add user as pending collaborator
        await rooms.updateOne(
            { _id: new ObjectId(id) },
            {
                $addToSet: { pendingCollaborators: normalEmail }
            }
        )

        // Notify room owner
        if (admin && room.ownerEmail && room.ownerEmail !== normalEmail) {
            try {
                const owner = await users.findOne({ email: room.ownerEmail, fcmToken: { $exists: true, $ne: null } })
                if (owner?.fcmToken) {
                    const userName = normalEmail.split('@')[0]
                    await admin.messaging().send({
                        notification: {
                            title: 'New Join Request!',
                            body: `${userName} wants to join "${room.name}" via QR code`
                        },
                        data: {
                            type: 'QR_JOIN_REQUESTED',
                            roomId: room._id.toString(),
                            roomName: room.name,
                            requesterEmail: normalEmail
                        },
                        token: owner.fcmToken
                    })
                }
            } catch (err) {
                console.error('Failed to notify room owner:', err)
            }
        }

        res.json({
            success: true,
            roomId: room._id.toString(),
            roomName: room.name,
            alreadyMember: false
        })
    } catch (e) {
        console.error('Error joining via QR:', e)
        res.status(500).json({ error: 'Failed to join room' })
    }
}

// GET /join/:id — Public redirect landing page
export const joinRoomRedirect = async (req, res) => {
    try {
        const { id } = req.params
        
        // Simple HTML with redirect logic
        const html = `
        <!DOCTYPE html>
        <html>
        <head>
            <title>Join BestBefore Room</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    background: #000;
                    color: white;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    height: 100vh;
                    text-align: center;
                }
                .card {
                    padding: 40px;
                    background: rgba(255, 255, 255, 0.05);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    border-radius: 32px;
                    max-width: 320px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                }
                h1 { font-size: 24px; margin-bottom: 20px; }
                p { font-size: 16px; color: rgba(255,255,255,0.6); margin-bottom: 30px; }
                .button {
                    display: inline-block;
                    padding: 16px 32px;
                    background: #007AFF;
                    color: white;
                    text-decoration: none;
                    border-radius: 100px;
                    font-weight: bold;
                    transition: transform 0.2s;
                }
                .button:active { transform: scale(0.95); }
                .logo { font-size: 60px; margin-bottom: 20px; filter: drop-shadow(0 0 10px #007AFF); }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="logo">▽</div>
                <h1>BestBefore</h1>
                <p>Welcome! You've been invited to a memory room.</p>
                <a href="bestbefore://room/${id}" class="button" id="openBtn">Open in App</a>
            </div>

            <script>
                // Autoreply after 1 second if on mobile
                window.onload = function() {
                    setTimeout(function() {
                        window.location.href = "bestbefore://room/${id}";
                    }, 1000);
                }
            </script>
        </body>
        </html>
        `
        res.header('Content-Type', 'text/html').send(html)
    } catch (e) {
        console.error('Error in joinRoomRedirect:', e)
        res.status(500).send('Something went wrong')
    }
}
