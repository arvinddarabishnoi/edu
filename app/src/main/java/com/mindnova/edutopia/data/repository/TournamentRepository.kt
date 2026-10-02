package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.Tournament
import com.mindnova.edutopia.data.models.TournamentParticipant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class TournamentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val tournamentsCollection = firestore.collection(Constants.COLL_TOURNAMENTS)

    fun getTournamentsFlow(): Flow<Resource<List<Tournament>>> =
        tournamentsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .listResourceFlow { doc ->
                doc.toObject(Tournament::class.java)?.copy(id = doc.id)
            }

    suspend fun getTournament(id: String): Result<Tournament?> {
        return try {
            val doc = tournamentsCollection.document(id).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(Tournament::class.java)?.copy(id = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    fun participantsCollection(tournamentId: String) =
        tournamentsCollection.document(tournamentId).collection("participants")

    fun getTournamentLeaderboardFlow(tournamentId: String): Flow<Resource<List<TournamentParticipant>>> =
        participantsCollection(tournamentId)
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(50)
            .listResourceFlow { doc ->
                doc.toObject(TournamentParticipant::class.java)?.copy(id = doc.id)
            }

    /**
     * Register participation idempotently: participant doc is keyed by userId,
     * participantCount increments only on the first-ever join for this user.
     */
    suspend fun registerParticipant(
        tournamentId: String,
        userId: String,
        userName: String,
        userClass: String
    ): Result<Unit> {
        return try {
            val participantRef = participantsCollection(tournamentId).document(userId)
            val tournamentRef = tournamentsCollection.document(tournamentId)
            firestore.runTransaction { tx ->
                val existing = tx.get(participantRef)
                if (!existing.exists()) {
                    tx.set(
                        participantRef,
                        mapOf(
                            "tournamentId" to tournamentId,
                            "userId" to userId,
                            "userName" to userName,
                            "userClass" to userClass,
                            "score" to 0,
                            "rank" to 0,
                            "timeTakenSeconds" to 0L,
                            "submittedAt" to 0L,
                            "joinedAt" to System.currentTimeMillis()
                        )
                    )
                    val snap = tx.get(tournamentRef)
                    if (!snap.exists()) {
                        throw IllegalStateException("This tournament no longer exists.")
                    }
                    val count = snap.getLong("participantCount") ?: 0L
                    tx.update(tournamentRef, "participantCount", (count + 1L).coerceAtLeast(0L))
                }
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun saveTournament(tournament: Tournament): Result<String> {
        return try {
            val docRef = if (tournament.id.isBlank()) {
                tournamentsCollection.document()
            } else {
                tournamentsCollection.document(tournament.id)
            }
            docRef.set(tournament.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /** Deletes a tournament together with its participants subcollection. */
    suspend fun deleteTournament(id: String): Result<Unit> {
        return try {
            val participants = participantsCollection(id).get().await()
            if (!participants.isEmpty) {
                val batch = firestore.batch()
                for (doc in participants.documents) batch.delete(doc.reference)
                batch.commit().await()
            }
            tournamentsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }
}
