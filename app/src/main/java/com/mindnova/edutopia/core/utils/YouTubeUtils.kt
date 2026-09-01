package com.mindnova.edutopia.core.utils

import java.util.regex.Pattern

object YouTubeUtils {

    private val YOUTUBE_ID_PATTERN = Pattern.compile(
        "^.*(?:youtu.be\\/|v\\/|e\\/|u\\/\\w+\\/|embed\\/|v=)([^#\\&\\?]*).*"
    )

    /**
     * Extracts YouTube Video ID from any valid YouTube URL.
     * Supports formats:
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://www.youtube.com/embed/VIDEO_ID
     * - VIDEO_ID directly
     */
    fun extractVideoId(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val trimmed = url.trim()
        if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains("?")) {
            return trimmed
        }
        val matcher = YOUTUBE_ID_PATTERN.matcher(trimmed)
        return if (matcher.matches() && matcher.group(1) != null && matcher.group(1)!!.length == 11) {
            matcher.group(1)!!
        } else {
            trimmed
        }
    }

    /**
     * Derives YouTube Thumbnail URL from Video ID or URL.
     */
    fun getThumbnailUrl(videoUrlOrId: String?): String {
        val videoId = extractVideoId(videoUrlOrId)
        return if (videoId.isNotBlank()) {
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        } else {
            ""
        }
    }

    /**
     * Builds clean HTML5 YouTube iframe embedding with responsive styling.
     */
    fun buildEmbedHtml(videoId: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000; }
                    html, body { width: 100%; height: 100%; overflow: hidden; }
                    .video-container { position: relative; width: 100%; height: 100%; }
                    iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <div class="video-container">
                    <iframe 
                        src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1" 
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                        allowfullscreen>
                    </iframe>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
