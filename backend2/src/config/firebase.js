import admin from 'firebase-admin'
import fs from 'fs'
import path from 'path'

// 1. Check for Environment Variable first (Production / Railway)
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    try {
        const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
        })
        console.log('Firebase Admin Initialized (using FIREBASE_SERVICE_ACCOUNT env var)')
    } catch (e) {
        console.error('Failed to parse FIREBASE_SERVICE_ACCOUNT env var. Ensure it is valid JSON.', e.message)
    }
}
// 2. Fallback to local file (Local Testing)
else {
    const serviceAccountPath = path.resolve(process.cwd(), 'service-account.json')
    if (fs.existsSync(serviceAccountPath)) {
        try {
            const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'))
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            })
            console.log('Firebase Admin Initialized (using local service-account.json)')
        } catch (e) {
            console.error('Failed to parse service-account.json file.', e.message)
        }
    } else {
        console.warn('WARNING: FIREBASE_SERVICE_ACCOUNT env var and service-account.json not found. Firebase Auth/Push will fail.')
    }
}

export default admin
