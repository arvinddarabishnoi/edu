package com.mindnova.edutopia.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class DailyGoal(
    @DocumentId
    val id: String = "",
    val dateKey: String = "", // e.g. "2026-09-01"
    val lecture: GoalLecture = GoalLecture(),
    val test: GoalTest = GoalTest(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class GoalLecture(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val chapter: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val duration: String = "45 mins"
)

@IgnoreExtraProperties
data class GoalTest(
    val id: String = "",
    val title: String = "",
    val totalQuestions: Int = 75,
    val durationMinutes: Int = 180,
    val scheduledStartTime: String = "7:00 PM",
    val status: String = "Available" // "Upcoming", "Available", "Live"
)
