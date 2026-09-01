package com.mindnova.edutopia.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.mindnova.edutopia.core.utils.Constants

@IgnoreExtraProperties
data class User(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val studentClass: String = "", // e.g. "Class 11", "Class 12", "Dropper"
    val photoUrl: String = "",
    val role: String = Constants.ROLE_STUDENT, // "student", "admin", "superAdmin"
    val xp: Long = 0L,
    val points: Long = 0L,
    val level: Int = 1,
    val rank: Long = 0L,
    val streak: Int = 1,
    val testsCompleted: Int = 0,
    val lecturesWatched: Int = 0,
    val pyqsSolved: Int = 0,
    val batchId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class AdminUser(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = Constants.ROLE_ADMIN, // "admin" or "superAdmin"
    val permissions: List<String> = emptyList(), // "users", "banners", "announcements", "lectures", "series", "batches", "tests", "questions", "pyq", "tournaments", "dailyGoals", "messages", "analytics"
    val createdBy: String = "",
    val status: String = "active", // "active", "inactive"
    val createdAt: Long = System.currentTimeMillis()
)

enum class AdminPermission(val key: String, val label: String, val description: String) {
    USERS("users", "User Management", "View and filter student profiles and stats"),
    BANNERS("banners", "Banners", "Create and manage home screen announcement banners"),
    ANNOUNCEMENTS("announcements", "Announcements", "Publish notifications & updates"),
    LECTURES("lectures", "Lectures", "Add and organize video content"),
    SERIES("series", "Series & Chapters", "Organize courses and subject hierarchies"),
    BATCHES("batches", "Batches", "Create batches and manage enrollment links"),
    TESTS("tests", "Tests & AITS", "Create and configure tests, duration & marks"),
    QUESTIONS("questions", "Question Bank", "Add single MCQs & import bulk JSON questions"),
    PYQ("pyq", "PYQ Bank", "Manage Previous Year Questions"),
    TOURNAMENTS("tournaments", "Tournaments", "Schedule and manage live JEE tournaments"),
    DAILY_GOALS("dailyGoals", "Daily Goals", "Configure daily lectures and tests for students"),
    MESSAGES("messages", "In-App Messaging", "Send broadcast and targeted messages"),
    ANALYTICS("analytics", "Analytics Dashboard", "View student progress and test statistics")
}
