# ==============================================================================
# BestBefore Android - ProGuard & R8 Optimization / Keep Rules
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Line Numbers & Stack Trace Preservation
# ------------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------------------
# 2. Gson & Data Models (Reflection Safety)
# ------------------------------------------------------------------------------
# Keep all data models and DTOs that are serialized/deserialized via reflection
-keep class com.dmb.bestbefore.data.api.models.** { *; }
-keep class com.dmb.bestbefore.data.models.** { *; }
-keep class com.dmb.bestbefore.data.ai.** { *; }

# Keep SerializedName annotations and Gson custom type adapters
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * extends com.google.gson.TypeAdapter { *; }

# ------------------------------------------------------------------------------
# 3. Retrofit & OkHttp
# ------------------------------------------------------------------------------
-keep class com.dmb.bestbefore.data.api.ApiService { *; }
-keep class com.dmb.bestbefore.data.ai.AiServiceApi { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ------------------------------------------------------------------------------
# 4. Android Framework Components (Services, Receivers, App)
# ------------------------------------------------------------------------------
-keep class com.dmb.bestbefore.BestBeforeApplication { *; }
-keep class com.dmb.bestbefore.MainActivity { *; }
-keep class com.dmb.bestbefore.notifications.MyFirebaseMessagingService { *; }
-keep class com.dmb.bestbefore.notifications.MusicPlayerService { *; }
-keep class com.dmb.bestbefore.notifications.MusicPlayerService$* { *; }

# ------------------------------------------------------------------------------
# 5. Firebase & Google Play Services
# ------------------------------------------------------------------------------
# Core Firebase BOM consumer rules are applied via AAR; preserve critical auth callbacks
-dontwarn com.google.firebase.**
-dontwarn androidx.media3.**

# Keep only reflective BarcodeScanner entrypoints required by embedded activity
-keep class com.journeyapps.barcodescanner.CaptureActivity { *; }
-keep class com.journeyapps.barcodescanner.CompoundBarcodeView { *; }