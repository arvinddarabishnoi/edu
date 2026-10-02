package com.mindnova.edutopia.domain

import com.mindnova.edutopia.data.models.Question
import com.mindnova.edutopia.data.models.TestModel
import com.mindnova.edutopia.domain.services.TestEvaluationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestEvaluationServiceTest {

    private fun q(
        id: String,
        correct: String,
        subject: String = "Physics",
        marks: Int = 4,
        neg: Int = 1,
        topic: String = "Kinematics",
        order: Int = 1
    ) = Question(
        id = id,
        testId = "t1",
        questionNumber = order,
        questionText = "Q $id",
        options = mapOf("A" to "a", "B" to "b", "C" to "c", "D" to "d"),
        correctAnswer = correct,
        subject = subject,
        topic = topic,
        marks = marks,
        negativeMarks = neg,
        order = order
    )

    private val test = TestModel(
        id = "t1",
        title = "Part A",
        totalQuestions = 5,
        marksPerQuestion = 4,
        negativeMarks = 1
    )

    @Test
    fun `correct wrong unattempted produce exact marks`() {
        val questions = listOf(
            q("q1", "A"), q("q2", "B"), q("q3", "C"), q("q4", "D", subject = "Chemistry"),
            q("q5", "A", subject = "Mathematics")
        )
        val answers = mapOf(
            "q1" to "A", // correct +4
            "q2" to "C", // wrong -1
            "q3" to "a", // lowercase-cased correct (case-insensitive) +4
            "q4" to ""   // blank -> unattempted 0
            // q5 absent -> unattempted 0
        )
        val r = TestEvaluationService.evaluate("u1", "Stu", test, questions, answers, 100L).result

        assertEquals(2, r.correctCount)
        assertEquals(1, r.incorrectCount)
        assertEquals(2, r.unattemptedCount)
        assertEquals(7, r.score) // 4 + 4 - 1
        assertEquals(20, r.maxScore) // 5 questions * 4
        // accuracy over ATTEMPTED only: 2 of 3
        assertEquals(66.7, r.accuracy, 0.01)
    }

    @Test
    fun `max score matches actual question set not the test default`() {
        val questions = listOf(
            q("q1", "A", marks = 3),
            q("q2", "A", marks = 2)
        )
        val r = TestEvaluationService.evaluate("u", "n", test, questions, emptyMap(), 1L).result
        assertEquals(5, r.maxScore)
        assertEquals(2, r.totalQuestions)
    }

    @Test
    fun `subject stats sum to totals`() {
        val questions = listOf(
            q("q1", "A", subject = "Physics"),
            q("q2", "B", subject = "Physics"),
            q("q3", "A", subject = "Chemistry"),
            q("q4", "A", subject = "Mathematics", marks = 5, neg = 2)
        )
        val answers = mapOf("q1" to "A", "q2" to "A", "q3" to "D", "q4" to "A")
        val eval = TestEvaluationService.evaluate("u", "n", test, questions, answers, 10L)
        val r = eval.result

        assertEquals(1, r.physicsScore.correctCount)
        assertEquals(1, r.physicsScore.incorrectCount)
        assertEquals(1, r.chemistryScore.incorrectCount)
        assertEquals(5, r.mathsScore.score)
        assertEquals(9, r.maxScore)
        assertEquals(4, r.correctCount + r.incorrectCount + r.unattemptedCount)

        // per-question verdicts agree with the aggregate
        assertEquals(4, eval.verdicts["q1"]!!.marksAwarded)
        assertEquals(-1, eval.verdicts["q2"]!!.marksAwarded)
        assertEquals(-1, eval.verdicts["q3"]!!.marksAwarded)
        assertEquals(5, eval.verdicts["q4"]!!.marksAwarded)
    }

    @Test
    fun `score can be negative and unattempted is zero impact`() {
        val questions = listOf(q("q1", "A"), q("q2", "A"))
        val answers = mapOf("q1" to "B", "q2" to "C")
        val r = TestEvaluationService.evaluate("u", "n", test, questions, answers, 1L).result
        assertEquals(-2, r.score)
        assertEquals(0, r.unattemptedCount)
    }

    @Test
    fun `no attempt means zero accuracy not a crash`() {
        val r = TestEvaluationService.evaluate("u", "n", test, listOf(q("q1", "A")), emptyMap(), 1L).result
        assertEquals(0.0, r.accuracy, 0.001)
        assertEquals(1, r.unattemptedCount)
    }

    @Test
    fun `percentile and rank are never fabricated`() {
        val questions = listOf(q("q1", "A"))
        val r = TestEvaluationService.evaluate("u", "n", test, questions, mapOf("q1" to "A"), 1L).result
        assertEquals(0.0, r.percentile, 0.0)
        assertEquals(0L, r.rank)
    }

    @Test
    fun `weak topics deterministic ordering`() {
        // Topic A: 0/2 correct, Topic B: 0/1 correct -> both <60%; order by acc then name, but equal acc -> name asc
        val questions = listOf(
            q("q1", "A", topic = "ZZ Top"), q("q2", "A", topic = "ZZ Top"),
            q("q3", "A", topic = "AA Top")
        )
        val answers = mapOf("q1" to "B", "q2" to "B", "q3" to "B")
        val r = TestEvaluationService.evaluate("u", "n", test, questions, answers, 1L).result
        assertTrue(r.weakTopicsIdentified.indexOf("Physics • AA Top") < r.weakTopicsIdentified.indexOf("Physics • ZZ Top"))
    }

    @Test
    fun `answers map is stored for the review screen`() {
        val questions = listOf(q("q1", "A"))
        val r = TestEvaluationService.evaluate("u", "n", test, questions, mapOf("q1" to " a "), 1L).result
        assertEquals(mapOf("q1" to "A"), r.answers)
    }
}
