package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.AuditLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuditLogRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val auditCollection = firestore.collection(Constants.COLL_AUDIT_LOGS)

    fun getAuditLogsFlow(limit: Long = 100): Flow<List<AuditLog>> = callbackFlow {
        val query = auditCollection.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(AuditLog::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun logAction(
        adminId: String,
        adminEmail: String,
        action: String,
        targetType: String,
        targetId: String,
        details: String
    ): Result<Unit> {
        return try {
            val docRef = auditCollection.document()
            val entry = AuditLog(
                id = docRef.id,
                adminId = adminId,
                adminEmail = adminEmail,
                action = action,
                targetType = targetType,
                targetId = targetId,
                details = details,
                timestamp = System.currentTimeMillis()
            )
            docRef.set(entry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
