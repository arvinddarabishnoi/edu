package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.AuditLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class AuditLogRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val auditCollection = firestore.collection(Constants.COLL_AUDIT_LOGS)

    fun getAuditLogsFlow(limit: Long = 100): Flow<Resource<List<AuditLog>>> =
        auditCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .listResourceFlow { doc ->
                doc.toObject(AuditLog::class.java)?.copy(id = doc.id)
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
            Result.failure(wrapFirestoreError(e))
        }
    }
}
