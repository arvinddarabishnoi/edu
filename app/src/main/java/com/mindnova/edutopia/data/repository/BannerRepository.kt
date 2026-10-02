package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.BannerItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BannerRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val bannersCollection = firestore.collection(Constants.COLL_BANNERS)

    /** Active banners in the current date window (date filtering is client-side). */
    fun getActiveBannersFlow(): Flow<Resource<List<BannerItem>>> =
        bannersCollection
            .whereEqualTo("active", true)
            .orderBy("priority", Query.Direction.DESCENDING)
            .listResourceFlow { doc ->
                doc.toObject(BannerItem::class.java)?.copy(id = doc.id)
            }
            .map { resource ->
                if (resource is Resource.Success) {
                    val now = System.currentTimeMillis()
                    val visible = resource.data.filter { it.startDate <= now && it.endDate >= now }
                    if (visible.isEmpty()) Resource.Empty() else Resource.Success(visible)
                } else resource
            }

    fun getAllBannersFlow(): Flow<Resource<List<BannerItem>>> =
        bannersCollection
            .orderBy("priority", Query.Direction.DESCENDING)
            .listResourceFlow { doc ->
                doc.toObject(BannerItem::class.java)?.copy(id = doc.id)
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
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun deleteBanner(id: String): Result<Unit> {
        return try {
            bannersCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }
}
