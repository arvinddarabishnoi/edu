package com.mindnova.edutopia.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class TestModel(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val category: String = "JEE Main AITS", // "JEE Main AITS", "JEE Advanced AITS", "PYQ", "Tournament", "Custom Test"
    val description: String = "",
    val subject: String = "All", // "Physics", "Chemistry", "Mathematics", "All"
    val durationMinutes: Int = 180,
    val totalQuestions: Int = 75,
    val marksPerQuestion: Int = 4,
    val negativeMarks: Int = 1,
    val instructions: String = "Each question carries 4 marks. For every incorrect answer, 1 mark will be deducted. Unattempted questions carry 0 marks.",
    val status: String = "published", // "published", "draft", "upcoming", "closed"
    val startDate: Long = 0L,
    val endDate: Long = Long.MAX_VALUE,
    val attemptsCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Question(
    @DocumentId
    val id: String = "",
    val testId: String = "",
    val questionNumber: Int = 1,
    val questionText: String = "",
    val options: Map<String, String> = mapOf("A" to "", "B" to "", "C" to "", "D" to ""),
    val correctAnswer: String = "A", // "A", "B", "C", "D"
    val explanation: String = "",
    val subject: String = "Physics", // "Physics", "Chemistry", "Mathematics"
    val chapter: String = "",
    val topic: String = "",
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
    val questionType: String = "Single Correct MCQ",
    val marks: Int = 4,
    val negativeMarks: Int = 1,
    val order: Int = 1
)

@IgnoreExtraProperties
data class TestAttempt(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val answers: Map<String, String> = emptyMap(), // questionId -> "A"|"B"|"C"|"D"
    val markedForReview: List<String> = emptyList(), // list of questionIds
    val visitedQuestions: List<String> = emptyList(),
    val timeSpentSeconds: Long = 0L,
    val isSubmitted: Boolean = false,
    val startedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long = 0L
)

@IgnoreExtraProperties
data class TestResult(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val testCategory: String = "",
    val score: Int = 0,
    val maxScore: Int = 300,
    val accuracy: Double = 0.0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unattemptedCount: Int = 0,
    val totalQuestions: Int = 75,
    val timeSpentSeconds: Long = 0L,
    val percentile: Double = 0.0,
    val rank: Long = 1L,
    val xpEarned: Long = 100L,
    val pointsEarned: Long = 50L,
    val physicsScore: SubjectPerformance = SubjectPerformance("Physics"),
    val chemistryScore: SubjectPerformance = SubjectPerformance("Chemistry"),
    val mathsScore: SubjectPerformance = SubjectPerformance("Mathematics"),
    val weakTopicsIdentified: List<String> = emptyList(),
    val submittedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class SubjectPerformance(
    val subject: String = "",
    val score: Int = 0,
    val maxScore: Int = 100,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unattemptedCount: Int = 0,
    val accuracyPercentage: Double = 0.0
)
