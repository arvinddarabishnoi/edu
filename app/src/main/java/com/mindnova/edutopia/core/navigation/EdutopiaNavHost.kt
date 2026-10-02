package com.mindnova.edutopia.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.mindnova.edutopia.ui.admin.AnnouncementsAdminScreen
import com.mindnova.edutopia.ui.admin.AdminAuditLogsScreen
import com.mindnova.edutopia.ui.admin.AdminBannersScreen
import com.mindnova.edutopia.ui.admin.AdminBatchesScreen
import com.mindnova.edutopia.ui.admin.AdminDailyGoalsScreen
import com.mindnova.edutopia.ui.admin.AdminDashboardScreen
import com.mindnova.edutopia.ui.admin.AdminJsonImporterScreen
import com.mindnova.edutopia.ui.admin.AdminLecturesScreen
import com.mindnova.edutopia.ui.admin.AdminMessagingScreen
import com.mindnova.edutopia.ui.admin.AdminPyqScreen
import com.mindnova.edutopia.ui.admin.AdminQuestionEditorScreen
import com.mindnova.edutopia.ui.admin.AdminSeriesScreen
import com.mindnova.edutopia.ui.admin.AdminSuperManagementScreen
import com.mindnova.edutopia.ui.admin.AdminTestsScreen
import com.mindnova.edutopia.ui.admin.AdminTournamentsScreen
import com.mindnova.edutopia.ui.admin.AdminUsersScreen
import com.mindnova.edutopia.ui.auth.LoginScreen
import com.mindnova.edutopia.ui.auth.ProfileSetupScreen
import com.mindnova.edutopia.ui.auth.SignupScreen
import com.mindnova.edutopia.ui.home.HomeScreen
import com.mindnova.edutopia.ui.intro.MindnovaBrandIntroScreen
import com.mindnova.edutopia.ui.learn.CoursesScreen
import com.mindnova.edutopia.ui.learn.LecturePlayerScreen
import com.mindnova.edutopia.ui.learn.SeriesDetailScreen
import com.mindnova.edutopia.ui.leaderboard.LeaderboardScreen
import com.mindnova.edutopia.ui.profile.EditProfileScreen
import com.mindnova.edutopia.ui.profile.ProfileScreen
import com.mindnova.edutopia.ui.pyq.PyqListScreen
import com.mindnova.edutopia.ui.pyq.PyqPracticeScreen
import com.mindnova.edutopia.ui.student.AnnouncementsScreen
import com.mindnova.edutopia.ui.tests.TestEngineScreen
import com.mindnova.edutopia.ui.tests.TestInstructionsScreen
import com.mindnova.edutopia.ui.tests.TestResultScreen
import com.mindnova.edutopia.ui.tests.TestReviewScreen
import com.mindnova.edutopia.ui.tests.TestsListScreen
import com.mindnova.edutopia.ui.tournament.TournamentDetailScreen
import com.mindnova.edutopia.ui.tournament.TournamentListScreen

/** Student top-level routes that keep the floating bottom bar visible. */
private val bottomNavRoutes = setOf(
    Screen.StudentHome.route,
    Screen.Courses.route,
    Screen.TestsList.route,
    Screen.Leaderboard.route,
    Screen.Profile.route
)

private fun NavBackStackEntry.argString(key: String): String =
    arguments?.getString(key) ?: error("Missing navigation argument '$key' for route $route")

private fun NavBackStackEntry.argInt(key: String, default: Int = 0): Int =
    arguments?.getInt(key) ?: default

@Composable
fun EdutopiaNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                EdutopiaBottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.BrandIntro.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 14 }
            },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(140)) + slideOutHorizontally(tween(200)) { it / 14 } }
        ) {
            // ── Startup & Auth ────────────────────────────────────────
            composable(
                route = Screen.BrandIntro.route,
                enterTransition = { fadeIn(tween(400)) },
                exitTransition = { fadeOut(tween(300)) }
            ) {
                MindnovaBrandIntroScreen(navController)
            }

            composable(
                route = Screen.Login.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                LoginScreen(navController)
            }

            composable(route = Screen.Signup.route) {
                SignupScreen(navController)
            }

            composable(route = Screen.ForgotPassword.route) {
                com.mindnova.edutopia.ui.auth.ForgotPasswordScreen(navController)
            }

            composable(route = Screen.ProfileSetup.route) {
                ProfileSetupScreen(navController)
            }

            // ── Student tabs ──────────────────────────────────────────
            composable(route = Screen.StudentHome.route) {
                Box2(innerPadding) { HomeScreen(navController) }
            }
            composable(route = Screen.Courses.route) {
                Box2(innerPadding) { CoursesScreen(navController) }
            }
            composable(route = Screen.TestsList.route) {
                Box2(innerPadding) { TestsListScreen(navController) }
            }
            composable(route = Screen.Leaderboard.route) {
                Box2(innerPadding) { LeaderboardScreen(navController) }
            }
            composable(route = Screen.Profile.route) {
                Box2(innerPadding) { ProfileScreen(navController) }
            }

            // ── Student feature screens ───────────────────────────────
            composable(
                route = Screen.SeriesDetail.route,
                arguments = listOf(navArgument("seriesId") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "edutopia://series/{seriesId}" })
            ) { entry ->
                SeriesDetailScreen(navController, seriesId = entry.argString("seriesId"))
            }

            composable(
                route = Screen.LecturePlayer.route,
                arguments = listOf(navArgument("lectureId") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "edutopia://lecture/{lectureId}" })
            ) { entry ->
                LecturePlayerScreen(navController, lectureId = entry.argString("lectureId"))
            }

            composable(
                route = Screen.TestInstructions.route,
                arguments = listOf(navArgument("testId") { type = NavType.StringType })
            ) { entry ->
                TestInstructionsScreen(navController, testId = entry.argString("testId"))
            }

            composable(
                route = Screen.TestEngine.route,
                arguments = listOf(navArgument("testId") { type = NavType.StringType })
            ) { entry ->
                TestEngineScreen(navController, testId = entry.argString("testId"))
            }

            composable(
                route = Screen.TestResult.route,
                arguments = listOf(navArgument("resultId") { type = NavType.StringType })
            ) { entry ->
                TestResultScreen(navController, resultId = entry.argString("resultId"))
            }

            composable(
                route = Screen.TestReview.route,
                arguments = listOf(
                    navArgument("resultId") { type = NavType.StringType },
                    navArgument("testId") { type = NavType.StringType }
                )
            ) { entry ->
                TestReviewScreen(
                    navController,
                    resultId = entry.argString("resultId"),
                    testId = entry.argString("testId")
                )
            }

            composable(route = Screen.PyqList.route) {
                PyqListScreen(navController)
            }

            composable(
                route = Screen.PyqPractice.route,
                arguments = listOf(
                    navArgument("subject") {
                        type = NavType.StringType
                        defaultValue = "All"
                    },
                    navArgument("year") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { entry ->
                PyqPracticeScreen(
                    navController,
                    subject = entry.arguments?.getString("subject").orEmpty().ifBlank { "All" },
                    year = entry.argInt("year", 0)
                )
            }

            composable(route = Screen.TournamentList.route) {
                TournamentListScreen(navController)
            }

            composable(
                route = Screen.TournamentDetail.route,
                arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
            ) { entry ->
                TournamentDetailScreen(navController, tournamentId = entry.argString("tournamentId"))
            }

            composable(
                route = Screen.Announcements.route,
                deepLinks = listOf(navDeepLink { uriPattern = "edutopia://announcements" })
            ) {
                AnnouncementsScreen(navController)
            }

            composable(route = Screen.EditProfile.route) {
                EditProfileScreen(navController)
            }

            // ── Admin ─────────────────────────────────────────────────
            composable(route = Screen.AdminDashboard.route) {
                AdminDashboardScreen(navController)
            }
            composable(route = Screen.AdminUsers.route) {
                AdminUsersScreen(navController)
            }
            composable(route = Screen.AdminBanners.route) {
                AdminBannersScreen(navController)
            }
            composable(
                route = Screen.AdminAnnouncements.route,
                deepLinks = listOf(navDeepLink { uriPattern = "edutopia://admin/announcements" })
            ) {
                AnnouncementsAdminScreen(navController)
            }
            composable(route = Screen.AdminLectures.route) {
                AdminLecturesScreen(navController)
            }
            composable(route = Screen.AdminSeries.route) {
                AdminSeriesScreen(navController)
            }
            composable(
                route = Screen.AdminBatches.route,
                deepLinks = listOf(navDeepLink { uriPattern = "edutopia://batches" })
            ) {
                AdminBatchesScreen(navController)
            }
            composable(route = Screen.AdminDailyGoals.route) {
                AdminDailyGoalsScreen(navController)
            }
            composable(route = Screen.AdminTests.route) {
                AdminTestsScreen(navController)
            }
            composable(
                route = Screen.AdminQuestionEditor.route,
                arguments = listOf(navArgument("testId") { type = NavType.StringType })
            ) { entry ->
                AdminQuestionEditorScreen(navController, testId = entry.argString("testId"))
            }
            composable(
                route = Screen.AdminJsonImporter.route,
                arguments = listOf(navArgument("testId") { type = NavType.StringType })
            ) { entry ->
                AdminJsonImporterScreen(navController, testId = entry.argString("testId"))
            }
            composable(route = Screen.AdminPyq.route) {
                AdminPyqScreen(navController)
            }
            composable(route = Screen.AdminTournaments.route) {
                AdminTournamentsScreen(navController)
            }
            composable(route = Screen.AdminSuperManagement.route) {
                AdminSuperManagementScreen(navController)
            }
            composable(route = Screen.AdminAuditLogs.route) {
                AdminAuditLogsScreen(navController)
            }
            composable(route = Screen.AdminMessaging.route) {
                AdminMessagingScreen(navController)
            }
        }
    }
}

/**
 * Content host for bottom-nav screens: reserves space for the floating bar so
 * content never hides behind it, while backgrounds stay edge-to-edge.
 */
@Composable
private fun Box2(padding: PaddingValues, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(padding)
            .padding(bottom = padding.calculateBottomPadding())
    ) {
        content()
    }
}
