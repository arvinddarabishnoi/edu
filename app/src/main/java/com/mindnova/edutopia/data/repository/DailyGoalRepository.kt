package com.mindnova.edutopia.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.DailyGoal
import com.mindnova.edutopia.data.models.GoalLecture
import com.mindnova.edutopia.data.models.GoalTest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DailyGoalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val dailyGoalsCollection = firestore.collection(Constants.COLL_DAILY_GOALS)

    fun getTodayGoalFlow(): Flow<DailyGoal?> = callbackFlow {
        val query = dailyGoalsCollection
            .whereEqualTo("isActive", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(fallbackGoal())
                return@addSnapshotListener
            }
            val goal = snapshot?.documents?.firstOrNull()?.let { doc ->
                doc.toObject(DailyGoal::class.java)?.copy(id = doc.id)
            } ?: fallbackGoal()
            trySend(goal)
        }
        awaitClose { listener.remove() }
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
            Result.failure(e)
        }
    }

    private fun fallbackGoal(): DailyGoal {
        return DailyGoal(
            id = "fallback",
            dateKey = "today",
            lecture = GoalLecture(
                id = "default_lec",
                title = "Rotational Motion — Lecture 12",
                subject = "Physics",
                chapter = "Rotational Motion",
                description = "Moment of Inertia & Parallel Axis Theorem with JEE Advanced Problem Solving",
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                duration = "52 mins"
            ),
            test = GoalTest(
                id = "default_test",
                title = "JEE Main Full Syllabus AITS — Test 08",
                totalQuestions = 75,
                durationMinutes = 180,
                scheduledStartTime = "7:00 PM",
                status = "Available"
            ),
            isActive = true
        )
    }
}
