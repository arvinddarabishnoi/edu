package com.mindnova.edutopia.domain.services

/**
 * Pure counter & idempotency rules shared by the repositories so the exact
 * business semantics (no double counting, no negative counts, no XP farming)
 * are unit-testable without a Firestore emulator.
 */
object Counters {

    enum class EnrollmentAction { NoOp, JoinNew, MoveFromOld }

    /**
     * Decide what an enrollment write should do given the user's current batch.
     * Same batch -> NoOp (idempotent: never increments studentCount twice).
     */
    fun enrollAction(currentBatchId: String, targetBatchId: String): EnrollmentAction = when {
        targetBatchId.isBlank() -> EnrollmentAction.NoOp
        currentBatchId == targetBatchId -> EnrollmentAction.NoOp
        currentBatchId.isBlank() -> EnrollmentAction.JoinNew
        else -> EnrollmentAction.MoveFromOld
    }

    /** Apply +1/-1 with a floor of zero. */
    fun applyDelta(current: Long, delta: Long): Long = (current + delta).coerceAtLeast(0L)

    /**
     * Series lectureCount maintenance:
     *  - create with a series      -> increment once
     *  - edit same series          -> nothing
     *  - edit moving series        -> decrement old, increment new
     *  - delete                    -> decrement old once
     */
    fun seriesCountChanges(
        isCreate: Boolean,
        oldSeriesId: String?,
        newSeriesId: String?
    ): Pair<Boolean, Boolean> { // (incrementNew, decrementOld)
        val newId = newSeriesId.orEmpty()
        val oldId = oldSeriesId.orEmpty()
        return when {
            isCreate -> (newId.isNotBlank() to false)
            newId == oldId -> (false to false)
            else -> (newId.isNotBlank() to oldId.isNotBlank())
        }
    }

    /** XP/completion credit is granted only when no record existed before. */
    fun shouldAwardFirstTimeCredit(existingRecordPresent: Boolean): Boolean = !existingRecordPresent

    /** PYQ: population counter only for the user's first-ever attempt. */
    fun pyqUniqueSolverDelta(firstAttemptForUser: Boolean): Long = if (firstAttemptForUser) 1L else 0L

    /** PYQ: XP only on the user's first CORRECT answer, never on repeats. */
    fun pyqShouldAwardXp(wasAlreadyCorrect: Boolean, isCorrectNow: Boolean): Boolean =
        isCorrectNow && !wasAlreadyCorrect

    /** A stored result doc with the same deterministic submission key = duplicate. */
    fun isDuplicateSubmission(existingResultDocumentPresent: Boolean): Boolean =
        existingResultDocumentPresent

    /** Deterministic submission key so double-taps collide instead of duplicating. */
    fun submissionKey(userId: String, testId: String, startedAt: Long): String =
        "${userId}_${testId}_${startedAt}"
}
