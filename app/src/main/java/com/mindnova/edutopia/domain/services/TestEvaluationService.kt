package com.mindnova.edutopia.domain.services

import com.mindnova.edutopia.data.models.Question
import com.mindnova.edutopia.data.models.SubjectPerformance
import com.mindnova.edutopia.data.models.TestModel
import com.mindnova.edutopia.data.models.TestResult

/**
 * Pure test evaluation. Deterministic and consistent:
 *  - correct answer -> +q.marks
 *  - wrong answer   -> -q.negativeMarks
 *  - unattempted    -> 0
 *  - maxScore equals the sum of the actual question marks
 *  - accuracy is computed over ATTEMPTED questions only
 *  - subject statistics are derived from the same pass as the totals
 *  - weak topics are sorted deterministically
 *
 * NO fabricated percentile or rank: those fields stay 0 until a trusted
 * server-side dataset (every relevant attempt) provides them.
 */
object TestEvaluationService {

    data class PerQuestionVerdict(
        val questionId: String,
        val chosenAnswer: String?,   // null when unattempted
        val isCorrect: Boolean,
        val marksAwarded: Int
    )

    data class Evaluation(
        val result: TestResult,
        val verdicts: Map<String, PerQuestionVerdict>
    )

    fun evaluate(
        userId: String,
        userName: String,
        test: TestModel,
        questions: List<Question>,
        answers: Map<String, String>,
        timeSpentSeconds: Long
    ): Evaluation {
        var totalScore = 0
        var correctCount = 0
        var incorrectCount = 0
        var unattemptedCount = 0

        val subjectScores = mutableMapOf<String, Int>()
        val subjectCorrect = mutableMapOf<String, Int>()
        val subjectIncorrect = mutableMapOf<String, Int>()
        val subjectUnattempted = mutableMapOf<String, Int>()
        val subjectMax = mutableMapOf<String, Int>()

        val topicTotal = mutableMapOf<String, Int>()
        val topicCorrect = mutableMapOf<String, Int>()

        val verdicts = LinkedHashMap<String, PerQuestionVerdict>()

        for (q in questions) {
            val chosen = answers[q.id]?.trim()?.takeIf { it.isNotEmpty() }?.uppercase()
            val correct = q.correctAnswer.trim().uppercase()
            val topicKey = if (q.topic.isNotBlank()) {
                "${q.subject} • ${q.topic}"
            } else if (q.chapter.isNotBlank()) {
                "${q.subject} • ${q.chapter}"
            } else {
                q.subject
            }

            val marks = if (q.marks > 0) q.marks else test.marksPerQuestion.coerceAtLeast(0)
            val neg = if (q.negativeMarks >= 0) q.negativeMarks else test.negativeMarks.coerceAtLeast(0)

            subjectMax[q.subject] = (subjectMax[q.subject] ?: 0) + marks
            topicTotal[topicKey] = (topicTotal[topicKey] ?: 0) + 1

            val verdict: PerQuestionVerdict
            when {
                chosen == null -> {
                    unattemptedCount++
                    subjectUnattempted[q.subject] = (subjectUnattempted[q.subject] ?: 0) + 1
                    verdict = PerQuestionVerdict(q.id, null, false, 0)
                }
                chosen == correct -> {
                    totalScore += marks
                    correctCount++
                    subjectCorrect[q.subject] = (subjectCorrect[q.subject] ?: 0) + 1
                    subjectScores[q.subject] = (subjectScores[q.subject] ?: 0) + marks
                    topicCorrect[topicKey] = (topicCorrect[topicKey] ?: 0) + 1
                    verdict = PerQuestionVerdict(q.id, chosen, true, marks)
                }
                else -> {
                    // Invalid option letters (not in the question's options) count as wrong.
                    totalScore -= neg
                    incorrectCount++
                    subjectIncorrect[q.subject] = (subjectIncorrect[q.subject] ?: 0) + 1
                    subjectScores[q.subject] = (subjectScores[q.subject] ?: 0) - neg
                    verdict = PerQuestionVerdict(q.id, chosen, false, -neg)
                }
            }
            verdicts[q.id] = verdict
        }

        val maxScore = questions.sumOf { q ->
            if (q.marks > 0) q.marks else test.marksPerQuestion.coerceAtLeast(0)
        }
        val attemptedCount = correctCount + incorrectCount
        val accuracy = if (attemptedCount > 0) {
            Math.round(correctCount.toDouble() / attemptedCount.toDouble() * 1000.0) / 10.0
        } else {
            0.0
        }

        // Weak topics: topics with <60% accuracy, deterministically ordered
        // (ascending accuracy, then name), capped to 5.
        val weakTopics = topicTotal.entries
            .map { (topic, total) ->
                topic to ((topicCorrect[topic] ?: 0).toDouble() / total.toDouble()) * 100.0
            }
            .filter { it.second < 60.0 }
            .sortedWith(compareBy({ it.second }, { it.first }))
            .take(5)
            .map { it.first }

        fun buildSubjectPerf(subjectName: String): SubjectPerformance {
            val subMax = subjectMax[subjectName] ?: 0
            val subCorr = subjectCorrect[subjectName] ?: 0
            val subIncorr = subjectIncorrect[subjectName] ?: 0
            val subUnatt = subjectUnattempted[subjectName] ?: 0
            val subAttempted = subCorr + subIncorr
            val subAcc = if (subAttempted > 0) {
                Math.round(subCorr.toDouble() / subAttempted.toDouble() * 1000.0) / 10.0
            } else {
                0.0
            }
            return SubjectPerformance(
                subject = subjectName,
                score = subjectScores[subjectName] ?: 0,
                maxScore = subMax,
                correctCount = subCorr,
                incorrectCount = subIncorr,
                unattemptedCount = subUnatt,
                accuracyPercentage = subAcc
            )
        }

        val xpEarned = XP_PER_TEST.toLong()
        val pointsEarned = (totalScore.coerceAtLeast(0) / MARKS_PER_POINT) + BASE_PARTICIPATION_POINTS

        val result = TestResult(
            userId = userId,
            userName = userName,
            testId = test.id,
            testTitle = test.title,
            testCategory = test.category,
            score = totalScore,
            maxScore = maxScore,
            accuracy = accuracy,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unattemptedCount = unattemptedCount,
            totalQuestions = questions.size,
            timeSpentSeconds = timeSpentSeconds.coerceAtLeast(0L),
            percentile = 0.0, // never fabricate
            rank = 0L,       // never fabricate
            xpEarned = xpEarned,
            pointsEarned = pointsEarned,
            physicsScore = buildSubjectPerf("Physics"),
            chemistryScore = buildSubjectPerf("Chemistry"),
            mathsScore = buildSubjectPerf("Mathematics"),
            weakTopicsIdentified = weakTopics,
            answers = answers.mapValues { it.value.trim().uppercase() }
                .filterKeys { key -> questions.any { it.id == key } }
                .filterValues { it.isNotBlank() }
        )

        return Evaluation(result, verdicts)
    }

    const val XP_PER_TEST = 100
    const val MARKS_PER_POINT = 4
    const val BASE_PARTICIPATION_POINTS = 25L

    /**
     * Legacy-compatible entry point returning just the TestResult.
     */
    fun evaluateTest(
        userId: String,
        userName: String,
        test: TestModel,
        questions: List<Question>,
        answers: Map<String, String>,
        timeSpentSeconds: Long
    ): TestResult = evaluate(userId, userName, test, questions, answers, timeSpentSeconds).result
}
