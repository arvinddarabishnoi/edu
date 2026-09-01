package com.mindnova.edutopia.domain.services

import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.Question
import com.mindnova.edutopia.data.models.SubjectPerformance
import com.mindnova.edutopia.data.models.TestModel
import com.mindnova.edutopia.data.models.TestResult

object TestEvaluationService {

    fun evaluateTest(
        userId: String,
        userName: String,
        test: TestModel,
        questions: List<Question>,
        answers: Map<String, String>, // questionId -> chosen option "A".."D"
        timeSpentSeconds: Long
    ): TestResult {
        var totalScore = 0
        var correctCount = 0
        var incorrectCount = 0
        var unattemptedCount = 0

        // Subject Maps
        val subjectQuestions = questions.groupBy { it.subject }
        val subjectScores = mutableMapOf<String, Int>()
        val subjectCorrect = mutableMapOf<String, Int>()
        val subjectIncorrect = mutableMapOf<String, Int>()
        val subjectUnattempted = mutableMapOf<String, Int>()

        // Topic accuracy map to identify weak topics
        val topicTotal = mutableMapOf<String, Int>()
        val topicCorrect = mutableMapOf<String, Int>()

        for (q in questions) {
            val chosen = answers[q.id]?.trim()?.uppercase()
            val correct = q.correctAnswer.trim().uppercase()
            val topicKey = if (q.topic.isNotBlank()) "${q.subject} • ${q.topic}" else "${q.subject} • ${q.chapter}"

            topicTotal[topicKey] = (topicTotal[topicKey] ?: 0) + 1

            if (chosen.isNullOrBlank()) {
                unattemptedCount++
                subjectUnattempted[q.subject] = (subjectUnattempted[q.subject] ?: 0) + 1
            } else if (chosen == correct) {
                totalScore += q.marks
                correctCount++
                subjectCorrect[q.subject] = (subjectCorrect[q.subject] ?: 0) + 1
                subjectScores[q.subject] = (subjectScores[q.subject] ?: 0) + q.marks
                topicCorrect[topicKey] = (topicCorrect[topicKey] ?: 0) + 1
            } else {
                totalScore -= q.negativeMarks
                incorrectCount++
                subjectIncorrect[q.subject] = (subjectIncorrect[q.subject] ?: 0) + 1
                subjectScores[q.subject] = (subjectScores[q.subject] ?: 0) - q.negativeMarks
            }
        }

        val totalQuestionsCount = questions.size.coerceAtLeast(1)
        val maxScore = questions.sumOf { it.marks }.coerceAtLeast(1)
        val attemptedCount = correctCount + incorrectCount
        val accuracy = if (attemptedCount > 0) {
            (correctCount.toDouble() / attemptedCount.toDouble()) * 100.0
        } else {
            0.0
        }

        // Weak topics: topics where accuracy < 60%
        val weakTopics = topicTotal.filter { (topic, total) ->
            val correct = topicCorrect[topic] ?: 0
            val acc = (correct.toDouble() / total.toDouble()) * 100.0
            acc < 60.0
        }.keys.take(5).toList()

        fun buildSubjectPerf(subjectName: String): SubjectPerformance {
            val qList = subjectQuestions[subjectName] ?: emptyList()
            val subMax = qList.sumOf { it.marks }.coerceAtLeast(1)
            val subScore = subjectScores[subjectName] ?: 0
            val subCorr = subjectCorrect[subjectName] ?: 0
            val subIncorr = subjectIncorrect[subjectName] ?: 0
            val subUnatt = subjectUnattempted[subjectName] ?: 0
            val subAttempted = subCorr + subIncorr
            val subAcc = if (subAttempted > 0) (subCorr.toDouble() / subAttempted.toDouble()) * 100.0 else 0.0
            return SubjectPerformance(
                subject = subjectName,
                score = subScore,
                maxScore = subMax,
                correctCount = subCorr,
                incorrectCount = subIncorr,
                unattemptedCount = subUnatt,
                accuracyPercentage = Math.round(subAcc * 10.0) / 10.0
            )
        }

        val physicsPerf = buildSubjectPerf(Constants.SUB_PHYSICS)
        val chemPerf = buildSubjectPerf(Constants.SUB_CHEMISTRY)
        val mathPerf = buildSubjectPerf(Constants.SUB_MATHEMATICS)

        val xpEarned = Constants.XP_PER_TEST.toLong()
        val pointsEarned = (totalScore.coerceAtLeast(0) / 4).toLong() + 25L

        // Estimated percentile formula from score/maxScore
        val normalizedScoreRatio = (totalScore.toDouble() / maxScore.toDouble()).coerceIn(0.0, 1.0)
        val estimatedPercentile = Math.round((70.0 + (normalizedScoreRatio * 29.9)) * 10.0) / 10.0

        return TestResult(
            userId = userId,
            userName = userName,
            testId = test.id,
            testTitle = test.title,
            testCategory = test.category,
            score = totalScore,
            maxScore = maxScore,
            accuracy = Math.round(accuracy * 10.0) / 10.0,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unattemptedCount = unattemptedCount,
            totalQuestions = totalQuestionsCount,
            timeSpentSeconds = timeSpentSeconds,
            percentile = estimatedPercentile,
            rank = 1L,
            xpEarned = xpEarned,
            pointsEarned = pointsEarned,
            physicsScore = physicsPerf,
            chemistryScore = chemPerf,
            mathsScore = mathPerf,
            weakTopicsIdentified = weakTopics,
            submittedAt = System.currentTimeMillis()
        )
    }
}
