package com.mindnova.edutopia.core.utils

object Constants {
    const val APP_NAME = "EDUTOPIA"
    const val COMPANY_NAME = "MINDNOVA"

    // Firestore Collections
    const val COLL_USERS = "users"
    const val COLL_ADMINS = "admins"
    const val COLL_BANNERS = "banners"
    const val COLL_ANNOUNCEMENTS = "announcements"
    const val COLL_DAILY_GOALS = "dailyGoals"
    const val COLL_SERIES = "series"
    const val COLL_LECTURES = "lectures"
    const val COLL_TESTS = "tests"
    const val COLL_QUESTIONS = "questions"
    const val COLL_TEST_ATTEMPTS = "testAttempts"
    const val COLL_TEST_RESULTS = "testResults"
    const val COLL_PYQS = "pyqs"
    const val COLL_TOURNAMENTS = "tournaments"
    const val COLL_BATCHES = "batches"
    const val COLL_MESSAGES = "messages"
    const val COLL_AUDIT_LOGS = "auditLogs"
    const val COLL_APP_CONFIG = "appConfig"

    // Roles
    const val ROLE_STUDENT = "student"
    const val ROLE_ADMIN = "admin"
    const val ROLE_SUPER_ADMIN = "superAdmin"

    // Default Gamification Values
    const val XP_PER_LECTURE = 20
    const val XP_PER_DAILY_GOAL = 50
    const val XP_PER_TEST = 100
    const val XP_PER_PYQ = 10
    const val XP_PER_TOURNAMENT = 150

    // Subjects
    const val SUB_PHYSICS = "Physics"
    const val SUB_CHEMISTRY = "Chemistry"
    const val SUB_MATHEMATICS = "Mathematics"
}
