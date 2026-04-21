import express from 'express'
import { authMiddleware } from '../middleware/authMiddleware.js'
import { getRoomSuggestions, acceptSuggestion, rejectSuggestion } from '../controllers/suggestionController.js'

const router = express.Router()

router.get('/:roomId/suggestions', authMiddleware, getRoomSuggestions)
router.post('/:roomId/suggestions/:suggestionId/accept', authMiddleware, acceptSuggestion)
router.post('/:roomId/suggestions/:suggestionId/reject', authMiddleware, rejectSuggestion)

export default router
