package com.mindnova.edutopia.domain.services

import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.sqrt

object GamificationService {

    /**
     * Level thresholds calculation formula:
     * XP required for Level N = 400 * (N-1)^1.45 rounded
     */
    fun xpRequiredForLevel(level: Int): Long {
        if (level <= 1) return 0L
        return (400.0 * Math.pow((level - 1).toDouble(), 1.45)).toLong()
    }

    /**
     * Calculates current level from total XP.
     */
    fun calculateLevel(xp: Long): Int {
        if (xp <= 0) return 1
        var level = 1
        while (xpRequiredForLevel(level + 1) <= xp) {
            level++
        }
        return level
    }

    /**
     * Returns pair of (progressFraction: Float 0..1, xpToNextLevel: Long)
     */
    fun calculateLevelProgress(xp: Long): Pair<Float, Long> {
        val currentLevel = calculateLevel(xp)
        val currentLevelXp = xpRequiredForLevel(currentLevel)
        val nextLevelXp = xpRequiredForLevel(currentLevel + 1)

        val xpInCurrentLevel = (xp - currentLevelXp).coerceAtLeast(0L)
        val levelSpan = (nextLevelXp - currentLevelXp).coerceAtLeast(1L)
        val progress = (xpInCurrentLevel.toFloat() / levelSpan.toFloat()).coerceIn(0f, 1f)
        val remaining = (nextLevelXp - xp).coerceAtLeast(0L)
        return Pair(progress, remaining)
    }

    /**
     * Evaluates updated streak based on last active timestamp.
     */
    fun evaluateStreak(currentStreak: Int, lastActiveTimestamp: Long): Int {
        if (lastActiveTimestamp <= 0) return 1

        val now = Calendar.getInstance()
        val lastActive = Calendar.getInstance().apply { timeInMillis = lastActiveTimestamp }

        val sameDay = now.get(Calendar.YEAR) == lastActive.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == lastActive.get(Calendar.DAY_OF_YEAR)

        if (sameDay) {
            return currentStreak.coerceAtLeast(1)
        }

        // Check if yesterday
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == lastActive.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == lastActive.get(Calendar.DAY_OF_YEAR)

        return if (isYesterday) {
            currentStreak + 1
        } else {
            1
        }
    }
}
