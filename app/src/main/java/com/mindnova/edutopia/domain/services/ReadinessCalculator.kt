package com.mindnova.edutopia.domain.services

import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.data.models.User
import kotlin.math.roundToInt

data class JeeReadiness(
    val readinessLevel: Int,
    val readinessPercentage: Int,
    val strongSubject: String,
    val strongSubjectAccuracy: Int,
    val needsImprovementSubject: String,
    val needsImprovementAccuracy: Int,
    val weakTopics: List<String>,
    val testCount: Int,
    val totalLecturesWatched: Int,
    val totalPyqsSolved: Int
)

object ReadinessCalculator {

    /**
     * Calculates JEE Readiness level and percentage score based on test history,
     * PYQ practice, lecture engagement, and consistency.
     */
    fun calculateReadiness(
        user: User,
        testResults: List<TestResult>,
        defaultWeakTopics: List<String> = listOf("Rotational Motion", "Integration", "Coordination Compounds")
    ): JeeReadiness {
        if (testResults.isEmpty()) {
            val basePercentage = ((user.lecturesWatched * 3 + user.pyqsSolved * 2).coerceIn(15, 60))
            val baseLevel = (basePercentage / 5).coerceAtLeast(1)
            return JeeReadiness(
                readinessLevel = baseLevel,
                readinessPercentage = basePercentage,
                strongSubject = "Physics",
                strongSubjectAccuracy = 75,
                needsImprovementSubject = "Mathematics",
                needsImprovementAccuracy = 58,
                weakTopics = defaultWeakTopics,
                testCount = user.testsCompleted,
                totalLecturesWatched = user.lecturesWatched,
                totalPyqsSolved = user.pyqsSolved
            )
        }

        // Test average accuracy (40% weight)
        val avgTestAccuracy = testResults.map { it.accuracy }.average().coerceIn(0.0, 100.0)

        // Subject averages
        val physAcc = testResults.map { it.physicsScore.accuracyPercentage }.average()
        val chemAcc = testResults.map { it.chemistryScore.accuracyPercentage }.average()
        val mathAcc = testResults.map { it.mathsScore.accuracyPercentage }.average()

        val subjectAverages = listOf(
            "Physics" to physAcc,
            "Chemistry" to chemAcc,
            "Mathematics" to mathAcc
        )
        val strongest = subjectAverages.maxByOrNull { it.second } ?: ("Physics" to 75.0)
        val weakest = subjectAverages.minByOrNull { it.second } ?: ("Mathematics" to 60.0)

        // Pyq score (25% weight)
        val pyqFactor = (user.pyqsSolved * 2.5).coerceIn(0.0, 100.0)

        // Lecture coverage factor (20% weight)
        val lectureFactor = (user.lecturesWatched * 4.0).coerceIn(0.0, 100.0)

        // Streak factor (15% weight)
        val streakFactor = (user.streak * 10.0).coerceIn(0.0, 100.0)

        val compositeScore = (avgTestAccuracy * 0.40) +
                (pyqFactor * 0.25) +
                (lectureFactor * 0.20) +
                (streakFactor * 0.15)

        val percentage = compositeScore.roundToInt().coerceIn(5, 99)
        val level = (percentage / 5).coerceAtLeast(1)

        val allWeakTopics = testResults.flatMap { it.weakTopicsIdentified }
            .distinct()
            .ifEmpty { defaultWeakTopics }
            .take(4)

        return JeeReadiness(
            readinessLevel = level,
            readinessPercentage = percentage,
            strongSubject = strongest.first,
            strongSubjectAccuracy = strongest.second.roundToInt(),
            needsImprovementSubject = weakest.first,
            needsImprovementAccuracy = weakest.second.roundToInt(),
            weakTopics = allWeakTopics,
            testCount = testResults.size,
            totalLecturesWatched = user.lecturesWatched,
            totalPyqsSolved = user.pyqsSolved
        )
    }
}
