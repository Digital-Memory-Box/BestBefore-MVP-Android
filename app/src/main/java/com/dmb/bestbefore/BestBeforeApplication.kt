package com.dmb.bestbefore

import android.app.Application
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp

class BestBeforeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        initCoil()
    }

    private fun initCoil() {
        try {
            Coil.setImageLoader(
                ImageLoader.Builder(this)
                    .memoryCache {
                        MemoryCache.Builder(this)
                            .maxSizePercent(0.20)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(cacheDir.resolve("coil_image_cache"))
                            .maxSizeBytes(100L * 1024 * 1024)
                            .build()
                    }
                    .crossfade(true)
                    .build()
            )
        } catch (e: Exception) {
            Log.e("BestBeforeApp", "Coil init failed, using defaults: ${e.message}")
        }
    }
}
