package com.mindnova.edutopia.nav

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindnova.edutopia.core.navigation.EdutopiaBottomNavBar
import com.mindnova.edutopia.core.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real bottom-navigation graph wiring (single-top, state
 * restore, no duplicate back-stack entries). Screens are lightweight stubs
 * here so this test verifies the NAVIGATION contract, independent of Firebase.
 */
@RunWith(AndroidJUnit4::class)
class NavigationWiringTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var navController: NavHostController

    private fun setUp() {
        rule.setContent {
            navController = rememberNavController()
            androidx.compose.foundation.layout.Column {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.StudentHome.route
                    ) {
                        composable(Screen.StudentHome.route) { Text("HOME") }
                        composable(Screen.Courses.route) { Text("COURSES") }
                        composable(Screen.TestsList.route) { Text("TESTS") }
                        composable(Screen.Leaderboard.route) { Text("RANKS") }
                        composable(Screen.Profile.route) { Text("PROFILE") }
                    }
                }
                EdutopiaBottomNavBar(navController = navController)
            }
        }
    }

    @Test
    fun bottomBarNavigatesBetweenTabs() {
        setUp()
        rule.onNodeWithText("HOME").assertExists()

        rule.onNodeWithContentDescription("Tests tab", substring = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("TESTS").assertExists()
        assertEquals(Screen.TestsList.route, navController.currentDestination?.route)

        rule.onNodeWithContentDescription("Profile tab", substring = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("PROFILE").assertExists()
        assertEquals(Screen.Profile.route, navController.currentDestination?.route)
    }

    @Test
    fun tabSwitchDoesNotDuplicateBackStack() {
        setUp()
        // Visit Tests twice; the start destination stays a single entry.
        rule.onNodeWithContentDescription("Tests tab", substring = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Home tab", substring = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Tests tab", substring = true).performClick()
        rule.waitForIdle()

        val stack = navController.backQueue
            .mapNotNull { it.destination?.route }
            .filter { it == Screen.StudentHome.route || it == Screen.TestsList.route }
        // Home (start) + Tests must each appear exactly once after tab churn.
        assertEquals(1, stack.count { it == Screen.TestsList.route })
        assertEquals(1, stack.count { it == Screen.StudentHome.route })
    }

    @Test
    fun backNavigationReturnsToPreviousTab() {
        setUp()
        rule.onNodeWithContentDescription("Rankings tab", substring = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("RANKS").assertExists()
        assertNotNull(navController.previousBackStackEntry)
        navController.popBackStack()
        rule.waitForIdle()
        rule.onNodeWithText("HOME").assertExists()
    }
}
