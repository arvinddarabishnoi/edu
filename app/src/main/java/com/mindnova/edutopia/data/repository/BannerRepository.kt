package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.BannerItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BannerRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val bannersCollection = firestore.collection(Constants.COLL_BANNERS)

    fun getActiveBannersFlow(): Flow<List<BannerItem>> = callbackFlow {
        val query = bannersCollection
            .whereEqualTo("active", true)
            .orderBy("priority", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(BannerItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun getAllBannersFlow(): Flow<List<BannerItem>> = callbackFlow {
        val listener = bannersCollection.orderBy("priority", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BannerItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveBanner(banner: BannerItem): Result<String> {
        return try {
            val docRef = if (banner.id.isBlank()) {
                bannersCollection.document()
            } else {
                bannersCollection.document(banner.id)
            }
            docRef.set(banner.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBanner(id: String): Result<Unit> {
        return try {
            bannersCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
