package com.mindnova.edutopia.domain

import com.mindnova.edutopia.domain.services.GamificationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GamificationServiceTest {

    @Test
    fun `level 1 boundary and monotonic thresholds`() {
        assertEquals(1, GamificationService.calculateLevel(0))
        assertEquals(1, GamificationService.calculateLevel(-100))
        assertEquals(0L, GamificationService.xpRequiredForLevel(1))
        val l2 = GamificationService.xpRequiredForLevel(2)
        assertTrue(l2 > 0)
        assertEquals(1, GamificationService.calculateLevel(l2 - 1))
        assertEquals(2, GamificationService.calculateLevel(l2))
        // strictly increasing thresholds
        for (i in 1 until 10) {
            assertTrue(GamificationService.xpRequiredForLevel(i + 1) > GamificationService.xpRequiredForLevel(i))
        }
    }

    @Test
    fun `level caps for absurd xp`() {
        assertEquals(GamificationService.MAX_LEVEL, GamificationService.calculateLevel(Long.MAX_VALUE / 2))
    }

    @Test
    fun `progress is bounded 0..1 and remaining never negative`() {
        for (xp in 0L..5000L step 137) {
            val (progress, remaining) = GamificationService.calculateLevelProgress(xp)
            assertTrue("progress in range ($xp)", progress in 0f..1f)
            assertTrue("remaining >= 0 ($xp)", remaining >= 0L)
        }
    }

    @Test
    fun `streak keeps on same day and increments on next day`() {
        val now = System.currentTimeMillis()
        // active today -> unchanged (at least 1)
        assertEquals(7, GamificationService.evaluateStreak(7, now))
        assertEquals(1, GamificationService.evaluateStreak(0, now))

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        assertEquals(8, GamificationService.evaluateStreak(7, yesterday.timeInMillis))

        val twoDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2) }
        assertEquals(1, GamificationService.evaluateStreak(7, twoDaysAgo.timeInMillis))
    }

    @Test
    fun `streak resets when never active before`() {
        assertEquals(1, GamificationService.evaluateStreak(9, 0L))
        assertEquals(1, GamificationService.evaluateStreak(9, -1L))
    }

    @Test
    fun `future clock skew does not break the streak`() {
        val future = System.currentTimeMillis() + 60L * 60_000
        assertEquals(5, GamificationService.evaluateStreak(5, future))
    }
}
