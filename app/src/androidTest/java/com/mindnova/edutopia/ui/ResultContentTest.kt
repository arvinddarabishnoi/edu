package com.mindnova.edutopia.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindnova.edutopia.core.theme.EdutopiaTheme
import com.mindnova.edutopia.data.models.SubjectPerformance
import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.ui.tests.TestResultContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleResult = TestResult(
        id = "r1",
        userId = "u1",
        testId = "t1",
        testTitle = "JEE Main Full Syllabus — Test 05",
        score = 47,
        maxScore = 120,
        accuracy = 78.6,
        correctCount = 15,
        incorrectCount = 4,
        unattemptedCount = 1,
        totalQuestions = 20,
        timeSpentSeconds = 5400,
        percentile = 0.0, // never fabricated
        rank = 0L,
        physicsScore = SubjectPerformance("Physics", 20, 40, 6, 2, 2, 75.0),
        chemistryScore = SubjectPerformance("Chemistry", 15, 40, 5, 1, 4, 83.3),
        mathsScore = SubjectPerformance("Mathematics", 12, 40, 4, 1, 5, 80.0),
        weakTopicsIdentified = listOf("Physics • Rotation")
    )

    @Test
    fun resultShowsExactNumbersAndNoFakeRank() {
        var reviewed = false
        composeRule.setContent {
            EdutopiaTheme {
                TestResultContent(
                    result = sampleResult,
                    onReview = { reviewed = true },
                    onBackToTests = {}
                )
            }
        }
        // Score is split across two text nodes ("47" and " / 120") for size contrast.
        composeRule.onNodeWithText("47", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(" / 120", substring = true, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("78.6% accuracy", substring = true).assertExists()
        // Honest disclosure replaces fabricated percentile/rank copy.
        composeRule
            .onNodeWithText("Rank & percentile are only published after", substring = true)
            .assertExists()
        composeRule.onNodeWithText("Topics needing attention").assertExists()

        composeRule.onNodeWithText("Review Answers").performClick()
        assertEquals(true, reviewed)
    }
}
