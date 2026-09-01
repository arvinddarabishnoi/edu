package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.InAppMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val messagesCollection = firestore.collection(Constants.COLL_MESSAGES)

    fun getMessagesForStudentFlow(
        studentClass: String = "",
        batchId: String = "",
        userId: String = ""
    ): Flow<List<InAppMessage>> = callbackFlow {
        val query = messagesCollection.orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(InAppMessage::class.java)?.copy(id = doc.id)
            }?.filter { msg ->
                msg.targetType == "all" ||
                        (msg.targetType == "class" && msg.targetValue.equals(studentClass, ignoreCase = true)) ||
                        (msg.targetType == "batch" && msg.targetValue == batchId) ||
                        (msg.targetType == "user" && msg.targetValue == userId)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(message: InAppMessage): Result<String> {
        return try {
            val docRef = if (message.id.isBlank()) messagesCollection.document() else messagesCollection.document(message.id)
            docRef.set(message.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
