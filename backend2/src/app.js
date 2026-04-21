import express from 'express'
import cors from 'cors'
import morgan from 'morgan'

import authRoutes from './routes/authRoutes.js'
import roomRoutes from './routes/roomRoutes.js'
import memoryRoutes from './routes/memoryRoutes.js'
import searchRoutes from './routes/searchRoutes.js'
import suggestionRoutes from './routes/suggestionRoutes.js'
import { getMemoriesByRoom, addMemoryToRoom, dumpMemories, getArchivedMemoriesByRoom } from './controllers/memoryController.js'
import { generateInviteToken, validateInviteToken, joinViaToken, createHandshakeInvite, getMyHandshakeInvites, acceptHandshakeInvite, declineHandshakeInvite, getRoomQRCode, joinViaQR, joinRoomRedirect } from './controllers/inviteController.js'
import { sendKnock } from './controllers/knockController.js'
import soundcloudRoutes from './routes/soundcloudRoutes.js'
import tagsRoutes from './routes/tagsRoutes.js'
import { authMiddleware } from './middleware/authMiddleware.js'

const app = express()

// Middleware
app.use(cors())
app.use(express.json({ limit: '20mb' }))
app.use(morgan('dev'))

// Health check
app.get('/health', (req, res) => res.json({ ok: true }))

// Route Mounts
// Map old `/auth/sync` and `/me` routes which were defined under authRoutes 
app.use('/auth', authRoutes)
app.use('/me', authRoutes) // /me defined in authRoutes

app.use('/rooms', roomRoutes)
app.use('/memories', memoryRoutes)
app.use('/search', searchRoutes)
app.use('/rooms', suggestionRoutes)
app.use('/api/soundcloud', soundcloudRoutes)
app.use('/tags', tagsRoutes)

// We define nested memory routes under rooms here to keep URL structure clean
// (i.e. /rooms/:roomId/memories)
app.get('/rooms/:roomId/memories', authMiddleware, getMemoriesByRoom)
app.post('/rooms/:roomId/memories', authMiddleware, addMemoryToRoom)
app.get('/rooms/:roomId/memories/archived', authMiddleware, getArchivedMemoriesByRoom)
app.post('/rooms/:roomId/dump', authMiddleware, dumpMemories)

// Invite token routes
app.post('/rooms/:id/invite-token', authMiddleware, generateInviteToken)
app.get('/invite/:token', validateInviteToken)          // Public — no auth
app.post('/invite/:token/join', authMiddleware, joinViaToken)

// Handshake invite routes
app.post('/rooms/:id/handshake-invites', authMiddleware, createHandshakeInvite)
app.get('/me/handshake-invites', authMiddleware, getMyHandshakeInvites)
app.post('/handshake-invites/:inviteId/accept', authMiddleware, acceptHandshakeInvite)
app.post('/handshake-invites/:inviteId/decline', authMiddleware, declineHandshakeInvite)

// Remote QR Generation
app.get('/rooms/:id/qr', authMiddleware, getRoomQRCode)

// QR Code Join
app.post('/rooms/:id/join-via-qr', authMiddleware, joinViaQR)

// Public Join/Redirect Page — room link (any platform)
app.get('/join/:id', joinRoomRedirect)

// Knock
app.post('/users/:email/knock', authMiddleware, sendKnock)

// Public Invite-Join Landing Page — invite token link (any platform)
app.get('/invite-join/:token', (req, res) => {
    const { token } = req.params
    const deepLink = `bestbefore://invite/${token}`
    const html = `
    <!DOCTYPE html>
    <html>
    <head>
        <title>Join BestBefore Room</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            body {
                margin: 0; padding: 0; background: #000; color: white;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                display: flex; flex-direction: column; align-items: center;
                justify-content: center; height: 100vh; text-align: center;
            }
            .card {
                padding: 40px; background: rgba(255,255,255,0.05);
                border: 1px solid rgba(255,255,255,0.1); border-radius: 32px;
                max-width: 320px; box-shadow: 0 10px 30px rgba(0,0,0,0.5);
            }
            h1 { font-size: 24px; margin-bottom: 20px; }
            p { font-size: 16px; color: rgba(255,255,255,0.6); margin-bottom: 30px; }
            .button {
                display: inline-block; padding: 16px 32px; background: #007AFF;
                color: white; text-decoration: none; border-radius: 100px;
                font-weight: bold; transition: transform 0.2s;
            }
            .button:active { transform: scale(0.95); }
            .logo { font-size: 60px; margin-bottom: 20px; filter: drop-shadow(0 0 10px #007AFF); }
        </style>
    </head>
    <body>
        <div class="card">
            <div class="logo">▽</div>
            <h1>BestBefore</h1>
            <p>You've been invited to a memory room!</p>
            <a href="${deepLink}" class="button" id="openBtn">Open in App</a>
        </div>
        <script>
            window.onload = function() {
                setTimeout(function() {
                    window.location.href = "${deepLink}";
                }, 800);
            }
        </script>
    </body>
    </html>`
    res.header('Content-Type', 'text/html').send(html)
})

export default app
