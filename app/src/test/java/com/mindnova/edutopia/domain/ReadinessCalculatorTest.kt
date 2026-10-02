package com.mindnova.edutopia.domain

import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.SubjectPerformance
import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.domain.services.ReadinessCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessCalculatorTest {

    private fun user(tests: Int = 0, lectures: Int = 0, pyqs: Int = 0, streak: Int = 0) = User(
        uid = "u1",
        name = "S",
        studentClass = "Class 12",
        lecturesWatched = lectures,
        pyqsSolved = pyqs,
        testsCompleted = tests,
        streak = streak
    )

    private fun result(accuracy: Double, weak: List<String> = emptyList()) = TestResult(
        userId = "u1",
        testId = "t1",
        accuracy = accuracy,
        totalQuestions = 10,
        maxScore = 40,
        weakTopicsIdentified = weak,
        physicsScore = SubjectPerformance("Physics", 10, 20, 3, 1, 1, accuracy),
        chemistryScore = SubjectPerformance("Chemistry", 10, 20, 3, 1, 1, accuracy),
        mathsScore = SubjectPerformance("Mathematics", 10, 20, 3, 1, 1, accuracy)
    )

    @Test
    fun `no test data means NOT enough data and no fabricated subjects`() {
        val r = ReadinessCalculator.calculateReadiness(user(), emptyList())
        assertFalse(r.hasEnoughData)
        assertEquals(0, r.readinessPercentage)
        assertEquals(0, r.readinessLevel)
        assertNull(r.strongSubject)
        assertNull(r.needsImprovementSubject)
        assertTrue(r.weakTopics.isEmpty())
        assertTrue(r.missingDataHints.isNotEmpty())
    }

    @Test
    fun `single test still insufficient for subject claims but yields honest topics`() {
        val r = ReadinessCalculator.calculateReadiness(
            user(tests = 1),
            listOf(result(80.0, weak = listOf("Physics • Rotation")))
        )
        assertFalse(r.hasEnoughData)
        assertNull(r.strongSubject)
        assertEquals(listOf("Physics • Rotation"), r.weakTopics)
    }

    @Test
    fun `with two tests subject insight is computed from real data only`() {
        val r = ReadinessCalculator.calculateReadiness(
            user(tests = 2, pyqs = 50, lectures = 30, streak = 10),
            listOf(result(90.0), result(70.0))
        )
        assertTrue(r.hasEnoughData)
        assertEquals(80.0, r.strongSubjectAccuracy!!.toDouble(), 1.0) // avg of both (all equal)
        assertEquals(2, r.testCount)
    }

    @Test
    fun `percentage never exceeds 100 or below 0`() {
        val strong = ReadinessCalculator.calculateReadiness(
            user(tests = 3, pyqs = 999, lectures = 999, streak = 999),
            listOf(result(100.0), result(100.0), result(100.0))
        )
        assertEquals(100, strong.readinessPercentage)

        val weak = ReadinessCalculator.calculateReadiness(
            user(tests = 3),
            listOf(result(0.0), result(0.0), result(0.0))
        )
        assertTrue(weak.readinessPercentage >= 0)
    }

    @Test
    fun `empty resource is not treated as an error`() {
        // sanity: Resource.Empty distinct from Error (used across app)
        val empty: Resource<List<Int>> = Resource.Empty()
        val err: Resource<List<Int>> = Resource.Error("boom")
        assertTrue(empty is Resource.Empty)
        assertFalse(empty is Resource.Error)
        assertTrue(err is Resource.Error)
    }
}
