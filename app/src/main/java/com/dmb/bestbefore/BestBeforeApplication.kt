package com.dmb.bestbefore

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class BestBeforeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        val factory = if (BuildConfig.DEBUG) {
            Log.d("BestBeforeApp", "App Check: Using DebugAppCheckProviderFactory")
            DebugAppCheckProviderFactory.getInstance()
        } else {
            Log.d("BestBeforeApp", "App Check: Using PlayIntegrityAppCheckProviderFactory")
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }

        firebaseAppCheck.installAppCheckProviderFactory(factory)
    }
}
