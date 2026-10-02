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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.ConfirmationDialog
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.EdutopiaGradientCard
import com.mindnova.edutopia.core.components.DestructiveButton
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.ProfileAvatar
import com.mindnova.edutopia.core.components.XpProgressBar
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.firstSettled
import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.BatchRepository
import com.mindnova.edutopia.data.repository.TestRepository
import com.mindnova.edutopia.data.repository.UserRepository
import com.mindnova.edutopia.domain.services.GamificationService
import com.mindnova.edutopia.domain.services.JeeReadiness
import com.mindnova.edutopia.domain.services.ReadinessCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val batchName: String? = null,
    val readiness: JeeReadiness? = null,
    val levelProgress: Float = 0f,
    val xpToNext: Long = 0L,
    val loading: Boolean = true,
    val error: String? = null
)

class ProfileViewModel @JvmOverloads constructor(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val testRepo: TestRepository = TestRepository(),
    private val batchRepo: BatchRepository = BatchRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ProfileUiState()
            val uid = authRepo.currentUserId
            if (uid == null) {
                _state.value = ProfileUiState(loading = false, error = "You are signed out.")
                return@launch
            }
            when (val r = userRepo.getUserFlow(uid).firstSettled()) {
                is Resource.Error -> _state.value =
                    ProfileUiState(loading = false, error = r.message)
                is Resource.Empty -> _state.value = ProfileUiState(
                    loading = false,
                    error = "Profile document not found. Please complete profile setup."
                )
                is Resource.Loading -> _state.value = ProfileUiState()
                is Resource.Success -> {
                    val user = r.data
                    val results = testRepo.getResultsForUserFlow(uid).firstSettled().let {
                        (it as? Resource.Success)?.data ?: emptyList<TestResult>()
                    }
                    val batch = if (user.batchId.isNotBlank()) {
                        batchRepo.getBatchById(user.batchId).getOrNull()?.name
                    } else null
                    val progress = GamificationService.calculateLevelProgress(user.xp)
                    _state.value = ProfileUiState(
                        user = user,
                        batchName = batch,
                        readiness = ReadinessCalculator.calculateReadiness(user, results),
                        levelProgress = progress.first,
                        xpToNext = progress.second,
                        loading = false
                    )
                }
            }
        }
    }

    fun logout() {
        authRepo.logout()
        _loggedOut.value = true
    }
}

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()

    LaunchedEffect(loggedOut) {
        if (loggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true } // clear entire stack
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (state.loading) {
            LoadingStateView()
            return@Column
        }
        if (state.error != null && state.user == null) {
            ErrorStateView(errorMessage = state.error, onRetry = { viewModel.load() })
            return@Column
        }
        val user = state.user ?: return@Column

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(name = user.name, photoUrl = user.photoUrl, size = 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user.name.ifBlank { "Student" },
                        color = TextWhitePrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        user.email,
                        color = TextWhiteSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        "${user.studentClass} • Level ${user.level}",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { navController.navigate(Screen.EditProfile.route) }
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit profile",
                        tint = TextWhiteSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(Modifier.height(16.dp))

            EdutopiaGradientCard(
                modifier = Modifier.fillMaxWidth(),
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Level ${user.level}",
                            color = TextWhitePrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${user.xp} XP total",
                            color = TextWhiteSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    XpProgressBar(progress = state.levelProgress)
                    Text(
                        "${state.xpToNext} XP to Level ${user.level + 1}",
                        color = TextWhiteMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${user.testsCompleted}", "Tests", Modifier.weight(1f))
                StatTile("${user.lecturesWatched}", "Lectures", Modifier.weight(1f))
                StatTile("${user.pyqsSolved}", "PYQs", Modifier.weight(1f))
                StatTile("${user.streak}", "Streak", Modifier.weight(1f), valueColor = AccentAmber)
            }

            Spacer(Modifier.height(16.dp))
            EdutopiaCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Batch", color = TextWhiteSecondary, fontSize = 13.sp)
                        Text(
                            state.batchName ?: "Not enrolled",
                            color = if (state.batchName != null) TextWhitePrimary else AccentAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Points", color = TextWhiteSecondary, fontSize = 13.sp)
                        Text("${user.points} pts", color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Readiness (estimated insight)", color = TextWhiteSecondary, fontSize = 13.sp)
                        Text(
                            state.readiness?.let {
                                if (it.hasEnoughData) "${it.readinessPercentage}%" else "Not enough data"
                            } ?: "—",
                            color = AccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            DestructiveButton(
                text = "Log out",
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(32.dp))
        }

        if (showLogoutConfirm) {
            ConfirmationDialog(
                title = "Log out?",
                message = "You'll need to sign in again to continue your preparation. Your progress stays saved.",
                confirmLabel = "Log out",
                destructive = true,
                onConfirm = {
                    showLogoutConfirm = false
                    viewModel.logout()
                },
                onDismiss = { showLogoutConfirm = false }
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextWhitePrimary
) {
    EdutopiaCard(modifier = modifier, backgroundColor = Color(0xFF18233C)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextWhiteMuted, fontSize = 10.sp)
        }
    }
}
