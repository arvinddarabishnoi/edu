package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.Batch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class BatchRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val batchesCollection = firestore.collection(Constants.COLL_BATCHES)
    private val usersCollection = firestore.collection(Constants.COLL_USERS)

    fun getAllBatchesFlow(): Flow<Resource<List<Batch>>> =
        batchesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .listResourceFlow { doc ->
                doc.toObject(Batch::class.java)?.copy(id = doc.id)
            }

    suspend fun getBatchById(batchId: String): Result<Batch?> {
        return try {
            val doc = batchesCollection.document(batchId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(Batch::class.java)?.copy(id = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun saveBatch(batch: Batch): Result<String> {
        return try {
            val docRef = if (batch.id.isBlank()) {
                batchesCollection.document()
            } else {
                batchesCollection.document(batch.id)
            }
            val generatedCode = if (batch.joinCode.isBlank()) {
                "EDU-" + (1000..9999).random()
            } else {
                batch.joinCode
            }
            docRef.set(batch.copy(id = docRef.id, joinCode = generatedCode)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun deleteBatch(batchId: String): Result<Unit> {
        return try {
            // Un-enroll students first so nobody points at a dangling batch.
            val affected = usersCollection
                .whereEqualTo("batchId", batchId)
                .get().await()
            if (!affected.isEmpty) {
                val batchWrite = firestore.batch()
                for (doc in affected.documents) {
                    batchWrite.update(doc.reference, "batchId", "")
                }
                batchWrite.commit().await()
            }
            batchesCollection.document(batchId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Idempotent enrollment inside a Firestore transaction:
     *  - already in the same batch   -> success, no counter changes
     *  - moving to a different batch -> decrement old batch, increment new one
     *  - fresh enrollment            -> increment new batch
     *  - missing target batch        -> explicit failure
     *  - counts are clamped at zero
     */
    suspend fun enrollStudentInBatch(userId: String, batchId: String): Result<Unit> {
        if (batchId.isBlank()) return Result.failure(Exception("Please choose a batch to join."))
        return try {
            val userRef = usersCollection.document(userId)
            val batchRef = batchesCollection.document(batchId)

            firestore.runTransaction { tx ->
                val batchSnap = tx.get(batchRef)
                if (!batchSnap.exists()) {
                    throw IllegalStateException("Batch '$batchId' no longer exists.")
                }
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) {
                    throw IllegalStateException("User profile '$userId' no longer exists.")
                }
                val user = userSnap.toObject(com.mindnova.edutopia.data.models.User::class.java)
                val oldBatchId = user?.batchId.orEmpty()

                when (com.mindnova.edutopia.domain.services.Counters
                    .enrollAction(oldBatchId, batchId)
                ) {
                    com.mindnova.edutopia.domain.services.Counters.EnrollmentAction.NoOp -> "already"
                    else -> {
                        // Decrement old batch safely (MoveFromOld only).
                        if (oldBatchId.isNotBlank()) {
                            val oldRef = batchesCollection.document(oldBatchId)
                            val oldSnap = tx.get(oldRef)
                            if (oldSnap.exists()) {
                                val oldCount = oldSnap.getLong("studentCount") ?: 0L
                                tx.update(
                                    oldRef, "studentCount",
                                    com.mindnova.edutopia.domain.services.Counters
                                        .applyDelta(oldCount, -1)
                                )
                            }
                        }
                        // Increment new batch safely.
                        val newCount = batchSnap.getLong("studentCount") ?: 0L
                        tx.update(
                            batchRef, "studentCount",
                            com.mindnova.edutopia.domain.services.Counters
                                .applyDelta(newCount, +1)
                        )
                        tx.update(userRef, "batchId", batchId)
                        "moved"
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /** Look up a batch by its join code (used by the student "join batch" flow). */
    suspend fun findBatchByJoinCode(code: String): Result<Batch?> {
        return try {
            val snapshot = batchesCollection
                .whereEqualTo("joinCode", code.trim().uppercase())
                .limit(1)
                .get().await()
            val batch = snapshot.documents.firstOrNull()?.let {
                it.toObject(Batch::class.java)?.copy(id = it.id)
            }
            Result.success(batch)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }
}
