package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.Question
import com.mindnova.edutopia.data.models.TestAttempt
import com.mindnova.edutopia.data.models.TestModel
import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.domain.services.TestEvaluationService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userRepo: UserRepository = UserRepository(firestore)
) {

    private val testsCollection = firestore.collection(Constants.COLL_TESTS)
    private val attemptsCollection = firestore.collection(Constants.COLL_TEST_ATTEMPTS)
    private val resultsCollection = firestore.collection(Constants.COLL_TEST_RESULTS)

    fun getTestsFlow(category: String = "All"): Flow<List<TestModel>> = callbackFlow {
        var query: Query = testsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        if (category != "All" && category.isNotBlank()) {
            query = testsCollection.whereEqualTo("category", category)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(TestModel::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getTest(testId: String): Result<TestModel?> {
        return try {
            val doc = testsCollection.document(testId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(TestModel::class.java)?.copy(id = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuestionsForTest(testId: String): Result<List<Question>> {
        return try {
            val snapshot = testsCollection.document(testId)
                .collection(Constants.COLL_QUESTIONS)
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()

            val questions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Question::class.java)?.copy(id = doc.id, testId = testId)
            }
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitTest(
        userId: String,
        userName: String,
        test: TestModel,
        questions: List<Question>,
        answers: Map<String, String>,
        timeSpentSeconds: Long
    ): Result<TestResult> {
        return try {
            // 1. Evaluate Result
            val result = TestEvaluationService.evaluateTest(
                userId = userId,
                userName = userName,
                test = test,
                questions = questions,
                answers = answers,
                timeSpentSeconds = timeSpentSeconds
            )

            // 2. Save Attempt & Result in Firestore
            val attemptRef = attemptsCollection.document()
            val attempt = TestAttempt(
                id = attemptRef.id,
                userId = userId,
                testId = test.id,
                testTitle = test.title,
                answers = answers,
                timeSpentSeconds = timeSpentSeconds,
                isSubmitted = true,
                startedAt = System.currentTimeMillis() - (timeSpentSeconds * 1000),
                submittedAt = System.currentTimeMillis()
            )

            val resultRef = resultsCollection.document()
            val finalResult = result.copy(id = resultRef.id)

            val batch = firestore.batch()
            batch.set(attemptRef, attempt)
            batch.set(resultRef, finalResult)
            batch.commit().await()

            // 3. Atomically reward XP and points
            userRepo.awardXpAndPoints(
                uid = userId,
                xpToAdd = finalResult.xpEarned,
                pointsToAdd = finalResult.pointsEarned,
                activityType = "TEST"
            )

            // 4. Increment test attempts count
            testsCollection.document(test.id).update(
                "attemptsCount", com.google.firebase.firestore.FieldValue.increment(1)
            )

            Result.success(finalResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserTestResultsFlow(userId: String): Flow<List<TestResult>> = callbackFlow {
        val query = resultsCollection
            .whereEqualTo("userId", userId)
            .orderBy("submittedAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(TestResult::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveTest(test: TestModel): Result<String> {
        return try {
            val docRef = if (test.id.isBlank()) testsCollection.document() else testsCollection.document(test.id)
            docRef.set(test.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveQuestions(testId: String, questions: List<Question>): Result<Unit> {
        return try {
            val questionsSubcoll = testsCollection.document(testId).collection(Constants.COLL_QUESTIONS)
            val batch = firestore.batch()
            for ((index, q) in questions.withIndex()) {
                val qRef = if (q.id.isBlank()) questionsSubcoll.document() else questionsSubcoll.document(q.id)
                val prepared = q.copy(
                    id = qRef.id,
                    testId = testId,
                    order = index + 1,
                    questionNumber = index + 1
                )
                batch.set(qRef, prepared)
            }
            batch.commit().await()

            // Update total questions count on test document
            testsCollection.document(testId).update("totalQuestions", questions.size).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTest(testId: String): Result<Unit> {
        return try {
            testsCollection.document(testId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
