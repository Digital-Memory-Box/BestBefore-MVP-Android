package com.dmb.bestbefore

import android.app.Application
import com.google.firebase.FirebaseApp

class BestBeforeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        // NOTE: App Check is intentionally disabled during development.
        // To re-enable for production, register the debug token in Firebase Console
        // and restore FirebaseAppCheck.installAppCheckProviderFactory(...)
        FirebaseApp.initializeApp(this)
    }
}
