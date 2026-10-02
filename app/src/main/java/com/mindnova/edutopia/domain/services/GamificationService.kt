package com.mindnova.edutopia.domain.services

import java.util.Calendar

/**
 * Level thresholds: XP required to reach level N = round(400 * (N-1)^1.45).
 * Level 1 starts at 0 XP. Monotonically increasing, so calculateLevel is a
 * simple bounded walk over the curve.
 */
object GamificationService {

    /** Hard ceiling to keep the level loop O(1) for absurd XP inputs. */
    const val MAX_LEVEL = 100

    fun xpRequiredForLevel(level: Int): Long {
        if (level <= 1) return 0L
        return Math.round(400.0 * Math.pow((level - 1).toDouble(), 1.45))
    }

    /**
     * Calculates current level from total XP.
     * Guarantees: level >= 1, and level <= MAX_LEVEL.
     */
    fun calculateLevel(xp: Long): Int {
        if (xp <= 0) return 1
        var level = 1
        while (level < MAX_LEVEL && xpRequiredForLevel(level + 1) <= xp) {
            level++
        }
        return level
    }

    /**
     * Returns (progressFraction 0..1 toward next level, XP remaining to next level).
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
     * Streak rule:
     *  - active today            -> keep current streak (min 1)
     *  - last active yesterday   -> streak + 1
     *  - any earlier (or never)  -> reset to 1
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

        // Future timestamps (clock skew / timezone edge): treat as already active today.
        if (lastActive.timeInMillis > now.timeInMillis) {
            return currentStreak.coerceAtLeast(1)
        }

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
