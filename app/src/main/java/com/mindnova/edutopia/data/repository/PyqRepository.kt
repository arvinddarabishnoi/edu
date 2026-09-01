package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.PyqQuestion
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PyqRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userRepo: UserRepository = UserRepository(firestore)
) {

    private val pyqCollection = firestore.collection(Constants.COLL_PYQS)

    fun getPyqsFlow(
        examType: String = "All",
        subject: String = "All",
        year: Int = 0
    ): Flow<List<PyqQuestion>> = callbackFlow {
        var query: Query = pyqCollection.orderBy("year", Query.Direction.DESCENDING)

        if (examType != "All" && examType.isNotBlank()) {
            query = query.whereEqualTo("examType", examType)
        }
        if (subject != "All" && subject.isNotBlank()) {
            query = query.whereEqualTo("subject", subject)
        }
        if (year > 0) {
            query = query.whereEqualTo("year", year)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(PyqQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun recordPyqSolved(userId: String, pyqId: String, isCorrect: Boolean): Result<Unit> {
        return try {
            pyqCollection.document(pyqId).update(
                "solvedCount", com.google.firebase.firestore.FieldValue.increment(1)
            ).await()

            if (isCorrect) {
                userRepo.awardXpAndPoints(
                    uid = userId,
                    xpToAdd = Constants.XP_PER_PYQ.toLong(),
                    pointsToAdd = 5L,
                    activityType = "PYQ"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePyq(pyq: PyqQuestion): Result<String> {
        return try {
            val docRef = if (pyq.id.isBlank()) pyqCollection.document() else pyqCollection.document(pyq.id)
            docRef.set(pyq.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePyq(id: String): Result<Unit> {
        return try {
            pyqCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
