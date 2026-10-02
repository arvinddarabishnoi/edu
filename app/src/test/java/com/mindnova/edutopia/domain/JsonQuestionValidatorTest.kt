package com.mindnova.edutopia.domain

import com.mindnova.edutopia.domain.services.JsonQuestionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonQuestionValidatorTest {

    private val validItem = """
        {
          "question": "A particle moves with v=2t. Find a at t=3.",
          "options": {"A": "2", "B": "3", "C": "6", "D": "9"},
          "correctAnswer": "A",
          "subject": "Physics",
          "difficulty": "Easy",
          "marks": 4,
          "negativeMarks": 1,
          "explanation": "a = dv/dt = 2"
        }
    """.trimIndent()

    @Test
    fun `single object accepted`() {
        val r = JsonQuestionValidator.validateJson(validItem)
        assertNull(r.globalError)
        assertEquals(1, r.totalCount)
        assertEquals(1, r.validCount)
        assertTrue(r.allValid)
        assertEquals("A", r.items[0].question?.correctAnswer)
    }

    @Test
    fun `array of objects accepted and ordered`() {
        val r = JsonQuestionValidator.validateJson("[$validItem, $validItem]")
        assertEquals(2, r.totalCount)
        assertEquals(2, r.validCount)
        assertEquals(1, r.items[0].question?.questionNumber)
        assertEquals(2, r.items[1].question?.questionNumber)
    }

    @Test
    fun `missing question text rejected`() {
        val r = JsonQuestionValidator.validateJson(
            """{"options": {"A":"1","B":"2","C":"3","D":"4"}, "correctAnswer": "A"}"""
        )
        assertFalse(r.items[0].isValid)
        assertTrue(r.items[0].errors.any { it.contains("question", ignoreCase = true) })
    }

    @Test
    fun `missing option D rejected, blank option rejected`() {
        val r = JsonQuestionValidator.validateJson(
            """{"question":"Q","options":{"A":"1","B":"2","C":"3"},"correctAnswer":"A"}"""
        )
        assertTrue(r.items[0].errors.any { it.contains("D") })

        val r2 = JsonQuestionValidator.validateJson(
            """{"question":"Q","options":{"A":"1","B":"2","C":"3","D":"  "},"correctAnswer":"A"}"""
        )
        assertTrue(r2.items[0].errors.any { it.contains("blank", ignoreCase = true) })
    }

    @Test
    fun `invalid correct answer rejected`() {
        val r = JsonQuestionValidator.validateJson(
            """{"question":"Q","options":{"A":"1","B":"2","C":"3","D":"4"},"correctAnswer":"E"}"""
        )
        assertFalse(r.items[0].isValid)
        assertTrue(r.items[0].errors.any { it.contains("correctAnswer") })
    }

    @Test
    fun `invalid subject and difficulty rejected`() {
        val r = JsonQuestionValidator.validateJson(
            """{"question":"Q","options":{"A":"1","B":"2","C":"3","D":"4"},"correctAnswer":"A",
                "subject":"Biology","difficulty":"Extreme","marks":4,"negativeMarks":1}"""
        )
        assertTrue(r.items[0].errors.any { it.contains("subject", ignoreCase = true) })
        assertTrue(r.items[0].errors.any { it.contains("difficulty", ignoreCase = true) })
    }

    @Test
    fun `invalid and negative marks rejected`() {
        val r = JsonQuestionValidator.validateJson(
            """{"question":"Q","options":{"A":"1","B":"2","C":"3","D":"4"},"correctAnswer":"A",
                "marks":-4,"negativeMarks":1}"""
        )
        assertTrue(r.items[0].errors.any { it.contains("marks", ignoreCase = true) })

        val r2 = JsonQuestionValidator.validateJson(
            """{"question":"Q","options":{"A":"1","B":"2","C":"3","D":"4"},"correctAnswer":"A",
                "marks":4,"negativeMarks":6}"""
        )
        assertTrue(r2.items[0].errors.any { it.contains("negativeMarks", ignoreCase = true) })

        val r3 = JsonQuestionValidator.validateJson(
            """{"question":"Q","options":{"A":"1","B":"2","C":"3","D":"4"},"correctAnswer":"A",
                "marks":"four"}"""
        )
        assertFalse(r3.items[0].isValid)
    }

    @Test
    fun `primitive null and nested-array items produce item-level errors, never skipped`() {
        val r = JsonQuestionValidator.validateJson(
            """[$validItem, "just a string", null, 42, [1,2]]"""
        )
        // every array element is counted: 5 total, 1 valid, 4 invalid
        assertEquals(5, r.totalCount)
        assertEquals(1, r.validCount)
        assertEquals(4, r.invalidCount)
        assertTrue(r.items[1].errors.any { it.contains("not a JSON object") })
        assertTrue(r.items[2].errors.any { it.contains("null") })
        assertFalse(r.allValid)
    }

    @Test
    fun `empty and malformed input produce global errors`() {
        assertEquals(0, JsonQuestionValidator.validateJson("").totalCount)
        assertNotNull(JsonQuestionValidator.validateJson("").globalError)
        assertNotNull(JsonQuestionValidator.validateJson("{not json] ").globalError)
        assertNotNull(JsonQuestionValidator.validateJson("null").globalError)
        assertNotNull(JsonQuestionValidator.validateJson("[]").globalError)
    }

    @Test
    fun `options array form supported`() {
        val r = JsonQuestionValidator.validateJson(
            """{"question":"Q","options":["1","2","3","4"],"answer":"b"}"""
        )
        assertTrue(r.items[0].isValid)
        assertEquals("B", r.items[0].question?.correctAnswer)
        assertEquals("2", r.items[0].question?.options?.get("B"))
    }

    @Test
    fun `unsupported question type rejected`() {
        val r = JsonQuestionValidator.validateJson(
            """{"question":"Q","options":{"A":"1","B":"2","C":"3","D":"4"},"correctAnswer":"A",
                "questionType":"Multiple Correct"}"""
        )
        assertTrue(r.items[0].errors.any { it.contains("questionType") })
    }

    @Test
    fun `import requires all items valid`() {
        val allGood = JsonQuestionValidator.validateJson("[$validItem]")
        val oneBad = JsonQuestionValidator.validateJson("[$validItem, {}]")
        assertTrue(allGood.allValid)
        assertFalse(oneBad.allValid)
    }
}
