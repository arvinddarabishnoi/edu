package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.AdminUser
import com.mindnova.edutopia.data.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AdminDashboardMetrics(
    val totalStudents: Long = 0L,
    val activeToday: Long = 0L,
    val totalTests: Long = 0L,
    val testsCompleted: Long = 0L,
    val totalLectures: Long = 0L,
    val totalBatches: Long = 0L,
    val totalAdmins: Long = 0L
)

class AdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val adminsCollection = firestore.collection(Constants.COLL_ADMINS)
    private val usersCollection = firestore.collection(Constants.COLL_USERS)
    private val testsCollection = firestore.collection(Constants.COLL_TESTS)
    private val lecturesCollection = firestore.collection(Constants.COLL_LECTURES)
    private val batchesCollection = firestore.collection(Constants.COLL_BATCHES)
    private val resultsCollection = firestore.collection(Constants.COLL_TEST_RESULTS)

    suspend fun getAdminProfile(uid: String): Result<AdminUser?> {
        return try {
            val doc = adminsCollection.document(uid).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(AdminUser::class.java)?.copy(uid = doc.id))
            } else {
                // Check if user is Super Admin in users collection
                val uDoc = usersCollection.document(uid).get().await()
                if (uDoc.exists()) {
                    val u = uDoc.toObject(User::class.java)
                    if (u?.role == Constants.ROLE_SUPER_ADMIN || u?.role == Constants.ROLE_ADMIN) {
                        return Result.success(
                            AdminUser(
                                uid = uid,
                                name = u.name,
                                email = u.email,
                                role = u.role,
                                permissions = listOf(
                                    "users", "banners", "announcements", "lectures", "series",
                                    "batches", "tests", "questions", "pyq", "tournaments",
                                    "dailyGoals", "messages", "analytics"
                                )
                            )
                        )
                    }
                }
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllAdminsFlow(): Flow<List<AdminUser>> = callbackFlow {
        val query = adminsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(AdminUser::class.java)?.copy(uid = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveAdmin(admin: AdminUser): Result<Unit> {
        return try {
            // Write to admins collection
            adminsCollection.document(admin.uid).set(admin).await()
            // Also update role in users collection
            usersCollection.document(admin.uid).update(
                mapOf(
                    "role" to admin.role,
                    "name" to admin.name,
                    "email" to admin.email
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAdmin(adminId: String): Result<Unit> {
        return try {
            adminsCollection.document(adminId).delete().await()
            usersCollection.document(adminId).update("role", Constants.ROLE_STUDENT).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllStudentsFlow(): Flow<List<User>> = callbackFlow {
        val query = usersCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getDashboardMetrics(): Result<AdminDashboardMetrics> {
        return try {
            val usersCount = usersCollection.get().await().size().toLong()
            val testsCount = testsCollection.get().await().size().toLong()
            val resultsCount = resultsCollection.get().await().size().toLong()
            val lecturesCount = lecturesCollection.get().await().size().toLong()
            val batchesCount = batchesCollection.get().await().size().toLong()
            val adminsCount = adminsCollection.get().await().size().toLong()

            // Active in last 24h
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            val activeCount = usersCollection.whereGreaterThan("lastActiveAt", oneDayAgo).get().await().size().toLong()

            Result.success(
                AdminDashboardMetrics(
                    totalStudents = usersCount,
                    activeToday = activeCount.coerceAtLeast(1L),
                    totalTests = testsCount,
                    testsCompleted = resultsCount,
                    totalLectures = lecturesCount,
                    totalBatches = batchesCount,
                    totalAdmins = (adminsCount + 1L).coerceAtLeast(1L)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
