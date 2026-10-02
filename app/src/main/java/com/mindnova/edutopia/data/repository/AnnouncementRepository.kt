package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.Announcement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class AnnouncementRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val announcementsCollection = firestore.collection(Constants.COLL_ANNOUNCEMENTS)

    fun getAnnouncementsFlow(): Flow<Resource<List<Announcement>>> =
        announcementsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .listResourceFlow { doc ->
                doc.toObject(Announcement::class.java)?.copy(id = doc.id)
            }

    suspend fun createAnnouncement(announcement: Announcement): Result<String> {
        return try {
            val docRef = if (announcement.id.isBlank()) {
                announcementsCollection.document()
            } else {
                announcementsCollection.document(announcement.id)
            }
            docRef.set(announcement.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun deleteAnnouncement(id: String): Result<Unit> {
        return try {
            announcementsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }
}
