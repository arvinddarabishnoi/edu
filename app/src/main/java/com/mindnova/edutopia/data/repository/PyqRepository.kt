package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.PyqAttemptor
import com.mindnova.edutopia.data.models.PyqQuestion
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.domain.services.GamificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Result of recording a PYQ answer, exposed so the UI can state whether XP was awarded. */
sealed interface PyqSubmitOutcome {
    data class Recorded(val firstCorrect: Boolean, val xpAwarded: Long) : PyqSubmitOutcome
    data object RepeatAttempt : PyqSubmitOutcome
}

class PyqRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val pyqCollection = firestore.collection(Constants.COLL_PYQS)
    private val usersCollection = firestore.collection(Constants.COLL_USERS)

    fun getPyqsFlow(examType: String = "All"): Flow<Resource<List<PyqQuestion>>> {
        val base = if (examType == "All" || examType.isBlank()) {
            pyqCollection
        } else {
            pyqCollection.whereEqualTo("examType", examType)
        }
        // Year/subject/chapter filters are applied client-side in the ViewModel to
        // avoid demanding a large matrix of composite indexes for optional filters.
        return base
            .orderBy("year", Query.Direction.DESCENDING)
            .limit(200)
            .listResourceFlow { doc ->
                doc.toObject(PyqQuestion::class.java)?.copy(id = doc.id)
            }
    }

    /** Full list for admin management. */
    fun getAllPyqsFlow(limit: Long = 200): Flow<Resource<List<PyqQuestion>>> =
        pyqCollection
            .orderBy("year", Query.Direction.DESCENDING)
            .limit(limit)
            .listResourceFlow { doc ->
                doc.toObject(PyqQuestion::class.java)?.copy(id = doc.id)
            }

    suspend fun getPyq(id: String): Result<PyqQuestion?> {
        return try {
            val doc = pyqCollection.document(id).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(PyqQuestion::class.java)?.copy(id = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Idempotent per-user PYQ attempt recording, all inside ONE transaction:
     *
     *  - pyqs/{pyqId}/attemptors/{uid} holds the user's attempt history
     *  - solvedCount (unique students) increments only on the user's FIRST attempt
     *  - totalAttempts/totalCorrect/accuracy update per attempt (real aggregates)
     *  - XP + pyqsSolved award exactly once, on the user's FIRST correct answer
     *  - marks best status persistently
     *
     * Repeating the same question therefore cannot farm XP or inflate counts.
     */
    suspend fun recordPyqAttempt(
        userId: String,
        pyqId: String,
        isCorrect: Boolean
    ): Result<PyqSubmitOutcome> {
        if (userId.isBlank() || pyqId.isBlank()) {
            return Result.failure(Exception("Missing user or question id."))
        }
        return try {
            val pyqRef = pyqCollection.document(pyqId)
            val attemptorRef = pyqRef.collection("attemptors").document(userId)
            val userRef = usersCollection.document(userId)

            val outcome = firestore.runTransaction { tx ->
                val pyqSnap = tx.get(pyqRef)
                if (!pyqSnap.exists()) {
                    throw IllegalStateException("This PYQ no longer exists.")
                }
                val attemptorSnap = tx.get(attemptorRef)
                val attemptor = if (attemptorSnap.exists()) {
                    attemptorSnap.toObject(PyqAttemptor::class.java)
                } else {
                    PyqAttemptor(uid = userId)
                }
                val isFirstAttempt = attemptor == null || attemptor.attempts == 0L
                val wasAlreadyCorrect = attemptor?.let { it.correctAttempts > 0 } ?: false

                val newAttempts = ((attemptor?.attempts ?: 0L) + 1L).coerceAtLeast(0L)
                val newCorrect = ((attemptor?.correctAttempts ?: 0L) + if (isCorrect) 1L else 0L)
                    .coerceAtLeast(0L)

                tx.set(
                    attemptorRef,
                    PyqAttemptor(
                        uid = userId,
                        attempts = newAttempts,
                        correctAttempts = newCorrect,
                        firstSolvedAt = if (isCorrect && (attemptor?.firstSolvedAt ?: 0L) == 0L)
                            System.currentTimeMillis() else attemptor?.firstSolvedAt ?: 0L,
                        lastAttemptAt = System.currentTimeMillis(),
                        xpAwarded = wasAlreadyCorrect
                    )
                )

                val totalAttempts = ((pyqSnap.getLong("totalAttempts") ?: 0L) + 1L).coerceAtLeast(0L)
                val totalCorrect = ((pyqSnap.getLong("totalCorrect") ?: 0L) + if (isCorrect) 1L else 0L)
                    .coerceAtLeast(0L)
                val solvedCount = (pyqSnap.getLong("solvedCount") ?: 0L) + if (isFirstAttempt) 1L else 0L
                val accuracy = if (totalAttempts > 0) {
                    Math.round((totalCorrect.toDouble() / totalAttempts.toDouble()) * 1000.0) / 10.0
                } else {
                    0.0
                }

                tx.update(
                    pyqRef,
                    mapOf(
                        "totalAttempts" to totalAttempts,
                        "totalCorrect" to totalCorrect,
                        "solvedCount" to solvedCount.coerceAtLeast(0L),
                        "accuracy" to accuracy
                    )
                )

                // XP only ever on the first-ever correct answer for this user+question.
                if (isCorrect && !wasAlreadyCorrect) {
                    val userSnap = tx.get(userRef)
                    val user = userSnap.toObject(User::class.java)
                    if (!userSnap.exists() || user == null) {
                        throw IllegalStateException("User profile missing; cannot award XP.")
                    }
                    val newXp = (user.xp + Constants.XP_PER_PYQ).coerceAtLeast(0L)
                    tx.update(
                        userRef,
                        mapOf(
                            "xp" to newXp,
                            "level" to GamificationService.calculateLevel(newXp),
                            "points" to (user.points + 5L).coerceAtLeast(0L),
                            "pyqsSolved" to (user.pyqsSolved + 1).coerceAtLeast(0),
                            "lastActiveAt" to System.currentTimeMillis()
                        )
                    )
                    "awarded"
                } else {
                    "repeat"
                }
            }.await()

            Result.success(
                if (outcome == "awarded") {
                    PyqSubmitOutcome.Recorded(firstCorrect = true, xpAwarded = Constants.XP_PER_PYQ.toLong())
                } else {
                    PyqSubmitOutcome.RepeatAttempt
                }
            )
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /** Admin CRUD */
    suspend fun savePyq(pyq: PyqQuestion): Result<String> {
        return try {
            val docRef = if (pyq.id.isBlank()) pyqCollection.document() else pyqCollection.document(pyq.id)
            docRef.set(pyq.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Deletes a PYQ and its attemptor subcollection so no orphan data remains.
     */
    suspend fun deletePyq(id: String): Result<Unit> {
        return try {
            val attemptors = pyqCollection.document(id).collection("attemptors").get().await()
            if (!attemptors.isEmpty) {
                val batch = firestore.batch()
                for (doc in attemptors.documents) batch.delete(doc.reference)
                batch.commit().await()
            }
            pyqCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }
}
