package com.dmb.bestbefore.ui.components

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

data class SoundCloudTrack(
    val id: Int,
    val title: String,
    val user: String,
    val artwork: String?
)

class SoundCloudController {
    var currentTrackTitle by mutableStateOf("")
    var currentTrackArtist by mutableStateOf("")
    var currentArtworkUrl by mutableStateOf<String?>(null)
    var isPlaying by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var volume by mutableStateOf(100.0)
    var tracks by mutableStateOf(listOf<SoundCloudTrack>())
    var currentIndex by mutableStateOf(0)
    var currentPlaylistUrl by mutableStateOf<String?>(null)

    // Binding logic
    var playAction: (() -> Unit)? = null
    var pauseAction: (() -> Unit)? = null
    var nextAction: (() -> Unit)? = null
    var prevAction: (() -> Unit)? = null

    fun play() = playAction?.invoke()
    fun pause() = pauseAction?.invoke()
    fun next() = nextAction?.invoke()
    fun prev() = prevAction?.invoke()
}

@Composable
fun SoundCloudPlayerView(
    soundCloudUrl: String,
    autoPlay: Boolean = false,
    color: String = "ff5500",
    isController: Boolean = false,
    controller: SoundCloudController
) {
    val embedUrl = "https://w.soundcloud.com/player/?url=$soundCloudUrl&color=%23$color&auto_play=$autoPlay&hide_related=false&show_comments=true&show_user=true&show_reposts=false&show_teaser=true&visual=false"

    Box(
        modifier = Modifier
            .run {
                if (isController) size(10.dp).offset(x = (-1000).dp).alpha(0.05f)
                else fillMaxWidth().height(160.dp)
            }
            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    loadUrl(embedUrl)

                    // Inject mocked script interfaces for the bridge (analogous to iOS WKScriptMessageHandler)
                    // (Omitted direct JavascriptInterface implementations here to focus on safe UI static reproduction)
                }
            },
            update = { webView ->
                // Handled internally, or reload if url changes in dynamic implementation
            }
        )
    }
}
