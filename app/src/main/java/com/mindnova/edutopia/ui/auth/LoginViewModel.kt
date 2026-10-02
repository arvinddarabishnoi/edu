package com.mindnova.edutopia.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.ProfileLookup
import com.mindnova.edutopia.data.repository.UserRepository
import com.mindnova.edutopia.domain.services.AuthValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where the login flow should route after a successful sign-in. */
enum class LoginDestination { None, ProfileSetup, StudentHome, AdminDashboard }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isSubmitting: Boolean = false,
    val isResettingPassword: Boolean = false,
    val resetEmail: String = "",
    val resetMessage: String? = null,
    val resetError: String? = null,
    val destination: LoginDestination = None
)

/**
 * @JvmOverloads generates the no-arg constructor required by the default
 * Compose `viewModel()` factory, while keeping dependencies injectable for tests.
 */
class LoginViewModel @JvmOverloads constructor(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            emailError = null,
            generalError = null
        )
    }

    fun onPasswordChange(value: String) {
        // Note: no trim here — the password keeps every character the user typed.
        _uiState.value = _uiState.value.copy(
            password = value,
            passwordError = null,
            generalError = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(generalError = null)
    }

    fun login() {
        val s = _uiState.value
        if (s.isSubmitting) return // prevent duplicate taps

        val validationError = AuthValidator.validateLogin(s.email, s.password)
        if (validationError != null) {
            _uiState.value = s.copy(generalError = validationError)
            return
        }

        _uiState.value = s.copy(isSubmitting = true, generalError = null)
        viewModelScope.launch {
            val result = authRepo.login(s.email, s.password)
            result.fold(
                onSuccess = { firebaseUser ->
                    // Auth succeeded — now decide the route from the profile state.
                    when (val profile = userRepo.lookupProfile(firebaseUser.uid)) {
                        is ProfileLookup.Failed -> {
                            // A Firestore outage must NOT be shown as "profile missing".
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                generalError =
                                "You are signed in, but your profile could not be loaded: ${profile.message}. " +
                                    "Check your connection and tap Sign In again."
                            )
                        }
                        ProfileLookup.Missing, is ProfileLookup.Incomplete -> {
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                destination = LoginDestination.ProfileSetup
                            )
                        }
                        is ProfileLookup.Complete -> {
                            routeCompleteProfile(profile.user)
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        generalError = e.localizedMessage ?: "Login failed. Please try again."
                    )
                }
            )
        }
    }

    private suspend fun routeCompleteProfile(user: com.mindnova.edutopia.data.models.User) {
        // Fire-and-track streak update; a failure here must not block entry.
        userRepo.updateLastActiveAndStreak(user.uid)
        _uiState.value = _uiState.value.copy(
            isSubmitting = false,
            destination = if (user.role == Constants.ROLE_ADMIN || user.role == Constants.ROLE_SUPER_ADMIN) {
                LoginDestination.AdminDashboard
            } else {
                LoginDestination.StudentHome
            }
        )
    }

    fun openResetDialog() {
        _uiState.value = _uiState.value.copy(
            resetEmail = _uiState.value.email,
            resetMessage = null,
            resetError = null
        )
    }

    fun onResetEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(resetEmail = value, resetError = null, resetMessage = null)
    }

    fun sendPasswordReset() {
        val s = _uiState.value
        if (s.isResettingPassword) return
        if (!AuthValidator.isValidEmail(s.resetEmail)) {
            _uiState.value = s.copy(resetError = "Enter a valid email address first.")
            return
        }
        _uiState.value = s.copy(isResettingPassword = true, resetError = null, resetMessage = null)
        viewModelScope.launch {
            authRepo.sendPasswordReset(s.resetEmail).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isResettingPassword = false,
                        resetMessage = "Password reset link sent to ${AuthValidator.normalizeEmail(s.resetEmail)}."
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isResettingPassword = false,
                        resetError = e.localizedMessage ?: "Could not send the reset email. Try again."
                    )
                }
            )
        }
    }
}
