package com.mindnova.edutopia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.AppTopBar
import com.mindnova.edutopia.core.components.DestructiveButton
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.SecondaryButton
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.data.models.AdminUser
import com.mindnova.edutopia.data.repository.AdminRepository
import com.mindnova.edutopia.data.repository.AuditLogRepository
import com.mindnova.edutopia.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Client-side gate for admin screens. NOTE: this only hides UI; the actual
 * authorization boundary is Firestore security rules (firestore.rules), so a
 * modified APK cannot write admin data without a privileged account.
 */
sealed interface AdminAuthState {
    data object Loading : AdminAuthState
    data class Authorized(val admin: AdminUser) : AdminAuthState
    data object Unauthorized : AdminAuthState
    data class Failed(val message: String) : AdminAuthState
}

class AdminSessionViewModel @JvmOverloads constructor(
    private val authRepo: AuthRepository = AuthRepository(),
    private val adminRepo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _auth = MutableStateFlow<AdminAuthState>(AdminAuthState.Loading)
    val auth: StateFlow<AdminAuthState> = _auth.asStateFlow()

    private val _snack = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snack: SharedFlow<String> = _snack.asSharedFlow()

    init {
        resolve()
    }

    fun resolve() {
        _auth.value = AdminAuthState.Loading
        viewModelScope.launch {
            val uid = authRepo.currentUserId
            if (uid == null) {
                _auth.value = AdminAuthState.Unauthorized
                return@launch
            }
            adminRepo.getAdminProfile(uid).fold(
                onSuccess = { admin ->
                    _auth.value = when {
                        admin == null -> AdminAuthState.Unauthorized
                        admin.status == "inactive" -> AdminAuthState.Failed(
                            "Your admin access has been deactivated. Contact the super admin."
                        )
                        else -> AdminAuthState.Authorized(admin)
                    }
                },
                onFailure = { e ->
                    _auth.value = AdminAuthState.Failed(
                        e.localizedMessage ?: "Could not verify your admin access."
                    )
                }
            )
        }
    }

    fun notify(message: String) {
        _snack.tryEmit(message)
    }

    suspend fun audit(
        action: String,
        targetType: String,
        targetId: String,
        details: String
    ) {
        val admin = (_auth.value as? AdminAuthState.Authorized)?.admin ?: return
        auditRepo.logAction(admin.uid, admin.email, action, targetType, targetId, details)
    }

    private val auditRepo = AuditLogRepository()
}

/**
 * Wraps admin screens: resolves admin identity first; renders gate states
 * (loading / error with retry / unauthorized) before exposing the content.
 */
@Composable
fun AdminGate(
    navController: NavController,
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (AdminAuthState.Authorized, AdminSessionViewModel) -> Unit
) {
    val session: AdminSessionViewModel = viewModel()
    val auth by session.auth.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        session.snack.collect { snackbarHostState.showSnackbar(it) }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            AppTopBar(
                title = title,
                subtitle = subtitle,
                onBack = { navController.popBackStack() },
                actions = actions
            )
            when (val a = auth) {
                AdminAuthState.Loading -> LoadingStateView("Verifying admin access…")
                AdminAuthState.Unauthorized -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = TextWhiteMuted,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Admin access required",
                            color = TextWhitePrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "You are not an admin, or your session expired. This section is also blocked by Firestore security rules.",
                            color = TextWhiteMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(18.dp))
                        SecondaryButton(
                            text = "Back to login",
                            onClick = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
                is AdminAuthState.Failed -> ErrorStateView(
                    title = "Access check failed",
                    errorMessage = a.message,
                    onRetry = { session.resolve() }
                )
                is AdminAuthState.Authorized -> content(a, session)
            }
        }
    }
}

/** Shared row used in every admin list for a consistent look. */
@Composable
fun AdminListRow(
    title: String,
    caption: String,
    trailing: String? = null,
    trailingColor: Color = TextWhiteSecondary,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    com.mindnova.edutopia.core.components.EdutopiaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF18233C),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = TextWhitePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(caption, color = TextWhiteMuted, fontSize = 11.sp, maxLines = 2)
            }
            if (trailing != null) {
                Text(
                    trailing,
                    color = trailingColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            trailingColor.copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            if (onDelete != null) {
                Spacer(Modifier.size(8.dp))
                DestructiveButton(text = "Delete", onClick = onDelete)
            }
        }
    }
}

