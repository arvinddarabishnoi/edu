package com.mindnova.edutopia.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.AppTopBar
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.EdutopiaTextField
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.firstSettled
import com.mindnova.edutopia.data.models.Batch
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.BatchRepository
import com.mindnova.edutopia.data.repository.UserRepository
import com.mindnova.edutopia.domain.services.AuthValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val user: User? = null,
    val name: String = "",
    val photoUrl: String = "",
    val studentClass: String = "",
    val batches: List<Batch> = emptyList(),
    val selectedBatchId: String = "",
    val joinCode: String = "",
    val message: String? = null,
    val messageIsError: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val loading: Boolean = true
)

class EditProfileViewModel @JvmOverloads constructor(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val batchRepo: BatchRepository = BatchRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = authRepo.currentUserId
            if (uid == null) {
                _state.value = _state.value.copy(loading = false, message = "Signed out", messageIsError = true)
                return@launch
            }
            when (val r = userRepo.getUserFlow(uid).firstSettled()) {
                is Resource.Success -> _state.value = _state.value.copy(
                    user = r.data,
                    name = r.data.name,
                    photoUrl = r.data.photoUrl,
                    studentClass = r.data.studentClass,
                    selectedBatchId = r.data.batchId,
                    loading = false
                )
                is Resource.Error -> _state.value = _state.value.copy(
                    loading = false,
                    message = r.message,
                    messageIsError = true
                )
                is Resource.Empty -> _state.value = _state.value.copy(
                    loading = false,
                    message = "No profile found — complete profile setup first.",
                    messageIsError = true
                )
                is Resource.Loading -> Unit
            }
        }
        viewModelScope.launch {
            batchRepo.getAllBatchesFlow().collect { r ->
                if (r is Resource.Success) {
                    _state.value = _state.value.copy(batches = r.data.filter { it.status == "active" })
                }
            }
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, message = null) }
    fun onPhotoChange(v: String) { _state.value = _state.value.copy(photoUrl = v.trim(), message = null) }
    fun onClassChange(v: String) { _state.value = _state.value.copy(studentClass = v, message = null) }
    fun selectBatch(id: String) { _state.value = _state.value.copy(selectedBatchId = id, message = null) }
    fun onJoinCodeChange(v: String) { _state.value = _state.value.copy(joinCode = v.uppercase().trim(), message = null) }

    fun save() {
        val s = _state.value
        val user = s.user ?: return
        if (s.saving) return
        val validation = AuthValidator.validateProfile(s.name, s.studentClass)
        if (validation != null) {
            _state.value = s.copy(message = validation, messageIsError = true)
            return
        }

        _state.value = s.copy(saving = true, message = null)
        viewModelScope.launch {
            val updated = user.copy(
                name = s.name.trim(),
                photoUrl = s.photoUrl,
                studentClass = s.studentClass
            )
            userRepo.saveUserProfile(updated).fold(
                onSuccess = {
                    _state.value = _state.value.copy(saving = false, saved = true)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        saving = false,
                        message = "Save failed: ${e.localizedMessage}",
                        messageIsError = true
                    )
                }
            )
        }
    }

    fun enroll() {
        val s = _state.value
        val user = s.user ?: return
        val uid = authRepo.currentUserId ?: return
        val targetBatchId = if (s.joinCode.isNotBlank()) {
            val found = batchRepo.findBatchByJoinCode(s.joinCode).getOrNull()
            if (found == null) {
                _state.value = s.copy(
                    message = "No active batch found for join code \"${s.joinCode}\".",
                    messageIsError = true
                )
                return
            }
            found.id
        } else {
            s.selectedBatchId
        }

        if (targetBatchId.isBlank()) {
            _state.value = s.copy(message = "Select a batch or enter a join code.", messageIsError = true)
            return
        }

        _state.value = s.copy(saving = true, message = null)
        viewModelScope.launch {
            // Idempotent + transactional in the repository (no double counting).
            batchRepo.enrollStudentInBatch(uid, targetBatchId).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        saving = false,
                        message = "Enrolled successfully. Your batch is now ${batchRepo.getBatchById(targetBatchId).getOrNull()?.name ?: "updated"}.",
                        messageIsError = false,
                        joinCode = ""
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        saving = false,
                        message = "Enrollment failed: ${e.localizedMessage}",
                        messageIsError = true
                    )
                }
            )
        }
    }
}

@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark).imePadding()) {
        AppTopBar(title = "Edit Profile", onBack = { navController.popBackStack() })

        if (state.loading) {
            LoadingStateView()
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            EdutopiaTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Full Name",
                placeholder = "Your name"
            )
            Spacer(Modifier.height(14.dp))
            EdutopiaTextField(
                value = state.photoUrl,
                onValueChange = viewModel::onPhotoChange,
                label = "Profile photo URL (optional)",
                placeholder = "https://…",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "Class",
                color = TextWhiteSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Class 11", "Class 12", "Dropper").forEach { option ->
                    val selected = state.studentClass == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                if (selected) BrandIndigo else Color(0xFF1E293B),
                                androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.onClassChange(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            color = if (selected) Color.White else TextWhiteMuted,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            EdutopiaPrimaryButton(
                text = "Save changes",
                onClick = { viewModel.save() },
                isLoading = state.saving && !state.saved,
                enabled = !state.saving
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "Batch Enrollment",
                color = TextWhitePrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Joining a new batch moves your enrollment — batch counters stay exact.",
                color = TextWhiteMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            if (state.batches.isEmpty()) {
                Text(
                    "No active batches available right now.",
                    color = TextWhiteMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                state.batches.forEach { batch ->
                    val selected = state.selectedBatchId == batch.id
                    EdutopiaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.selectBatch(batch.id) },
                        backgroundColor = if (selected) BrandIndigo.copy(alpha = 0.14f) else Color(0xFF18233C),
                        borderColor = if (selected) BrandIndigo.copy(alpha = 0.5f) else Color(0xFF2B3C62)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(batch.name, color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${batch.targetClass} • ${batch.studentCount} students • ${batch.accessType}",
                                    color = TextWhiteMuted,
                                    fontSize = 11.sp
                                )
                            }
                            if (selected) {
                                Text("✓", color = AccentEmerald, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            EdutopiaTextField(
                value = state.joinCode,
                onValueChange = viewModel::onJoinCodeChange,
                label = "Or join with a code",
                placeholder = "e.g. EDU-4821"
            )
            Spacer(Modifier.height(10.dp))
            EdutopiaPrimaryButton(
                text = "Enroll / Switch batch",
                onClick = { viewModel.enroll() },
                enabled = !state.saving && (state.selectedBatchId.isNotBlank() || state.joinCode.isNotBlank()),
                height = 44.dp,
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(AccentEmerald, AccentCyan)
                )
            )

            if (state.message != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    state.message!!,
                    color = if (state.messageIsError) AccentRose else AccentEmerald,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
