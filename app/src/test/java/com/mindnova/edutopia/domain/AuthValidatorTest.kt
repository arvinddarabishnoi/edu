package com.mindnova.edutopia.domain

import com.mindnova.edutopia.domain.services.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun `valid email passes`() {
        assertTrue(AuthValidator.isValidEmail("student@example.com"))
        assertTrue(AuthValidator.isValidEmail("  student+jee@sub.example.co.in  "))
    }

    @Test
    fun `invalid emails rejected`() {
        assertFalse(AuthValidator.isValidEmail(""))
        assertFalse(AuthValidator.isValidEmail("plainstring"))
        assertFalse(AuthValidator.isValidEmail("no@domain"))
        assertFalse(AuthValidator.isValidEmail("@example.com"))
        assertFalse(AuthValidator.isValidEmail("a b@example.com"))
    }

    @Test
    fun `password length rule uses raw password, never trimmed`() {
        // A 5-char password fails
        assertNotNull(AuthValidator.validateLogin("a@b.co", "12345"))
        // A 6-char password (including a space) passes because spaces are real characters.
        assertNull(AuthValidator.validateLogin("a@b.co", " 1234 "))
        // Trimming "12345 " to "12345" would wrongly REJECT a valid 6-char password.
        assertTrue(AuthValidator.isStrongEnough("12345 "))
        assertFalse(AuthValidator.isStrongEnough("12345"))
    }

    @Test
    fun `password with trailing space counted raw`() {
        // "12345" is 5 chars and fails; " 12345" is 6 and passes.
        assertFalse(AuthValidator.isStrongEnough("12345"))
        assertTrue(AuthValidator.isStrongEnough(" 12345"))
    }

    @Test
    fun `normalize email trims and lowercases but never touches password`() {
        assertEquals("user@example.com", AuthValidator.normalizeEmail("  User@Example.COM "))
    }

    @Test
    fun `signup requires matching confirmation`() {
        val err = AuthValidator.validateSignup(
            name = "Aman", email = "a@b.co", password = "secret1",
            confirmPassword = "secret2", studentClass = "Class 12"
        )
        assertNotNull(err)
        assertTrue(err!!.contains("match", ignoreCase = true))
    }

    @Test
    fun `signup requires class and name`() {
        assertNotNull(AuthValidator.validateSignup("", "a@b.co", "secret1", "secret1", ""))
        assertNull(
            AuthValidator.validateSignup(
                "Aman", "a@b.co", "secret1", "secret1", "Class 12"
            )
        )
    }

    @Test
    fun `login rejects blank password even if email valid`() {
        assertNotNull(AuthValidator.validateLogin("a@b.co", ""))
    }
}
