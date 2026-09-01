package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.domain.services.GamificationService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val usersCollection = firestore.collection(Constants.COLL_USERS)

    fun getUserFlow(uid: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            val user = snapshot?.toObject(User::class.java)?.copy(uid = snapshot.id)
            trySend(user)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getUser(uid: String): Result<User?> {
        return try {
            val doc = usersCollection.document(uid).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(User::class.java)?.copy(uid = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLastActiveAndStreak(uid: String): Result<Unit> {
        return try {
            val doc = usersCollection.document(uid).get().await()
            if (doc.exists()) {
                val user = doc.toObject(User::class.java) ?: return Result.success(Unit)
                val newStreak = GamificationService.evaluateStreak(user.streak, user.lastActiveAt)
                usersCollection.document(uid).update(
                    mapOf(
                        "lastActiveAt" to System.currentTimeMillis(),
                        "streak" to newStreak
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically adds XP and Points and recalculates level.
     */
    suspend fun awardXpAndPoints(
        uid: String,
        xpToAdd: Long,
        pointsToAdd: Long,
        activityType: String
    ): Result<Unit> {
        return try {
            val userRef = usersCollection.document(uid)
            firestore.runTransaction { tx ->
                val snapshot = tx.get(userRef)
                if (snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        val newXp = (user.xp + xpToAdd).coerceAtLeast(0L)
                        val newPoints = (user.points + pointsToAdd).coerceAtLeast(0L)
                        val newLevel = GamificationService.calculateLevel(newXp)

                        var testsCompleted = user.testsCompleted
                        var lecturesWatched = user.lecturesWatched
                        var pyqsSolved = user.pyqsSolved

                        when (activityType) {
                            "TEST" -> testsCompleted++
                            "LECTURE" -> lecturesWatched++
                            "PYQ" -> pyqsSolved++
                        }

                        tx.update(
                            userRef,
                            mapOf(
                                "xp" to newXp,
                                "points" to newPoints,
                                "level" to newLevel,
                                "testsCompleted" to testsCompleted,
                                "lecturesWatched" to lecturesWatched,
                                "pyqsSolved" to pyqsSolved,
                                "lastActiveAt" to System.currentTimeMillis()
                            )
                        )
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLeaderboardFlow(limit: Long = 50): Flow<List<User>> = callbackFlow {
        val query = usersCollection
            .whereEqualTo("role", Constants.ROLE_STUDENT)
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(limit)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapIndexed { index, doc ->
                val u = doc.toObject(User::class.java) ?: User()
                u.copy(uid = doc.id, rank = (index + 1).toLong())
            } ?: emptyList()
            trySend(users)
        }
        awaitClose { listener.remove() }
    }
}
