package com.mindnova.edutopia.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    private val defaultDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val defaultTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val defaultDateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun formatDate(timestampMs: Long): String {
        if (timestampMs <= 0) return ""
        return defaultDateFormat.format(Date(timestampMs))
    }

    fun formatTime(timestampMs: Long): String {
        if (timestampMs <= 0) return ""
        return defaultTimeFormat.format(Date(timestampMs))
    }

    fun formatDateTime(timestampMs: Long): String {
        if (timestampMs <= 0) return ""
        return defaultDateTimeFormat.format(Date(timestampMs))
    }

    /**
     * Formats seconds into HH:MM:SS countdown timer format.
     */
    fun formatTimerCountdown(secondsRemaining: Long): String {
        val hrs = TimeUnit.SECONDS.toHours(secondsRemaining)
        val mins = TimeUnit.SECONDS.toMinutes(secondsRemaining) % 60
        val secs = secondsRemaining % 60
        return if (hrs > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }

    /**
     * Formats duration in minutes to readable string (e.g. 180 min -> "3 Hours", 90 min -> "1 hr 30 mins").
     */
    fun formatDurationMinutes(minutes: Int): String {
        if (minutes <= 0) return "0 mins"
        val hrs = minutes / 60
        val remainingMins = minutes % 60
        return when {
            hrs > 0 && remainingMins == 0 -> "$hrs ${if (hrs == 1) "Hour" else "Hours"}"
            hrs > 0 -> "$hrs hr $remainingMins mins"
            else -> "$remainingMins mins"
        }
    }

    /**
     * Formats greeting based on the current hour of the day.
     */
    fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..22 -> "Good Evening"
            else -> "Good Night"
        }
    }
}
