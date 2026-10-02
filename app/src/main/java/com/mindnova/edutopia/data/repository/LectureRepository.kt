package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.docResourceFlow
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.core.utils.YouTubeUtils
import com.mindnova.edutopia.data.models.Lecture
import com.mindnova.edutopia.data.models.Series
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.domain.services.GamificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Outcome of a completion attempt, so the UI can distinguish first-time credit from no-op. */
sealed interface LectureCompletionOutcome {
    data object Awarded : LectureCompletionOutcome
    data object AlreadyCompleted : LectureCompletionOutcome
}

class LectureRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val seriesCollection = firestore.collection(Constants.COLL_SERIES)
    private val lecturesCollection = firestore.collection(Constants.COLL_LECTURES)
    private val usersCollection = firestore.collection(Constants.COLL_USERS)

    fun getSeriesFlow(): Flow<Resource<List<Series>>> =
        seriesCollection
            .orderBy("order", Query.Direction.ASCENDING)
            .listResourceFlow { doc ->
                doc.toObject(Series::class.java)?.copy(id = doc.id)
            }

    /**
     * Client-side filtered variant to avoid needing a subject+order composite index
     * for the "courses by subject" filter. Sorting stays server-side on `order`.
     */
    fun getSeriesBySubjectFlow(subject: String): Flow<Resource<List<Series>>> {
        val upstream = seriesCollection
            .orderBy("order", Query.Direction.ASCENDING)
            .listResourceFlow { doc ->
                doc.toObject(Series::class.java)?.copy(id = doc.id)
            }
        if (subject == "All" || subject.isBlank()) return upstream
        return upstream.map { resource ->
            when (resource) {
                is Resource.Success -> {
                    val filtered = resource.data.filter { it.subject == subject }
                    if (filtered.isEmpty()) Resource.Empty() else Resource.Success(filtered)
                }
                else -> resource
            }
        }
    }

    fun getLecturesForSeriesFlow(seriesId: String): Flow<Resource<List<Lecture>>> =
        lecturesCollection
            .whereEqualTo("seriesId", seriesId)
            .orderBy("lectureNumber", Query.Direction.ASCENDING)
            .listResourceFlow { doc ->
                doc.toObject(Lecture::class.java)?.copy(id = doc.id)
            }

    fun getAllLecturesFlow(): Flow<Resource<List<Lecture>>> =
        lecturesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .listResourceFlow { doc ->
                doc.toObject(Lecture::class.java)?.copy(id = doc.id)
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
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * All lecture ids the user has completed — a single collection read that
     * powers locked/completed states across a whole series at once.
     */
    fun getCompletedLectureIdsFlow(uid: String): Flow<Resource<Set<String>>> =
        usersCollection.document(uid)
            .collection("lectureProgress")
            .listResourceFlow { doc -> doc.id }
            .map { resource ->
                when (resource) {
                    is Resource.Success -> Resource.Success(resource.data.toSet())
                    is Resource.Empty -> Resource.Success(emptySet())
                    is Resource.Loading -> Resource.Loading
                    is Resource.Error -> Resource.Error(resource.message, resource.cause)
                }
            }

    /** One-shot variant used by flows that combine it with lecture lists. */
    suspend fun getCompletedLectureIds(uid: String): Result<Set<String>> {
        return try {
            val snapshot = usersCollection.document(uid)
                .collection("lectureProgress")
                .limit(500)
                .get().await()
            Result.success(snapshot.documents.map { it.id }.toSet())
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    // ------------------------------------------------------------------
    // Admin: series CRUD
    // ------------------------------------------------------------------

    suspend fun saveSeries(series: Series): Result<String> {
        return try {
            val docRef = if (series.id.isBlank()) {
                seriesCollection.document()
            } else {
                seriesCollection.document(series.id)
            }
            docRef.set(series.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun deleteSeries(seriesId: String): Result<Unit> {
        return try {
            // Remove all lectures belonging to the series, then the series itself.
            val lectures = lecturesCollection
                .whereEqualTo("seriesId", seriesId)
                .get().await()
            if (!lectures.isEmpty) {
                val batch = firestore.batch()
                for (doc in lectures.documents) batch.delete(doc.reference)
                batch.commit().await()
            }
            seriesCollection.document(seriesId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    // ------------------------------------------------------------------
    // Admin: lecture CRUD — lectureCount bookkeeping is exact
    // ------------------------------------------------------------------

    /**
     * Creates or edits a lecture.
     *  - Create: increments the target series lectureCount once (transaction).
     *  - Edit (same series): no counter changes.
     *  - Edit (series changed): decrement old series, increment new series.
     * Counts never go negative.
     */
    suspend fun saveLecture(lecture: Lecture): Result<String> {
        return try {
            val videoId = YouTubeUtils.extractVideoId(lecture.videoUrl)
            if (lecture.videoUrl.isNotBlank() && videoId.isBlank()) {
                return Result.failure(
                    Exception("That doesn't look like a valid YouTube URL or video ID.")
                )
            }
            val thumbnail = if (lecture.thumbnailUrl.isBlank()) {
                YouTubeUtils.getThumbnailUrl(videoId)
            } else {
                lecture.thumbnailUrl
            }
            val prepared = lecture.copy(videoId = videoId, thumbnailUrl = thumbnail)

            val isCreate = prepared.id.isBlank()
            val docRef = if (isCreate) lecturesCollection.document() else lecturesCollection.document(prepared.id)

            var oldSeriesId: String? = null
            if (!isCreate) {
                val existing = lecturesCollection.document(prepared.id).get().await()
                oldSeriesId = if (existing.exists()) {
                    existing.getString("seriesId")
                } else {
                    null
                }
            }

            docRef.set(prepared.copy(id = docRef.id)).await()

            val newSeriesId = prepared.seriesId
            val (incNew, decOld) = com.mindnova.edutopia.domain.services.Counters
                .seriesCountChanges(isCreate, oldSeriesId, newSeriesId)

            if (incNew || decOld) {
                firestore.runTransaction { tx ->
                    if (incNew) adjustCount(tx, newSeriesId, +1)
                    if (decOld) oldSeriesId?.let { adjustCount(tx, it, -1) }
                    Unit
                }.await()
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    private fun adjustCount(
        tx: com.google.firebase.firestore.Transaction,
        seriesId: String,
        delta: Int
    ) {
        if (seriesId.isBlank()) return
        val seriesRef = seriesCollection.document(seriesId)
        val snap = tx.get(seriesRef)
        if (snap.exists()) {
            val series = snap.toObject(Series::class.java)
            val current = series?.lectureCount ?: 0
            tx.update(seriesRef, "lectureCount", (current + delta).coerceAtLeast(0))
        }
    }

    /**
     * Deletes a lecture and decrements its series count exactly once.
     * Also removes the lecture's progress subcollection to avoid orphans.
     */
    suspend fun deleteLecture(id: String): Result<Unit> {
        return try {
            val lectureDoc = lecturesCollection.document(id).get().await()
            if (!lectureDoc.exists()) {
                return Result.failure(Exception("This lecture no longer exists."))
            }
            val seriesId = lectureDoc.getString("seriesId").orEmpty()

            // Clean progress subcollection
            val progress = lecturesCollection.document(id).collection("progress").get().await()
            if (!progress.isEmpty) {
                val batch = firestore.batch()
                for (doc in progress.documents) batch.delete(doc.reference)
                batch.commit().await()
            }

            lecturesCollection.document(id).delete().await()

            if (seriesId.isNotBlank()) {
                firestore.runTransaction { tx ->
                    adjustCount(tx, seriesId, -1)
                    Unit
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Marks a lecture as completed for a user, idempotently.
     * On first completion (inside one transaction):
     *  - creates lectures/{id}/progress/{uid}
     *  - increments the lecture's unique-view counter (`views`)
     *  - awards XP + lecturesWatched once
     * Repeat completions are no-ops — no XP farming, no view inflation.
     */
    suspend fun markLectureCompleted(userId: String, lectureId: String): Result<LectureCompletionOutcome> {
        if (userId.isBlank() || lectureId.isBlank()) {
            return Result.failure(Exception("Missing user or lecture id."))
        }
        return try {
            val lectureRef = lecturesCollection.document(lectureId)
            val progressRef = usersCollection.document(userId)
                .collection("lectureProgress")
                .document(lectureId)
            val userRef = usersCollection.document(userId)

            val outcome = firestore.runTransaction { tx ->
                val progressSnap = tx.get(progressRef)
                if (progressSnap.exists()) {
                    "already"
                } else {
                    val lectureSnap = tx.get(lectureRef)
                    if (!lectureSnap.exists()) {
                        throw IllegalStateException("This lecture no longer exists.")
                    }
                    val userSnap = tx.get(userRef)
                    if (!userSnap.exists()) {
                        throw IllegalStateException("User profile missing; cannot award XP.")
                    }
                    val user = userSnap.toObject(User::class.java)!!
                    val newViews = (lectureSnap.getLong("views") ?: 0L) + 1
                    val newXp = user.xp + Constants.XP_PER_LECTURE
                    val newLevel = GamificationService.calculateLevel(newXp)

                    tx.set(
                        progressRef,
                        mapOf(
                            "completed" to true,
                            "completedAt" to System.currentTimeMillis()
                        )
                    )
                    tx.update(lectureRef, "views", newViews)
                    tx.update(
                        userRef,
                        mapOf(
                            "xp" to newXp,
                            "level" to newLevel,
                            "lecturesWatched" to (user.lecturesWatched + 1).coerceAtLeast(0),
                            "lastActiveAt" to System.currentTimeMillis()
                        )
                    )
                    "awarded"
                }
            }.await()

            Result.success(
                if (outcome == "awarded") LectureCompletionOutcome.Awarded
                else LectureCompletionOutcome.AlreadyCompleted
            )
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }
}
