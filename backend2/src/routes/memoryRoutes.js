import express from 'express'
import { authMiddleware } from '../middleware/authMiddleware.js'
import { getTrendingMemories, getMemoryCount, getMemoriesByRoom, addMemoryToRoom } from '../controllers/memoryController.js'

const router = express.Router()

router.get('/trending', authMiddleware, getTrendingMemories)
router.get('/count', authMiddleware, getMemoryCount)

export default router
