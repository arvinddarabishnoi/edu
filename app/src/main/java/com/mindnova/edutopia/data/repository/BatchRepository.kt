package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.Batch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BatchRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val batchesCollection = firestore.collection(Constants.COLL_BATCHES)
    private val usersCollection = firestore.collection(Constants.COLL_USERS)

    fun getAllBatchesFlow(): Flow<List<Batch>> = callbackFlow {
        val query = batchesCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Batch::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
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
            Result.failure(e)
        }
    }

    suspend fun saveBatch(batch: Batch): Result<String> {
        return try {
            val docRef = if (batch.id.isBlank()) batchesCollection.document() else batchesCollection.document(batch.id)
            val generatedCode = if (batch.joinCode.isBlank()) "EDU-" + (1000..9999).random() else batch.joinCode
            docRef.set(batch.copy(id = docRef.id, joinCode = generatedCode)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enrollStudentInBatch(userId: String, batchId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("batchId", batchId).await()
            batchesCollection.document(batchId).update(
                "studentCount", com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
