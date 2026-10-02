package com.mindnova.edutopia.ui.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.ProfileLookup
import com.mindnova.edutopia.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class IntroDestination { None, Login, ProfileSetup, StudentHome, AdminDashboard }

data class BrandIntroUiState(
    val animationDone: Boolean = false,
    val destination: IntroDestination = IntroDestination.None,
    val error: String? = null
)

/**
 * Resolves the startup route from the persisted Firebase Auth session:
 *  - signed out                        -> Login
 *  - signed in, profile missing/incomplete -> ProfileSetup
 *  - signed in, admin                  -> AdminDashboard
 *  - signed in, student                -> StudentHome
 *  - signed in, profile lookup FAILED  -> Error with retry (NOT Login!)
 *    Losing the session because of a transient Firestore outage would be a bug;
 *    the user stays on intro with a retry action instead.
 */
class BrandIntroViewModel @JvmOverloads constructor(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrandIntroUiState())
    val uiState: StateFlow<BrandIntroUiState> = _uiState.asStateFlow()

    init {
        resolveRoute()
    }

    fun resolveRoute() {
        _uiState.value = BrandIntroUiState()
        viewModelScope.launch {
            // Let the brand animation breathe, but don't block longer than needed.
            delay(1600L)

            val firebaseUser = authRepo.currentUser
            if (firebaseUser == null) {
                _uiState.value = _uiState.value.copy(
                    animationDone = true,
                    destination = IntroDestination.Login
                )
                return@launch
            }

            when (val profile = userRepo.lookupProfile(firebaseUser.uid)) {
                is ProfileLookup.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        animationDone = true,
                        error = "You are signed in, but your profile could not be loaded:\n" +
                            "${profile.message}\n" +
                            "Your session is preserved — retry once your connection is back."
                    )
                }
                ProfileLookup.Missing, is ProfileLookup.Incomplete -> {
                    _uiState.value = _uiState.value.copy(
                        animationDone = true,
                        destination = IntroDestination.ProfileSetup
                    )
                }
                is ProfileLookup.Complete -> {
                    val user = profile.user
                    // Refresh streak/lastActive on entry; failure here is non-fatal.
                    userRepo.updateLastActiveAndStreak(user.uid)
                    _uiState.value = _uiState.value.copy(
                        animationDone = true,
                        destination = if (user.role == Constants.ROLE_ADMIN || user.role == Constants.ROLE_SUPER_ADMIN) {
                            IntroDestination.AdminDashboard
                        } else {
                            IntroDestination.StudentHome
                        }
                    )
                }
            }
        }
    }
}
