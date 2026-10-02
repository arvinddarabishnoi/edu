package com.mindnova.edutopia.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.mindnova.edutopia.domain.services.AuthValidator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Email is normalized (trim + lowercase). The password is passed through
     * verbatim — it must NEVER be trimmed, leading/trailing spaces are part of
     * the credential.
     */
    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        val normalizedEmail = AuthValidator.normalizeEmail(email)
        return try {
            val authResult = auth.signInWithEmailAndPassword(normalizedEmail, pass).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Authentication returned a null user. Please retry."))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(friendlyAuthError(e))
        }
    }

    suspend fun signup(email: String, pass: String): Result<FirebaseUser> {
        val normalizedEmail = AuthValidator.normalizeEmail(email)
        return try {
            val authResult = auth.createUserWithEmailAndPassword(normalizedEmail, pass).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Account creation failed. Please retry."))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(friendlyAuthError(e))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val normalizedEmail = AuthValidator.normalizeEmail(email)
        return try {
            auth.sendPasswordResetEmail(normalizedEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(friendlyAuthError(e))
        }
    }

    fun logout() {
        auth.signOut()
    }

    private fun friendlyAuthError(e: Throwable): Exception {
        val message = when (e) {
            is FirebaseAuthInvalidUserException ->
                "No account found with this email. Check the address or sign up first."
            is FirebaseAuthInvalidCredentialsException ->
                "Incorrect email or password."
            is FirebaseAuthUserCollisionException ->
                "An account with this email already exists. Try signing in instead."
            is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Use at least ${AuthValidator.MIN_PASSWORD_LENGTH} characters."
            else -> e.message ?: "Authentication failed. Please check your connection and retry."
        }
        return Exception(message, e)
    }
}
