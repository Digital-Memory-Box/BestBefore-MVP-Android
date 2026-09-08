package com.dmb.bestbefore.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AnalyticsManager {
    private const val TAG = "AnalyticsManager"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var firebaseCrashlytics: FirebaseCrashlytics? = null

    fun init(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            firebaseCrashlytics = FirebaseCrashlytics.getInstance()
            firebaseCrashlytics?.setCrashlyticsCollectionEnabled(!com.dmb.bestbefore.BuildConfig.DEBUG)
            Log.d(TAG, "Firebase Analytics & Crashlytics initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Firebase Analytics: ${e.message}")
        }
    }

    // -- Screen Tracking -------------------------------------------------------
    fun logScreenView(screenName: String, screenClass: String = screenName) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            firebaseCrashlytics?.log("ScreenView: $screenName")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging screen view: ${e.message}")
        }
    }

    // -- User Identity & Segmentation ------------------------------------------
    fun setUserProperties(userId: String?, userType: String? = null) {
        try {
            if (userId != null) {
                firebaseAnalytics?.setUserId(userId)
                firebaseCrashlytics?.setUserId(userId)
            }
            if (userType != null) {
                firebaseAnalytics?.setUserProperty("user_tier", userType)
                firebaseCrashlytics?.setCustomKey("user_tier", userType)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting user properties: ${e.message}")
        }
    }

    // -- Auth Events -----------------------------------------------------------
    fun logLogin(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    fun logSignUp(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

    // -- Room & Memory Core Funnel Events --------------------------------------
    fun logCreateRoom(roomName: String, isPrivate: Boolean, capsuleDays: Int) {
        val bundle = Bundle().apply {
            putString("room_name", roomName.take(50))
            putBoolean("is_private", isPrivate)
            putInt("capsule_duration_days", capsuleDays)
        }
        firebaseAnalytics?.logEvent("create_room", bundle)
    }

    fun logAddMemory(roomId: String, memoryType: String) {
        val bundle = Bundle().apply {
            putString("room_id", roomId)
            putString("memory_type", memoryType)
        }
        firebaseAnalytics?.logEvent("add_memory", bundle)
    }

    fun logUnlockCapsule(roomId: String, roomName: String) {
        val bundle = Bundle().apply {
            putString("room_id", roomId)
            putString("room_name", roomName.take(50))
        }
        firebaseAnalytics?.logEvent("unlock_capsule", bundle)
    }

    fun logShareRoom(roomId: String, method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, roomId)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "room")
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SHARE, bundle)
    }

    // -- Safety & Moderation Events --------------------------------------------
    fun logReportContent(targetType: String, targetId: String, reason: String) {
        val bundle = Bundle().apply {
            putString("target_type", targetType)
            putString("target_id", targetId)
            putString("reason", reason)
        }
        firebaseAnalytics?.logEvent("report_content", bundle)
    }

    fun logBlockUser(blockedUserId: String) {
        val bundle = Bundle().apply {
            putString("blocked_user_id", blockedUserId)
        }
        firebaseAnalytics?.logEvent("block_user", bundle)
    }

    // -- Crashlytics Logging ---------------------------------------------------
    fun recordException(throwable: Throwable) {
        firebaseCrashlytics?.recordException(throwable)
    }

    fun log(message: String) {
        firebaseCrashlytics?.log(message)
    }
}
