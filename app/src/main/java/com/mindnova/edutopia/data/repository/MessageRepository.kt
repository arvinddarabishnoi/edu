package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.InAppMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MessageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val messagesCollection = firestore.collection(Constants.COLL_MESSAGES)

    /**
     * Targeting (class/batch/user) is filtered client-side so no per-target
     * composite index is required. Errors propagate; they are not swallowed.
     */
    fun getMessagesForStudentFlow(
        studentClass: String = "",
        batchId: String = "",
        userId: String = ""
    ): Flow<Resource<List<InAppMessage>>> =
        messagesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .listResourceFlow { doc ->
                doc.toObject(InAppMessage::class.java)?.copy(id = doc.id)
            }
            .map { resource ->
                if (resource is Resource.Success) {
                    val visible = resource.data.filter { msg ->
                        msg.targetType == "all" ||
                            (msg.targetType == "class" &&
                                msg.targetValue.equals(studentClass, ignoreCase = true)) ||
                            (msg.targetType == "batch" && batchId.isNotBlank() && msg.targetValue == batchId) ||
                            (msg.targetType == "user" && msg.targetValue == userId)
                    }
                    if (visible.isEmpty()) Resource.Empty() else Resource.Success(visible)
                } else resource
            }

    fun getAllMessagesFlow(): Flow<Resource<List<InAppMessage>>> =
        messagesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .listResourceFlow { doc ->
                doc.toObject(InAppMessage::class.java)?.copy(id = doc.id)
            }

    suspend fun sendMessage(message: InAppMessage): Result<String> {
        return try {
            val docRef = if (message.id.isBlank()) {
                messagesCollection.document()
            } else {
                messagesCollection.document(message.id)
            }
            docRef.set(message.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun deleteMessage(id: String): Result<Unit> {
        return try {
            messagesCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }
}
