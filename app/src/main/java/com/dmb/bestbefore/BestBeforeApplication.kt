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
        com.dmb.bestbefore.data.api.RetrofitClient.init(this)
        FirebaseApp.initializeApp(this)
        com.dmb.bestbefore.analytics.AnalyticsManager.init(this)
        initCoil()
    }

    private fun initCoil() {
        try {
            val coilOkHttpClient = okhttp3.OkHttpClient.Builder()
                .cache(okhttp3.Cache(cacheDir.resolve("coil_http_cache"), 50L * 1024 * 1024))
                .build()

            Coil.setImageLoader(
                ImageLoader.Builder(this)
                    .okHttpClient(coilOkHttpClient)
                    .components {
                        add(coil.map.Mapper<String, String> { data, _ ->
                            if (data.startsWith("/")) {
                                com.dmb.bestbefore.data.api.RetrofitClient.BASE_URL.removeSuffix("/") + data
                            } else {
                                data
                            }
                        })
                    }
                    .memoryCache {
                        MemoryCache.Builder(this)
                            .maxSizePercent(0.25)
                            .strongReferencesEnabled(true)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(cacheDir.resolve("coil_image_cache"))
                            .maxSizeBytes(150L * 1024 * 1024)
                            .build()
                    }
                    .crossfade(false)
                    .respectCacheHeaders(false)
                    .build()
            )
        } catch (e: Exception) {
            Log.e("BestBeforeApp", "Coil init failed, using defaults: ${e.message}")
        }
    }
}
