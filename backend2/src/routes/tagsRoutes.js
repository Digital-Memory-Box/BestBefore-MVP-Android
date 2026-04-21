import express from 'express'
import { authMiddleware } from '../middleware/authMiddleware.js'
import { getAllTags, getFlatTags } from '../controllers/tagsController.js'

const router = express.Router()

router.get('/', authMiddleware, getAllTags)       // grouped by category
router.get('/flat', authMiddleware, getFlatTags)  // flat array

export default router
