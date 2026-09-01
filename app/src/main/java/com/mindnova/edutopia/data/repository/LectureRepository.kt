package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.YouTubeUtils
import com.mindnova.edutopia.data.models.Lecture
import com.mindnova.edutopia.data.models.Series
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LectureRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val seriesCollection = firestore.collection(Constants.COLL_SERIES)
    private val lecturesCollection = firestore.collection(Constants.COLL_LECTURES)

    fun getSeriesFlow(subject: String = "All"): Flow<List<Series>> = callbackFlow {
        var query: Query = seriesCollection.orderBy("order", Query.Direction.ASCENDING)
        if (subject != "All" && subject.isNotBlank()) {
            query = seriesCollection.whereEqualTo("subject", subject)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Series::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun getLecturesForSeriesFlow(seriesId: String): Flow<List<Lecture>> = callbackFlow {
        val query = lecturesCollection
            .whereEqualTo("seriesId", seriesId)
            .orderBy("lectureNumber", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Lecture::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun getAllLecturesFlow(): Flow<List<Lecture>> = callbackFlow {
        val listener = lecturesCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Lecture::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getLecture(id: String): Result<Lecture?> {
        return try {
            val doc = lecturesCollection.document(id).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(Lecture::class.java)?.copy(id = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSeries(series: Series): Result<String> {
        return try {
            val docRef = if (series.id.isBlank()) seriesCollection.document() else seriesCollection.document(series.id)
            docRef.set(series.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveLecture(lecture: Lecture): Result<String> {
        return try {
            val videoId = YouTubeUtils.extractVideoId(lecture.videoUrl)
            val thumbnail = if (lecture.thumbnailUrl.isBlank()) YouTubeUtils.getThumbnailUrl(videoId) else lecture.thumbnailUrl

            val prepared = lecture.copy(
                videoId = videoId,
                thumbnailUrl = thumbnail
            )

            val docRef = if (prepared.id.isBlank()) lecturesCollection.document() else lecturesCollection.document(prepared.id)
            docRef.set(prepared.copy(id = docRef.id)).await()

            // Increment series lecture count if seriesId is present
            if (prepared.seriesId.isNotBlank()) {
                firestore.runTransaction { tx ->
                    val seriesRef = seriesCollection.document(prepared.seriesId)
                    val sSnap = tx.get(seriesRef)
                    if (sSnap.exists()) {
                        val s = sSnap.toObject(Series::class.java)
                        if (s != null) {
                            tx.update(seriesRef, "lectureCount", (s.lectureCount + 1))
                        }
                    }
                }
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLecture(id: String): Result<Unit> {
        return try {
            lecturesCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
