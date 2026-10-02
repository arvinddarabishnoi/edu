package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.docResourceFlow
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.Question
import com.mindnova.edutopia.data.models.TestAttempt
import com.mindnova.edutopia.data.models.TestModel
import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.domain.services.GamificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Outcome of submitAttempt so the UI can prove idempotency to the student. */
sealed interface SubmissionOutcome {
    data class Accepted(val resultId: String) : SubmissionOutcome
    data class Duplicate(val resultId: String) : SubmissionOutcome
}

class TestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val testsCollection = firestore.collection(Constants.COLL_TESTS)
    private val resultsCollection = firestore.collection(Constants.COLL_TEST_RESULTS)
    private val attemptsCollection = firestore.collection(Constants.COLL_TEST_ATTEMPTS)
    private val usersCollection = firestore.collection(Constants.COLL_USERS)

    fun getPublishedTestsFlow(): Flow<Resource<List<TestModel>>> =
        testsCollection
            .whereEqualTo("status", "published")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .listResourceFlow { doc ->
                doc.toObject(TestModel::class.java)?.copy(id = doc.id)
            }

    fun getAllTestsFlow(): Flow<Resource<List<TestModel>>> =
        testsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .listResourceFlow { doc ->
                doc.toObject(TestModel::class.java)?.copy(id = doc.id)
            }

    suspend fun getTestById(testId: String): Result<TestModel?> {
        return try {
            val doc = testsCollection.document(testId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(TestModel::class.java)?.copy(id = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    fun getQuestionsForTestFlow(testId: String): Flow<Resource<List<Question>>> =
        testsCollection.document(testId)
            .collection(Constants.COLL_QUESTIONS)
            .orderBy("order", Query.Direction.ASCENDING)
            .listResourceFlow { doc ->
                doc.toObject(Question::class.java)?.copy(id = doc.id)
            }

    suspend fun getQuestionsForTest(testId: String): Result<List<Question>> {
        return try {
            val subcoll = testsCollection.document(testId).collection(Constants.COLL_QUESTIONS)
            val snapshot = subcoll.orderBy("order", Query.Direction.ASCENDING).get().await()
            val questions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Question::class.java)?.copy(id = doc.id)
            }
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    fun getResultsForUserFlow(userId: String): Flow<Resource<List<TestResult>>> =
        resultsCollection
            .whereEqualTo("userId", userId)
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .limit(25)
            .listResourceFlow { doc ->
                doc.toObject(TestResult::class.java)?.copy(id = doc.id)
            }

    suspend fun getResultById(resultId: String): Result<TestResult?> {
        return try {
            val doc = resultsCollection.document(resultId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(TestResult::class.java)?.copy(id = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * The in-progress attempt (for resume protection). One doc per user+test,
     * overwritten as the student navigates; cleared on submit.
     */
    fun getActiveAttemptFlow(userId: String, testId: String): Flow<Resource<TestAttempt>> =
        attemptsCollection.document(attemptDocId(userId, testId))
            .docResourceFlow { doc ->
                doc.toObject(TestAttempt::class.java)?.copy(id = doc.id)
            }

    private fun attemptDocId(userId: String, testId: String): String = "${userId}_$testId"

    private fun resultDocId(userId: String, testId: String, startedAt: Long): String =
        com.mindnova.edutopia.domain.services.Counters.submissionKey(userId, testId, startedAt)

    /**
     * Persists (or updates) the in-progress attempt so a process death or
     * rotation can resume the same attempt. Only allowed while unsubmitted.
     */
    suspend fun saveInProgressAttempt(attempt: TestAttempt): Result<Unit> {
        if (attempt.userId.isBlank() || attempt.testId.isBlank()) {
            return Result.failure(Exception("Cannot persist an attempt without user and test ids."))
        }
        return try {
            attemptsCollection.document(attemptDocId(attempt.userId, attempt.testId))
                .set(attempt.copy(isSubmitted = false), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun saveTest(test: TestModel): Result<String> {
        return try {
            val docRef = if (test.id.isBlank()) {
                testsCollection.document()
            } else {
                testsCollection.document(test.id)
            }
            docRef.set(test.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Replaces the question set for a test:
     *  - deletes old question docs that are not in the new set
     *  - writes/updates questions preserving stable ids, re-sequencing order
     *  - updates totalQuestions atomically in the same batch
     */
    suspend fun saveQuestions(testId: String, questions: List<Question>): Result<Unit> {
        return try {
            val questionsSubcoll = testsCollection.document(testId).collection(Constants.COLL_QUESTIONS)
            val existingSnapshot = questionsSubcoll.get().await()
            val existingIds = existingSnapshot.documents.map { it.id }.toSet()
            val newIds = questions.map { it.id }.filter { it.isNotBlank() }.toSet()

            val batch = firestore.batch()

            // Delete questions that no longer belong to the test.
            for (id in existingIds - newIds) {
                batch.delete(questionsSubcoll.document(id))
            }

            // Write new/updated questions with stable ids.
            for ((index, q) in questions.withIndex()) {
                val qRef = if (q.id.isBlank()) {
                    questionsSubcoll.document()
                } else {
                    questionsSubcoll.document(q.id)
                }
                val prepared = q.copy(
                    id = qRef.id,
                    testId = testId,
                    order = index + 1,
                    questionNumber = index + 1
                )
                batch.set(qRef, prepared)
            }

            // Keep the parent's totalQuestions in sync in the same atomic batch.
            batch.update(testsCollection.document(testId), "totalQuestions", questions.size)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Atomic, idempotent submission. Deterministic document ids mean a double
     * tap (or retry after a partial failure) can never create duplicate
     * results, duplicate attempts, duplicate XP, or a double attemptsCount.
     *
     * Everything — result, attempt, test counter, user XP/level/stats, attempt
     * cleanup — happens in ONE transaction; any failure aborts the whole write
     * and is surfaced to the caller (nothing silently continues).
     */
    suspend fun submitAttempt(
        attempt: TestAttempt,
        result: TestResult,
        xpToAward: Long,
        pointsToAward: Long
    ): Result<SubmissionOutcome> {
        val userId = attempt.userId
        val testId = attempt.testId
        if (userId.isBlank() || testId.isBlank()) {
            return Result.failure(Exception("Missing user or test id for submission."))
        }
        val startedAt = attempt.startedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val resultId = resultDocId(userId, testId, startedAt)
        val resultRef = resultsCollection.document(resultId)
        val attemptRef = attemptsCollection.document(attemptDocId(userId, testId))
        val testRef = testsCollection.document(testId)
        val userRef = usersCollection.document(userId)

        return try {
            val outcome = firestore.runTransaction { tx ->
                val existing = tx.get(resultRef)
                if (existing.exists()) {
                    // Already submitted for this attempt — duplicate tap.
                    "duplicate|$resultId"
                } else {
                    val userSnap = tx.get(userRef)
                    if (!userSnap.exists()) {
                        throw IllegalStateException("User profile missing; submission cannot be recorded.")
                    }
                    val user = userSnap.toObject(User::class.java)
                        ?: throw IllegalStateException("User profile could not be parsed.")
                    val testSnap = tx.get(testRef)
                    if (!testSnap.exists()) {
                        throw IllegalStateException("Test no longer exists; submission aborted.")
                    }

                    val newXp = (user.xp + xpToAward).coerceAtLeast(0L)
                    val newPoints = (user.points + pointsToAward).coerceAtLeast(0L)
                    val newLevel = GamificationService.calculateLevel(newXp)
                    val attemptsCount = testSnap.getLong("attemptsCount") ?: 0L

                    tx.set(resultRef, result.copy(id = resultId, submissionKey = resultId))
                    tx.set(attemptRef, attempt.copy(id = attemptDocId(userId, testId), isSubmitted = true, submittedAt = System.currentTimeMillis()))
                    tx.update(testRef, "attemptsCount", attemptsCount + 1L)
                    tx.update(
                        userRef,
                        mapOf(
                            "xp" to newXp,
                            "points" to newPoints,
                            "level" to newLevel,
                            "testsCompleted" to (user.testsCompleted + 1).coerceAtLeast(0),
                            "lastActiveAt" to System.currentTimeMillis()
                        )
                    )
                    "accepted|$resultId"
                }
            }.await()

            val (kind, id) = outcome.split("|", limit = 2)
            Result.success(
                if (kind == "accepted") SubmissionOutcome.Accepted(id)
                else SubmissionOutcome.Duplicate(id)
            )
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Deletes a test plus its subcollections (questions) and per-user attempt
     * docs. Parent deletion does NOT remove subcollections in Firestore, so we
     * clean them explicitly to avoid orphan data.
     * Existing published results are retained (they are historical records).
     */
    suspend fun deleteTest(testId: String): Result<Unit> {
        return try {
            val questionsSubcoll = testsCollection.document(testId).collection(Constants.COLL_QUESTIONS)
            val questionsSnapshot = questionsSubcoll.get().await()
            if (!questionsSnapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in questionsSnapshot.documents) batch.delete(doc.reference)
                batch.commit().await()
            }

            // Remove in-progress attempts for this test.
            val attempts = attemptsCollection.whereEqualTo("testId", testId).get().await()
            if (!attempts.isEmpty) {
                val batch = firestore.batch()
                for (doc in attempts.documents) batch.delete(doc.reference)
                batch.commit().await()
            }

            testsCollection.document(testId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Counts the attempts for the leaderboard/insights (real count, no faking).
     */
    suspend fun countAttemptsForTest(testId: String): Result<Long> {
        return try {
            val snapshot = attemptsCollection
                .whereEqualTo("testId", testId)
                .get().await()
            Result.success(snapshot.documents.size.toLong())
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    companion object {
        const val ATTEMPTS_COLLECTION = Constants.COLL_TEST_ATTEMPTS
    }
}
