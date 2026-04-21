import express from 'express';
import { getPlaylist, streamTrack } from '../controllers/soundcloudController.js';

const router = express.Router();

router.get('/playlist', getPlaylist);
router.get('/stream/:trackId', streamTrack);

export default router;
