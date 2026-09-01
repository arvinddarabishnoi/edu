package com.mindnova.edutopia.core.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mindnova.edutopia.core.utils.YouTubeUtils

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    videoId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cleanVideoId = YouTubeUtils.extractVideoId(videoId)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        if (cleanVideoId.isNotBlank()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.pluginState = WebSettings.PluginState.ON
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        setBackgroundColor(android.graphics.Color.BLACK)
                        loadDataWithBaseURL(
                            "https://www.youtube.com",
                            YouTubeUtils.buildEmbedHtml(cleanVideoId),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(
                        "https://www.youtube.com",
                        YouTubeUtils.buildEmbedHtml(cleanVideoId),
                        "text/html",
                        "UTF-8",
                        null
                    )
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
