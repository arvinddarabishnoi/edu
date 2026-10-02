package com.mindnova.edutopia.domain.services

/**
 * Pure, testable validation for credential input.
 * Email is trimmed; the password is NEVER trimmed (trailing/leading
 * whitespace is part of the password).
 */
object AuthValidator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.%'-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val MIN_PASSWORD_LENGTH = 6

    fun isValidEmail(raw: String): Boolean =
        raw.trim().let { it.length <= 254 && EMAIL_REGEX.matches(it) }

    fun isStrongEnough(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH

    fun validateLogin(email: String, password: String): String? {
        if (email.isBlank()) return "Please enter your email address."
        if (!isValidEmail(email)) return "That doesn't look like a valid email address."
        if (password.isEmpty()) return "Please enter your password."
        return null
    }

    fun validateSignup(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        studentClass: String
    ): String? {
        if (name.trim().length < 2) return "Please enter your full name (at least 2 characters)."
        if (email.isBlank()) return "Please enter your email address."
        if (!isValidEmail(email)) return "That doesn't look like a valid email address."
        if (!isStrongEnough(password))
            return "Password must be at least $MIN_PASSWORD_LENGTH characters."
        if (password != confirmPassword) return "Passwords do not match."
        if (studentClass.isBlank()) return "Please select your class."
        return null
    }

    fun validateProfile(name: String, studentClass: String): String? {
        if (name.trim().length < 2) return "Please enter your full name."
        if (studentClass.isBlank()) return "Please select your class."
        return null
    }

    /** Canonical email used for all auth calls: trimmed + lowercase. */
    fun normalizeEmail(raw: String): String = raw.trim().lowercase()
}
