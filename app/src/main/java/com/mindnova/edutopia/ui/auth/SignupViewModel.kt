package com.mindnova.edutopia.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.UserRepository
import com.mindnova.edutopia.domain.services.AuthValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val studentClass: String = "",
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val accountCreated: Boolean = false
)

class SignupViewModel @JvmOverloads constructor(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onNameChange(v: String) = update { copy(name = v, error = null) }
    fun onEmailChange(v: String) = update { copy(email = v, error = null) }
    fun onPasswordChange(v: String) = update { copy(password = v, error = null) }
    fun onConfirmChange(v: String) = update { copy(confirmPassword = v, error = null) }
    fun onClassChange(v: String) = update { copy(studentClass = v, error = null) }

    private inline fun update(transform: SignupUiState.() -> SignupUiState) {
        _uiState.value = _uiState.value.transform()
    }

    fun signup() {
        val s = _uiState.value
        if (s.isSubmitting) return

        val error = AuthValidator.validateSignup(
            name = s.name,
            email = s.email,
            password = s.password,
            confirmPassword = s.confirmPassword,
            studentClass = s.studentClass
        )
        if (error != null) {
            _uiState.value = s.copy(error = error)
            return
        }

        _uiState.value = s.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            authRepo.signup(s.email, s.password).fold(
                onSuccess = { firebaseUser ->
                    val profile = User(
                        uid = firebaseUser.uid,
                        name = s.name.trim(),
                        email = AuthValidator.normalizeEmail(s.email),
                        studentClass = s.studentClass,
                        role = Constants.ROLE_STUDENT
                    )
                    userRepo.saveUserProfile(profile).fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(isSubmitting = false, accountCreated = true)
                        },
                        onFailure = { e ->
                            // Auth succeeded but profile failed: tell the user precisely.
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                error = "Account was created, but your profile could not be saved: " +
                                    "${e.localizedMessage}. Sign in and it will retry profile setup."
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = e.localizedMessage ?: "Signup failed. Please try again."
                    )
                }
            )
        }
    }
}
