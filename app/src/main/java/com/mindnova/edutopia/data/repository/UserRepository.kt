package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.docResourceFlow
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.domain.services.GamificationService
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Distinguishes why a profile check failed — this is critical after a
 * successful login so a Firestore outage is never mistaken for
 * "profile missing".
 */
sealed interface ProfileLookup {
    data object Missing : ProfileLookup
    data class Incomplete(val user: User) : ProfileLookup
    data class Complete(val user: User) : ProfileLookup
    data class Failed(val message: String, val cause: Throwable? = null) : ProfileLookup
}

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val usersCollection = firestore.collection(Constants.COLL_USERS)

    fun getUserFlow(uid: String): Flow<Resource<User>> =
        usersCollection.document(uid).docResourceFlow { doc ->
            doc.toObject(User::class.java)?.copy(uid = doc.id)
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
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Classifies the profile state after auth success, differentiating:
     * profile missing, profile incomplete, and lookup failed (network/rules).
     */
    suspend fun lookupProfile(uid: String): ProfileLookup {
        return when (val result = getUser(uid)) {
            is Result.Success -> {
                val user = result.getOrNull()
                when {
                    user == null -> ProfileLookup.Missing
                    user.name.isBlank() || user.studentClass.isBlank() ->
                        ProfileLookup.Incomplete(user)
                    else -> ProfileLookup.Complete(user)
                }
            }
            is Result.Failure -> ProfileLookup.Failed(
                result.exceptionOrNull()?.message ?: "Failed to load your profile.",
                result.exceptionOrNull()
            )
            else -> ProfileLookup.Failed("Unexpected profile lookup state.")
        }
    }

    suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
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
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Atomically adds XP and points and recalculates level.
     * Fails loudly if the transaction fails — callers must not ignore the result.
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
                if (!snapshot.exists()) {
                    throw IllegalStateException("User profile $uid does not exist; cannot award XP.")
                }
                val user = snapshot.toObject(User::class.java)
                    ?: throw IllegalStateException("User profile $uid could not be parsed.")

                val newXp = (user.xp + xpToAdd).coerceAtLeast(0L)
                val newPoints = (user.points + pointsToAdd).coerceAtLeast(0L)
                val newLevel = GamificationService.calculateLevel(newXp)

                var testsCompleted = user.testsCompleted
                var lecturesWatched = user.lecturesWatched
                var pyqsSolved = user.pyqsSolved

                when (activityType) {
                    "TEST" -> testsCompleted = (testsCompleted + 1).coerceAtLeast(0)
                    "LECTURE" -> lecturesWatched = (lecturesWatched + 1).coerceAtLeast(0)
                    "PYQ" -> pyqsSolved = (pyqsSolved + 1).coerceAtLeast(0)
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
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Leaderboard by points. Requires composite index (role asc, points desc)
     * — see firestore.indexes.json. Missing indexes surface as Error, not Empty.
     */
    fun getLeaderboardFlow(limit: Long = 50): Flow<Resource<List<User>>> =
        usersCollection
            .whereEqualTo("role", Constants.ROLE_STUDENT)
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(limit)
            .listResourceFlow { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }
}
