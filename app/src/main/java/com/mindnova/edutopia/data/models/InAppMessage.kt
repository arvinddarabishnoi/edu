package com.mindnova.edutopia.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class InAppMessage(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetType: String = "all", // "all", "class", "batch", "user"
    val targetValue: String = "", // "Class 11", "Class 12", "Dropper", batchId, or userId
    val actionUrl: String = "",
    val senderName: String = "Admin MINDNOVA",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId
    val id: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val action: String = "", // "CREATE_TEST", "DELETE_TEST", "ADD_LECTURE", "CREATE_ADMIN", etc.
    val targetType: String = "", // "Test", "Lecture", "Banner", "Admin", "User", "DailyGoal"
    val targetId: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class GamificationConfig(
    val xpPerLecture: Long = 20L,
    val xpPerDailyGoal: Long = 50L,
    val xpPerTest: Long = 100L,
    val xpPerPyq: Long = 10L,
    val xpPerTournament: Long = 150L,
    val streakBonusXp: Long = 30L
)
