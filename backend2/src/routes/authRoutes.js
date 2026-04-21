import express from 'express'
import { authMiddleware } from '../middleware/authMiddleware.js'
import { syncAuth, getMe, updateMe, searchUsers } from '../controllers/authController.js'

const router = express.Router()

router.post('/sync', authMiddleware, syncAuth)

// Mounted at both /auth and /me in app.js — use root path so /me resolves correctly
router.get('/', authMiddleware, getMe)
router.patch('/', authMiddleware, updateMe)

// User search — GET /users/search?q=<query>
router.get('/search', authMiddleware, searchUsers)

export default router
