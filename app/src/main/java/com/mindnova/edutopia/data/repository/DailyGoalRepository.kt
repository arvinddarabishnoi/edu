package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.listResourceFlow
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import com.mindnova.edutopia.data.models.DailyGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class DailyGoalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val dailyGoalsCollection = firestore.collection(Constants.COLL_DAILY_GOALS)

    /**
     * Streams today's goal, falling back to the most recent active goal.
     * An empty database yields [Resource.Empty] (the UI shows "no goal set"),
     * never fabricated placeholder content.
     */
    fun getTodayGoalFlow(): Flow<Resource<DailyGoal>> {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
        return dailyGoalsCollection
            .orderBy("dateKey", Query.Direction.DESCENDING)
            .limit(10)
            .listResourceFlow { doc ->
                doc.toObject(DailyGoal::class.java)?.copy(id = doc.id)
            }
            .map { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val todays = resource.data.firstOrNull { it.dateKey == todayKey && it.isActive }
                            ?: resource.data.firstOrNull { it.isActive }
                        if (todays == null) Resource.Empty() else Resource.Success(todays)
                    }
                    is Resource.Empty -> Resource.Empty()
                    else -> resource
                }
            }
    }

    /** All goals, newest first (admin management). */
    fun getAllGoalsFlow(limit: Long = 50): Flow<Resource<List<DailyGoal>>> =
        dailyGoalsCollection
            .orderBy("dateKey", Query.Direction.DESCENDING)
            .limit(limit)
            .listResourceFlow { doc ->
                doc.toObject(DailyGoal::class.java)?.copy(id = doc.id)
            }

    suspend fun getGoalById(id: String): Result<DailyGoal?> {
        return try {
            val doc = dailyGoalsCollection.document(id).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(DailyGoal::class.java)?.copy(id = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun saveDailyGoal(goal: DailyGoal): Result<String> {
        return try {
            val docRef = if (goal.id.isBlank()) {
                dailyGoalsCollection.document()
            } else {
                dailyGoalsCollection.document(goal.id)
            }
            docRef.set(goal.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }

    suspend fun deleteDailyGoal(id: String): Result<Unit> {
        return try {
            dailyGoalsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(wrapFirestoreError(e))
        }
    }
}
