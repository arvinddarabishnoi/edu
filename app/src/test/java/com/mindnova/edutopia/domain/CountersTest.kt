package com.mindnova.edutopia.domain

import com.mindnova.edutopia.domain.services.Counters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountersTest {

    // ── Batch enrollment (idempotent) ────────────────────────────────
    @Test
    fun `re-enrolling into the same batch is a no-op`() {
        assertEquals(
            Counters.EnrollmentAction.NoOp,
            Counters.enrollAction("batch1", "batch1")
        )
    }

    @Test
    fun `fresh join vs move are distinguished`() {
        assertEquals(
            Counters.EnrollmentAction.JoinNew,
            Counters.enrollAction("", "batch2")
        )
        assertEquals(
            Counters.EnrollmentAction.MoveFromOld,
            Counters.enrollAction("batch1", "batch2")
        )
        assertEquals(
            Counters.EnrollmentAction.NoOp,
            Counters.enrollAction("batch1", "")
        )
    }

    @Test
    fun `counts never go negative`() {
        assertEquals(0L, Counters.applyDelta(0, -1))
        assertEquals(4L, Counters.applyDelta(5, -1))
        assertEquals(6L, Counters.applyDelta(5, +1))
    }

    // ── Lecture counts ───────────────────────────────────────────────
    @Test
    fun `create increments once, edit same series does nothing`() {
        assertEquals(true to false, Counters.seriesCountChanges(isCreate = true, oldSeriesId = null, newSeriesId = "s1"))
        assertEquals(false to false, Counters.seriesCountChanges(isCreate = false, oldSeriesId = "s1", newSeriesId = "s1"))
    }

    @Test
    fun `series move decrements old and increments new`() {
        assertEquals(true to true, Counters.seriesCountChanges(isCreate = false, oldSeriesId = "s1", newSeriesId = "s2"))
    }

    @Test
    fun `create without series increments nothing`() {
        assertEquals(false to false, Counters.seriesCountChanges(isCreate = true, oldSeriesId = null, newSeriesId = ""))
    }

    // ── XP farming protection ────────────────────────────────────────
    @Test
    fun `lecture completion credit only when no prior record`() {
        assertTrue(Counters.shouldAwardFirstTimeCredit(existingRecordPresent = false))
        assertFalse(Counters.shouldAwardFirstTimeCredit(existingRecordPresent = true))
    }

    @Test
    fun `pyq xp awarded only on first correct, repeats give nothing`() {
        assertTrue(Counters.pyqShouldAwardXp(wasAlreadyCorrect = false, isCorrectNow = true))
        assertFalse(Counters.pyqShouldAwardXp(wasAlreadyCorrect = true, isCorrectNow = true))
        assertFalse(Counters.pyqShouldAwardXp(wasAlreadyCorrect = false, isCorrectNow = false))
    }

    @Test
    fun `pyq unique solver count moves only on first attempt`() {
        assertEquals(1L, Counters.pyqUniqueSolverDelta(firstAttemptForUser = true))
        assertEquals(0L, Counters.pyqUniqueSolverDelta(firstAttemptForUser = false))
    }

    // ── Test submission idempotency ──────────────────────────────────
    @Test
    fun `duplicate submission detected by deterministic key`() {
        val key = Counters.submissionKey("u1", "t1", 123L)
        assertEquals("u1_t1_123", key)
        // second tap with same attempt start produces the SAME key -> collision
        assertEquals(key, Counters.submissionKey("u1", "t1", 123L))
        // a NEW attempt (different startedAt) produces a different key
        assertFalse(key == Counters.submissionKey("u1", "t1", 456L))
        assertTrue(Counters.isDuplicateSubmission(existingResultDocumentPresent = true))
        assertFalse(Counters.isDuplicateSubmission(existingResultDocumentPresent = false))
    }
}
