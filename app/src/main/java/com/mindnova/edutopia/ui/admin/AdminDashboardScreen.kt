package com.mindnova.edutopia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.PlayLesson
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsTournament
import androidx.compose.material.icons.filled.Task
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.ConfirmationDialog
import com.mindnova.edutopia.core.components.EdutopiaGradientCard
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.data.repository.AdminRepository
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.AdminDashboardMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminMetricsUiState(
    val metrics: AdminDashboardMetrics? = null,
    val loading: Boolean = true,
    val error: String? = null
)

class AdminDashboardViewModel @JvmOverloads constructor(
    private val adminRepo: AdminRepository = AdminRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AdminMetricsUiState())
    val state: StateFlow<AdminMetricsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            adminRepo.getDashboardMetrics().fold(
                onSuccess = { _state.value = AdminMetricsUiState(metrics = it, loading = false) },
                onFailure = { e ->
                    _state.value = AdminMetricsUiState(
                        loading = false,
                        error = e.localizedMessage ?: "Failed to load dashboard metrics."
                    )
                }
            )
        }
    }

    fun logout(onDone: () -> Unit) {
        authRepo.logout()
        onDone()
    }
}

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminDashboardViewModel = viewModel()
) {
    val session: AdminSessionViewModel = viewModel()
    val auth by session.auth.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLogout by remember { mutableStateOf(false) }

    AdminGate(
        navController = navController,
        title = "Admin Console",
        subtitle = "EDUTOPIA · MINDNOVA"
    ) { authorized, _ ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Welcome, ${authorized.admin.name.ifBlank { authorized.admin.email }}",
                        color = TextWhitePrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Role: ${authorized.admin.role} • ${authorized.admin.permissions.size} permissions",
                        color = TextWhiteMuted,
                        fontSize = 12.sp
                    )
                }

                // Metrics — real counts; 0 means 0 (no fudge factors)
                item {
                    when {
                        state.loading -> LoadingStateView("Loading live metrics…")
                        state.error != null -> ErrorStateView(
                            title = "Metrics unavailable",
                            errorMessage = state.error,
                            onRetry = { viewModel.load() }
                        )
                        else -> {
                            val m = state.metrics!!
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Live platform metrics",
                                    color = TextWhiteSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MetricCard("Students", m.totalStudents, BrandIndigo, Icons.Default.School, Modifier.weight(1f))
                                    MetricCard("Active today", m.activeToday, AccentEmerald, Icons.Default.InsertChart, Modifier.weight(1f))
                                    MetricCard("Admins", m.totalAdmins, AccentRose, Icons.Default.Shield, Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MetricCard("Tests", m.totalTests, AccentCyan, Icons.Default.Quiz, Modifier.weight(1f))
                                    MetricCard("Attempts", m.testsCompleted, AccentAmber, Icons.Default.Task, Modifier.weight(1f))
                                    MetricCard("Lectures", m.totalLectures, BrandIndigo, Icons.Default.PlayLesson, Modifier.weight(1f))
                                    MetricCard("Batches", m.totalBatches, AccentEmerald, Icons.Default.Groups, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Management",
                        color = TextWhiteSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                val sections = listOf(
                    Triple("Users & Roles", Screen.AdminUsers.route, Icons.Default.AccountTree),
                    Triple("Batches", Screen.AdminBatches.route, Icons.Default.Groups),
                    Triple("Series & Lectures", Screen.AdminSeries.route, Icons.Default.Category),
                    Triple("Tests & Questions", Screen.AdminTests.route, Icons.Default.Assignment),
                    Triple("PYQ Bank", Screen.AdminPyq.route, Icons.Default.Quiz),
                    Triple("Daily Goals", Screen.AdminDailyGoals.route, Icons.Default.Task),
                    Triple("Tournaments", Screen.AdminTournaments.route, Icons.Default.SportsTournament),
                    Triple("Banners", Screen.AdminBanners.route, Icons.Default.Image),
                    Triple("Announcements", Screen.AdminAnnouncements.route, Icons.Default.Campaign),
                    Triple("Messaging", Screen.AdminMessaging.route, Icons.Default.Chat),
                    Triple("Super Admin", Screen.AdminSuperManagement.route, Icons.Default.Shield),
                    Triple("Audit Logs", Screen.AdminAuditLogs.route, Icons.Default.History)
                )
                items(sections.size) { idx ->
                    val (label, route, icon) = sections[idx]
                    AdminSectionTile(label = label, icon = icon) {
                        navController.navigate(route)
                    }
                }

                item {
                    com.mindnova.edutopia.core.components.DestructiveButton(
                        text = "Log out of admin",
                        onClick = { showLogout = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    if (showLogout) {
        ConfirmationDialog(
            title = "Log out?",
            message = "You'll return to the sign-in screen.",
            confirmLabel = "Log out",
            destructive = true,
            onConfirm = {
                showLogout = false
                viewModel.logout {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            },
            onDismiss = { showLogout = false }
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: Long,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    EdutopiaGradientCard(
        modifier = modifier,
        gradientBrush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(color.copy(alpha = 0.16f), Color(0xFF0F172A))
        ),
        borderColor = color.copy(alpha = 0.3f)
    ) {
        Column {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(8.dp))
            Text("$value", color = TextWhitePrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextWhiteMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AdminSectionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF18233C))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(12.dp))
            Text(
                label,
                color = TextWhitePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text("›", color = TextWhiteMuted, fontSize = 18.sp)
        }
    }
}
