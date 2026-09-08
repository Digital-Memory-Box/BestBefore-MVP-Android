package com.dmb.bestbefore.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dmb.bestbefore.MainActivity
import com.dmb.bestbefore.R
import com.dmb.bestbefore.data.api.RetrofitClient // To use the base url for the stream

class MusicPlayerService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackId: Long = 0L
    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

    companion object {
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "music_playback_channel"

        const val ACTION_PLAY = "com.dmb.bestbefore.action.PLAY"
        const val ACTION_PAUSE = "com.dmb.bestbefore.action.PAUSE"
        const val ACTION_RESUME = "com.dmb.bestbefore.action.RESUME"
        const val ACTION_STOP = "com.dmb.bestbefore.action.STOP"
        const val ACTION_NEXT = "com.dmb.bestbefore.action.NEXT"
        const val ACTION_PREVIOUS = "com.dmb.bestbefore.action.PREVIOUS"

        const val EXTRA_TRACK_ID = "track_id"
        const val EXTRA_TRACK_TITLE = "track_title"
        const val EXTRA_TRACK_ARTIST = "track_artist"
        const val EXTRA_TRACK_STREAM_URL = "track_stream_url"
        const val EXTRA_TRACK_ARTWORK = "track_artwork"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Immediately start foreground to satisfy the Android OS contract (must be called within 5 seconds of startForegroundService)
        startForegroundCompat(buildNotification(false, "BestBefore Music"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_PLAY -> {
                val trackId = intent.getLongExtra(EXTRA_TRACK_ID, 0L)
                val title = intent.getStringExtra(EXTRA_TRACK_TITLE) ?: "Unknown"
                val artist = intent.getStringExtra(EXTRA_TRACK_ARTIST) ?: "Unknown"
                val streamUrlPath = intent.getStringExtra(EXTRA_TRACK_STREAM_URL)

                currentTrackId = trackId
                currentTitle = title
                currentArtist = artist

                if (trackId != 0L && !streamUrlPath.isNullOrBlank()) {
                    val fullUrl = if (streamUrlPath.startsWith("http", ignoreCase = true)) {
                        streamUrlPath
                    } else {
                        val baseUrl = RetrofitClient.BASE_URL.dropLastWhile { it == '/' }
                        "$baseUrl$streamUrlPath"
                    }

                    startForegroundCompat(buildNotification(true, "Buffering..."))
                    startPlayback(fullUrl)
                } else {
                    stopPlayback()
                    stopSelf()
                }
            }
            ACTION_PAUSE -> {
                if (mediaPlayer?.isPlaying == true) {
                    try {
                        mediaPlayer?.pause()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    MusicPlayerManager.onPlaybackPaused()
                    updateNotification(false)
                }
            }
            ACTION_RESUME -> {
                if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                    try {
                        mediaPlayer?.start()
                        MusicPlayerManager.onPlaybackStarted()
                        updateNotification(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
            }
            ACTION_NEXT -> {
                MusicPlayerManager.playNext(this)
            }
            ACTION_PREVIOUS -> {
                MusicPlayerManager.playPrevious(this)
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startPlayback(url: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, android.os.PowerManager.PARTIAL_WAKE_LOCK)

                if (wifiLock == null) {
                    val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                    wifiLock = wifiManager?.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "BestBeforeMusicLock")
                }
                if (wifiLock?.isHeld == false) {
                    wifiLock?.acquire()
                }

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener(this@MusicPlayerService)
                setOnCompletionListener(this@MusicPlayerService)
                setOnErrorListener(this@MusicPlayerService)
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
            stopSelf()
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        try {
            mp?.start()
            MusicPlayerManager.onPlaybackStarted()
            updateNotification(true)
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
            stopSelf()
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        MusicPlayerManager.onTrackCompleted(this)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        stopPlayback()
        stopSelf()
        return true
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // Ignore if not initialized/playing
        }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        if (wifiLock?.isHeld == true) {
            try {
                wifiLock?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        MusicPlayerManager.onPlaybackStopped()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            // Ignored if service is not in foreground
        }
    }

    private fun updateNotification(isPlaying: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val title = if (currentTitle.isNotBlank()) currentTitle else "BestBefore Music"
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(isPlaying, title))
    }

    private fun buildNotification(isPlaying: Boolean, titleText: String): android.app.Notification {
        val prevIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPending = PendingIntent.getService(this, 4, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val prevAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_previous, "Previous", prevPending
        ).build()

        val playPauseAction = if (isPlaying) {
            val pauseIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause, "Pause", pausePending
            ).build()
        } else {
            val playIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_RESUME }
            val playPending = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play, "Play", playPending
            ).build()
        }

        val nextIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 5, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val nextAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_next, "Next", nextPending
        ).build()

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPending = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val artistText = if (currentArtist.isNotBlank()) currentArtist else "Playing music"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText(artistText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(mainPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background music playback controls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
