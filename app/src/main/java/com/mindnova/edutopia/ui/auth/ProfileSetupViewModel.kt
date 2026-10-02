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

data class ProfileSetupUiState(
    val name: String = "",
    val studentClass: String = "",
    val batchId: String = "",
    val photoUrl: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class ProfileSetupViewModel @JvmOverloads constructor(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    init {
        // Prefill from any existing (possibly incomplete) profile doc.
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            userRepo.getUser(uid).onSuccess { existing ->
                if (existing != null) {
                    _uiState.value = _uiState.value.copy(
                        name = existing.name,
                        studentClass = existing.studentClass,
                        batchId = existing.batchId,
                        photoUrl = existing.photoUrl
                    )
                }
            }
        }
    }

    fun onNameChange(v: String) { _uiState.value = _uiState.value.copy(name = v, error = null) }
    fun onClassChange(v: String) { _uiState.value = _uiState.value.copy(studentClass = v, error = null) }

    fun save() {
        val s = _uiState.value
        if (s.isSaving) return
        val error = AuthValidator.validateProfile(s.name, s.studentClass)
        if (error != null) {
            _uiState.value = s.copy(error = error)
            return
        }
        val uid = authRepo.currentUserId
        if (uid == null) {
            _uiState.value = s.copy(error = "Your session expired. Please sign in again.")
            return
        }

        _uiState.value = s.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val existing = userRepo.getUser(uid).getOrNull()
            val profile = (existing ?: User(uid = uid)).copy(
                uid = uid,
                name = s.name.trim(),
                email = existing?.email ?: authRepo.currentUser?.email ?: "",
                studentClass = s.studentClass,
                batchId = existing?.batchId ?: "",
                photoUrl = existing?.photoUrl ?: "",
                role = existing?.role ?: Constants.ROLE_STUDENT
            )
            userRepo.saveUserProfile(profile).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isSaving = false, saved = true) },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.localizedMessage ?: "Failed to save your profile."
                    )
                }
            )
        }
    }
}
