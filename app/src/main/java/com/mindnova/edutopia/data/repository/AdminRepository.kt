package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.docResourceFlow
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.AdminUser
import com.mindnova.edutopia.data.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar

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
    private val attemptsCollection = firestore.collection(Constants.COLL_TEST_ATTEMPTS)

    /**
     * Resolves the admin profile. Superseding rule: admins must have a doc in
     * the `admins` collection; superAdmins additionally exist in `users` with
     * role superAdmin. Firestore *errors* propagate (never treated as "not an admin").
     */
    suspend fun getAdminProfile(uid: String): Result<AdminUser?> {
        return try {
            val doc = adminsCollection.document(uid).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(AdminUser::class.java)?.copy(uid = doc.id))
            } else {
                // Fallback for a super admin whose admin doc may be missing.
                val uDoc = usersCollection.document(uid).get().await()
                val u = if (uDoc.exists()) uDoc.toObject(User::class.java) else null
                if (u?.role == Constants.ROLE_SUPER_ADMIN) {
                    Result.success(
                        AdminUser(
                            uid = uid,
                            name = u.name,
                            email = u.email,
                            role = u.role,
                            permissions = ALL_PERMISSIONS
                        )
                    )
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /** Reactive authorization check used before rendering any admin route. */
    fun isAuthorizedAdminFlow(uid: String): Flow<Resource<Boolean>> =
        adminsCollection.document(uid).docResourceFlow { doc ->
            doc.getString("status") != "inactive"
        }.map { resource ->
            when (resource) {
                // No admins doc -> not an admin (super-admin fallback handled by getAdminProfile at call site).
                is Resource.Empty -> Resource.Success(false)
                else -> resource
            }
        }

    fun getAllAdminsFlow(): Flow<Resource<List<AdminUser>>> =
        adminsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .listResourceFlow { doc ->
                doc.toObject(AdminUser::class.java)?.copy(uid = doc.id)
            }

    /**
     * Creates or updates an admin. Only superAdmins may change roles/permissions:
     * the client enforces UX, firestore.rules enforces the write itself.
     */
    suspend fun saveAdmin(admin: AdminUser, actorUid: String, actorIsSuperAdmin: Boolean): Result<Unit> {
        if (!actorIsSuperAdmin && admin.role == Constants.ROLE_SUPER_ADMIN) {
            return Result.failure(Exception("Only a super admin can create another super admin."))
        }
        return try {
            firestore.runTransaction { tx ->
                val adminRef = adminsCollection.document(admin.uid)
                tx.set(adminRef, admin.copy(uid = admin.uid, createdBy = admin.createdBy.ifBlank { actorUid }))
                val userRef = usersCollection.document(admin.uid)
                val existing = tx.get(userRef)
                if (existing.exists()) {
                    tx.update(
                        userRef,
                        mapOf(
                            "role" to admin.role,
                            "name" to admin.name,
                            "email" to admin.email
                        )
                    )
                } else {
                    tx.set(
                        userRef,
                        mapOf(
                            "uid" to admin.uid,
                            "name" to admin.name,
                            "email" to admin.email,
                            "role" to admin.role,
                            "studentClass" to "",
                            "createdAt" to System.currentTimeMillis(),
                            "lastActiveAt" to System.currentTimeMillis()
                        )
                    )
                }
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Revokes admin: removes the admins doc and demotes the users doc to student,
     * atomically. Super admin accounts cannot be removed by ordinary admins.
     */
    suspend fun removeAdmin(adminId: String, actorIsSuperAdmin: Boolean): Result<Unit> {
        return try {
            val existing = adminsCollection.document(adminId).get().await()
            val existingRole = existing.getString("role")
            if (existingRole == Constants.ROLE_SUPER_ADMIN && !actorIsSuperAdmin) {
                return Result.failure(Exception("Only a super admin can remove another super admin."))
            }
            firestore.runTransaction { tx ->
                tx.delete(adminsCollection.document(adminId))
                tx.update(usersCollection.document(adminId), "role", Constants.ROLE_STUDENT)
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    fun getAllStudentsFlow(): Flow<Resource<List<User>>> =
        usersCollection
            .whereEqualTo("role", Constants.ROLE_STUDENT)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .listResourceFlow { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }

    fun getAllUsersFlow(): Flow<Resource<List<User>>> =
        usersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .listResourceFlow { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }

    suspend fun setUserRole(uid: String, newRole: String, actorIsSuperAdmin: Boolean): Result<Unit> {
        if (!actorIsSuperAdmin) {
            return Result.failure(Exception("Only a super admin can change user roles."))
        }
        if (newRole !in listOf(Constants.ROLE_STUDENT, Constants.ROLE_ADMIN)) {
            return Result.failure(Exception("Invalid target role."))
        }
        return try {
            firestore.runTransaction { tx ->
                tx.update(usersCollection.document(uid), "role", newRole)
                if (newRole == Constants.ROLE_STUDENT) {
                    tx.delete(adminsCollection.document(uid))
                }
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    /**
     * Real dashboard metrics.
     *  - totalStudents counts role == "student" only
     *  - activeToday is the true count of users active since local midnight (can be 0)
     *  - totalAdmins counts documents in `admins` (can be 0)
     * No coercion, no "+1" hacks; failures propagate to the UI as errors.
     */
    suspend fun getDashboardMetrics(): Result<AdminDashboardMetrics> {
        return try {
            val studentsCount = usersCollection
                .whereEqualTo("role", Constants.ROLE_STUDENT)
                .count().get().await().count
            val testsCount = testsCollection.count().get().await().count
            val resultsCount = resultsCollection.count().get().await().count
            val attemptsCount = attemptsCollection
                .whereEqualTo("isSubmitted", true)
                .count().get().await().count
            val lecturesCount = lecturesCollection.count().get().await().count
            val batchesCount = batchesCollection.count().get().await().count
            val adminsCount = adminsCollection.count().get().await().count

            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val activeCount = usersCollection
                .whereGreaterThanOrEqualTo("lastActiveAt", startOfToday)
                .count().get().await().count

            Result.success(
                AdminDashboardMetrics(
                    totalStudents = studentsCount,
                    activeToday = activeCount,
                    totalTests = testsCount,
                    testsCompleted = resultsCount,
                    totalLectures = lecturesCount,
                    totalBatches = batchesCount,
                    totalAdmins = adminsCount
                )
            )
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    companion object {
        val ALL_PERMISSIONS = listOf(
            "users", "banners", "announcements", "lectures", "series",
            "batches", "tests", "questions", "pyq", "tournaments",
            "dailyGoals", "messages", "analytics"
        )
    }
}
