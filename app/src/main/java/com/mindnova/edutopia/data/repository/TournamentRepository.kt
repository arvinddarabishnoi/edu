package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.Tournament
import com.mindnova.edutopia.data.models.TournamentParticipant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TournamentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val tournamentsCollection = firestore.collection(Constants.COLL_TOURNAMENTS)

    fun getTournamentsFlow(): Flow<List<Tournament>> = callbackFlow {
        val query = tournamentsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Tournament::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun getTournamentLeaderboardFlow(tournamentId: String): Flow<List<TournamentParticipant>> = callbackFlow {
        val query = tournamentsCollection.document(tournamentId)
            .collection("participants")
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapIndexed { index, doc ->
                val p = doc.toObject(TournamentParticipant::class.java) ?: TournamentParticipant()
                p.copy(id = doc.id, rank = index + 1)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveTournament(tournament: Tournament): Result<String> {
        return try {
            val docRef = if (tournament.id.isBlank()) tournamentsCollection.document() else tournamentsCollection.document(tournament.id)
            docRef.set(tournament.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
