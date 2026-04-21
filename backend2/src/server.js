import dotenv from 'dotenv'
import app from './app.js'
import { connectDB } from './config/db.js'
import { checkExpiredRooms } from './cron/roomCron.js'

dotenv.config()

const port = Number(process.env.PORT) || 3000

// Initialize MongoDB then start server
connectDB().then(() => {
    app.listen(port, () => {
        console.log(`API listening on http://localhost:${port}`)

        // Check for expired rooms every 5 minutes
        setInterval(checkExpiredRooms, 5 * 60 * 1000)
        // Run once immediately on startup
        checkExpiredRooms()
    })
}).catch(err => {
    console.error('Failed to init server', err)
    process.exit(1)
})
