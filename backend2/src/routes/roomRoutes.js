import express from 'express'
import { authMiddleware } from '../middleware/authMiddleware.js'
import { getDiscoverRooms, getRooms, getRoomById, createRoom, updateRoom, deleteRoom, acceptInvite, declineInvite, approveJoinRequest, denyJoinRequest, getInitialDiscoveryRooms } from '../controllers/roomController.js'

const router = express.Router()

router.get('/discover', authMiddleware, getDiscoverRooms)
router.post('/discover/initial', authMiddleware, getInitialDiscoveryRooms)
router.get('/', authMiddleware, getRooms)
router.get('/:id', authMiddleware, getRoomById)
router.post('/', authMiddleware, createRoom)
router.post('/:id/accept-invite', authMiddleware, acceptInvite)
router.post('/:id/decline-invite', authMiddleware, declineInvite)
router.post('/:id/approve-join', authMiddleware, approveJoinRequest)
router.post('/:id/deny-join', authMiddleware, denyJoinRequest)
router.patch('/:id', authMiddleware, updateRoom)
router.delete('/:id', authMiddleware, deleteRoom)

export default router
