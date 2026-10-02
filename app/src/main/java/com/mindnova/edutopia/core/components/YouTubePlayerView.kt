package com.mindnova.edutopia.core.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.utils.YouTubeUtils

/**
 * Renders a YouTube video through an embedded WebView.
 *
 * - The WebView is keyed on [videoId]: navigating to a different lecture
 *   recreates it once, while ordinary recompositions never reload the current
 *   video (no restarts / flicker).
 * - Invalid/empty ids render an inline, actionable message instead of a black hole.
 * - Resources are released when the composable leaves the composition.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    videoId: String,
    modifier: Modifier = Modifier
) {
    val cleanVideoId = YouTubeUtils.extractVideoId(videoId)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        if (cleanVideoId.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No video link set for this lecture yet.",
                    color = TextWhiteMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            key(cleanVideoId) {
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
                            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
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
                    update = { /* intentionally empty: reload happens only via key() */ },
                    modifier = Modifier.matchParentSize(),
                    onRelease = { webView ->
                        webView.stopLoading()
                        webView.destroy()
                    }
                )
            }
        }
    }
}
