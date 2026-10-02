package com.mindnova.edutopia.core

import com.mindnova.edutopia.core.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every route declared for navigation must be unique, and parameterized
 * routes must keep their placeholder names in sync with createRoute().
 */
class ScreenRoutesTest {

    private val allRoutes = listOf(
        Screen.BrandIntro.route,
        Screen.Login.route,
        Screen.Signup.route,
        Screen.ForgotPassword.route,
        Screen.ProfileSetup.route,
        Screen.StudentHome.route,
        Screen.Courses.route,
        Screen.TestsList.route,
        Screen.Leaderboard.route,
        Screen.Profile.route,
        Screen.SeriesDetail.route,
        Screen.LecturePlayer.route,
        Screen.TestInstructions.route,
        Screen.TestEngine.route,
        Screen.TestResult.route,
        Screen.TestReview.route,
        Screen.PyqList.route,
        Screen.PyqPractice.route,
        Screen.TournamentList.route,
        Screen.TournamentDetail.route,
        Screen.Announcements.route,
        Screen.EditProfile.route,
        Screen.AdminDashboard.route,
        Screen.AdminUsers.route,
        Screen.AdminBanners.route,
        Screen.AdminAnnouncements.route,
        Screen.AdminLectures.route,
        Screen.AdminSeries.route,
        Screen.AdminBatches.route,
        Screen.AdminDailyGoals.route,
        Screen.AdminTests.route,
        Screen.AdminQuestionEditor.route,
        Screen.AdminJsonImporter.route,
        Screen.AdminPyq.route,
        Screen.AdminTournaments.route,
        Screen.AdminSuperManagement.route,
        Screen.AdminAuditLogs.route,
        Screen.AdminMessaging.route
    )

    @Test
    fun routesAreUnique() {
        assertEquals(allRoutes.size, allRoutes.toSet().size)
    }

    @Test
    fun parameterRoutesMatchCreatedPaths() {
        val seriesRoute = Screen.SeriesDetail.createRoute("abc123")
        assertTrue(seriesRoute.startsWith("series_detail/"))
        val placeholders = Screen.SeriesDetail.route.split("/").filter { it.contains("{") }
        assertTrue(placeholders.isNotEmpty())
        assertEquals("series_detail/abc123", seriesRoute)

        val review = Screen.TestReview.createRoute("r1", "t1")
        assertTrue(review.startsWith("test_review/r1/t1"))

        val pyq = Screen.PyqPractice.createRoute("Physics", 2024)
        assertEquals("pyq_practice?subject=Physics&year=2024", pyq)
        // argument names in the pattern must match the query keys
        assertTrue(Screen.PyqPractice.route.contains("subject={subject}"))
        assertTrue(Screen.PyqPractice.route.contains("year={year}"))
    }

    @Test
    fun everyArgumentPlaceholdersAreBalanced() {
        for (route in allRoutes) {
            val open = route.count { it == '{' }
            val close = route.count { it == '}' }
            assertEquals("unbalanced placeholder in $route", open, close)
        }
    }
}
