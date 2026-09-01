package com.mindnova.edutopia.core.navigation

sealed class Screen(val route: String) {
    // Startup & Auth
    object BrandIntro : Screen("brand_intro")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object ProfileSetup : Screen("profile_setup")

    // Student Bottom Nav
    object StudentHome : Screen("student_home")
    object Courses : Screen("courses")
    object TestsList : Screen("tests_list")
    object Leaderboard : Screen("leaderboard")
    object Profile : Screen("profile")

    // Student Feature Screens
    object SeriesDetail : Screen("series_detail/{seriesId}") {
        fun createRoute(seriesId: String) = "series_detail/$seriesId"
    }
    object LecturePlayer : Screen("lecture_player/{lectureId}") {
        fun createRoute(lectureId: String) = "lecture_player/$lectureId"
    }
    object TestInstructions : Screen("test_instructions/{testId}") {
        fun createRoute(testId: String) = "test_instructions/$testId"
    }
    object TestEngine : Screen("test_engine/{testId}") {
        fun createRoute(testId: String) = "test_engine/$testId"
    }
    object TestResult : Screen("test_result/{resultId}") {
        fun createRoute(resultId: String) = "test_result/$resultId"
    }
    object TestReview : Screen("test_review/{resultId}/{testId}") {
        fun createRoute(resultId: String, testId: String) = "test_review/$resultId/$testId"
    }
    object PyqList : Screen("pyq_list")
    object PyqPractice : Screen("pyq_practice?subject={subject}&year={year}") {
        fun createRoute(subject: String = "All", year: Int = 0) = "pyq_practice?subject=$subject&year=$year"
    }
    object TournamentList : Screen("tournament_list")
    object TournamentDetail : Screen("tournament_detail/{tournamentId}") {
        fun createRoute(tournamentId: String) = "tournament_detail/$tournamentId"
    }
    object Announcements : Screen("announcements")
    object EditProfile : Screen("edit_profile")

    // Admin Screens
    object AdminDashboard : Screen("admin_dashboard")
    object AdminUsers : Screen("admin_users")
    object AdminBanners : Screen("admin_banners")
    object AdminAnnouncements : Screen("admin_announcements")
    object AdminLectures : Screen("admin_lectures")
    object AdminSeries : Screen("admin_series")
    object AdminBatches : Screen("admin_batches")
    object AdminDailyGoals : Screen("admin_daily_goals")
    object AdminTests : Screen("admin_tests")
    object AdminQuestionEditor : Screen("admin_question_editor/{testId}") {
        fun createRoute(testId: String) = "admin_question_editor/$testId"
    }
    object AdminJsonImporter : Screen("admin_json_importer/{testId}") {
        fun createRoute(testId: String) = "admin_json_importer/$testId"
    }
    object AdminPyq : Screen("admin_pyq")
    object AdminTournaments : Screen("admin_tournaments")
    object AdminSuperManagement : Screen("admin_super_management")
    object AdminAuditLogs : Screen("admin_audit_logs")
    object AdminMessaging : Screen("admin_messaging")
}
