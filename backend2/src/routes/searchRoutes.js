import express from 'express'
import { authMiddleware } from '../middleware/authMiddleware.js'
import { semanticSearchRooms } from '../controllers/semanticSearchController.js'

const router = express.Router()

router.post('/search', authMiddleware, semanticSearchRooms)

export default router
