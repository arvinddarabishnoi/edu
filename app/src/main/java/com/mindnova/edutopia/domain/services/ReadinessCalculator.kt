package com.mindnova.edutopia.domain.services

import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.data.models.User
import kotlin.math.roundToInt

/**
 * Student readiness insight. IMPORTANT semantics:
 *  - This is an ESTIMATED learning insight, never an official exam percentile.
 *  - With insufficient data the result carries hasEnoughData = false and the
 *    UI must render "Not enough data yet" plus guidance — no fabricated subjects.
 *  - Strong/weak subject analysis requires MIN_TESTS_FOR_INSIGHTS actual test
 *    results; before that no subject claims are made at all.
 */
data class JeeReadiness(
    val hasEnoughData: Boolean,
    val readinessLevel: Int,
    val readinessPercentage: Int,
    val strongSubject: String?,
    val strongSubjectAccuracy: Int?,
    val needsImprovementSubject: String?,
    val needsImprovementAccuracy: Int?,
    val weakTopics: List<String>,
    val testCount: Int,
    val totalLecturesWatched: Int,
    val totalPyqsSolved: Int,
    val missingDataHints: List<String>
)

object ReadinessCalculator {

    const val MIN_TESTS_FOR_INSIGHTS = 2
    const val TESTS_WEIGHT = 0.40
    const val PYQ_WEIGHT = 0.25
    const val LECTURE_WEIGHT = 0.20
    const val STREAK_WEIGHT = 0.15
    const val PYQS_FOR_FULL_PYQ_SCORE = 40
    const val LECTURES_FOR_FULL_SCORE = 25
    const val STREAK_DAYS_FOR_FULL_SCORE = 10

    fun calculateReadiness(
        user: User,
        testResults: List<TestResult>
    ): JeeReadiness {
        val submittedResults = testResults.filter { it.totalQuestions > 0 }
        val hasEnoughData = submittedResults.size >= MIN_TESTS_FOR_INSIGHTS

        val missingHints = buildList {
            if (submittedResults.size < MIN_TESTS_FOR_INSIGHTS) {
                add("Take ${MIN_TESTS_FOR_INSIGHTS - submittedResults.size} more full-length test(s)")
            }
            if (user.pyqsSolved < 10) add("Solve more PYQs (${user.pyqsSolved}/10 recent)")
            if (user.lecturesWatched < 5) add("Watch a few concept lectures")
        }

        if (!hasEnoughData) {
            return JeeReadiness(
                hasEnoughData = false,
                readinessLevel = 0,
                readinessPercentage = 0,
                strongSubject = null,
                strongSubjectAccuracy = null,
                needsImprovementSubject = null,
                needsImprovementAccuracy = null,
                weakTopics = if (submittedResults.isEmpty()) {
                    emptyList()
                } else {
                    // A single test still yields honest per-topic feedback.
                    submittedResults.flatMap { it.weakTopicsIdentified }.distinct().take(4)
                },
                testCount = submittedResults.size,
                totalLecturesWatched = user.lecturesWatched,
                totalPyqsSolved = user.pyqsSolved,
                missingDataHints = missingHints
            )
        }

        val avgTestAccuracy = submittedResults.map { it.accuracy }.average().coerceIn(0.0, 100.0)

        // Only count subjects that actually appeared in the tests.
        data class SubAgg(val acc: Double, val samples: Int)
        val subjectAcc = listOf(
            "Physics" to submittedResults.map { it.physicsScore }
                .filter { it.maxScore > 0 || it.correctCount + it.incorrectCount + it.unattemptedCount > 0 },
            "Chemistry" to submittedResults.map { it.chemistryScore }
                .filter { it.maxScore > 0 || it.correctCount + it.incorrectCount + it.unattemptedCount > 0 },
            "Mathematics" to submittedResults.map { it.mathsScore }
                .filter { it.maxScore > 0 || it.correctCount + it.incorrectCount + it.unattemptedCount > 0 }
        ).mapNotNull { (name, perfs) ->
            if (perfs.isEmpty()) return@mapNotNull null
            val acc = perfs.map { it.accuracyPercentage }.average()
            name to SubAgg(acc, perfs.size)
        }

        val strongest = subjectAcc.maxByOrNull { it.second.acc }
        val weakest = subjectAcc.minByOrNull { it.second.acc }

        val pyqFactor = (user.pyqsSolved.toDouble() / PYQS_FOR_FULL_PYQ_SCORE * 100.0).coerceIn(0.0, 100.0)
        val lectureFactor = (user.lecturesWatched.toDouble() / LECTURES_FOR_FULL_SCORE * 100.0).coerceIn(0.0, 100.0)
        val streakFactor = (user.streak.toDouble() / STREAK_DAYS_FOR_FULL_SCORE * 100.0).coerceIn(0.0, 100.0)

        val composite = avgTestAccuracy * TESTS_WEIGHT +
            pyqFactor * PYQ_WEIGHT +
            lectureFactor * LECTURE_WEIGHT +
            streakFactor * STREAK_WEIGHT

        val percentage = composite.roundToInt().coerceIn(0, 100)

        val mergedWeak = submittedResults
            .flatMap { it.weakTopicsIdentified }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(4)
            .map { it.key }

        return JeeReadiness(
            hasEnoughData = true,
            readinessLevel = (percentage / 10).coerceAtLeast(1),
            readinessPercentage = percentage,
            strongSubject = strongest?.first,
            strongSubjectAccuracy = strongest?.second?.acc?.roundToInt(),
            needsImprovementSubject = weakest?.takeIf { it.second.acc < 100.0 }?.first,
            needsImprovementAccuracy = weakest?.takeIf { it.second.acc < 100.0 }?.second?.acc?.roundToInt(),
            weakTopics = mergedWeak,
            testCount = submittedResults.size,
            totalLecturesWatched = user.lecturesWatched,
            totalPyqsSolved = user.pyqsSolved,
            missingDataHints = missingHints
        )
    }
}
