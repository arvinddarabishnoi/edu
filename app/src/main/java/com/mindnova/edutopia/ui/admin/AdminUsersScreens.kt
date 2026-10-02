package com.mindnova.edutopia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.AppTopBar
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.ProfileAvatar
import com.mindnova.edutopia.core.components.ResourceView
import com.mindnova.edutopia.core.components.SecondaryButton
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.DateTimeUtils
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.AdminUser
import com.mindnova.edutopia.data.models.AuditLog
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.data.repository.AdminRepository
import com.mindnova.edutopia.data.repository.AuditLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ══════════════════════ USERS ══════════════════════

data class AdminUsersUiState(
    val users: List<User> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false
)

class AdminUsersViewModel @JvmOverloads constructor(
    private val adminRepo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<AdminUsersUiState> = combine(
        adminRepo.getAllUsersFlow(),
        query
    ) { resource, q ->
        when (resource) {
            is Resource.Loading -> AdminUsersUiState(query = q)
            is Resource.Error -> AdminUsersUiState(query = q, loading = false, error = resource.message)
            is Resource.Empty -> AdminUsersUiState(query = q, loading = false, isEmpty = true)
            is Resource.Success -> {
                val filtered = resource.data.filter {
                    q.isBlank() ||
                        it.name.contains(q, ignoreCase = true) ||
                        it.email.contains(q, ignoreCase = true) ||
                        it.uid == q
                }
                AdminUsersUiState(users = filtered, query = q, loading = false, isEmpty = filtered.isEmpty())
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUsersUiState())

    fun setQuery(value: String) { query.value = value }

    suspend fun promote(uid: String, toAdmin: Boolean, isSuper: Boolean): Result<Unit> =
        adminRepo.setUserRole(uid, if (toAdmin) Constants.ROLE_ADMIN else Constants.ROLE_STUDENT, isSuper)
}

@Composable
fun AdminUsersScreen(navController: NavController) {
    val viewModel: AdminUsersViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var targetUser by remember { mutableStateOf<User?>(null) }

    AdminGate(navController = navController, title = "Users", subtitle = "Student & staff directory") { authorized, sess ->
        val isSuper = authorized.admin.role == Constants.ROLE_SUPER_ADMIN
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            AdminCrudList(
                resource = if (state.error != null) Resource.Error(state.error!!)
                else if (state.loading) Resource.Loading
                else if (state.isEmpty) Resource.Empty()
                else Resource.Success(state.users),
                searchHint = "Search by name, email or UID",
                query = state.query,
                onQueryChange = viewModel::setQuery,
                emptyTitle = "No matching users",
                emptyDescription = "Students appear here once they register.",
                rowContent = { user ->
                    UserRow(
                        user = user,
                        canManageRoles = isSuper,
                        onPromote = { targetUser = user }
                    )
                }
            )
        }

        targetUser?.let { u ->
            AdminEditorDialog(
                title = "Manage role — ${u.name.ifBlank { u.email }}",
                onDismiss = { targetUser = null },
                onSave = { targetUser = null },
                saveLabel = "Close",
                saveEnabled = true
            ) {
                Text(
                    "Current role: ${u.role}",
                    color = TextWhiteSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = if (u.role == Constants.ROLE_STUDENT) "Make Admin" else "Demote to Student",
                        onClick = {
                            scope.launch {
                                viewModel.promote(u.uid, u.role == Constants.ROLE_STUDENT, isSuper)
                                    .onSuccess {
                                        sess.notify("Role updated for ${u.name.ifBlank { u.email }}")
                                        sess.audit(
                                            if (u.role == Constants.ROLE_STUDENT) "GRANT_ADMIN" else "REVOKE_ADMIN",
                                            "User", u.uid, "Role changed for ${u.email}"
                                        )
                                    }
                                    .onFailure { e -> sess.notify("Failed: ${e.localizedMessage}") }
                            }
                            targetUser = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


@Composable
private fun UserRow(user: User, canManageRoles: Boolean, onPromote: () -> Unit) {
    EdutopiaCard(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF18233C)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(name = user.name, photoUrl = user.photoUrl, size = 40.dp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.name.ifBlank { "Unnamed" },
                        color = TextWhitePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    if (user.role != Constants.ROLE_STUDENT) {
                        Spacer(Modifier.size(6.dp))
                        Text(
                            user.role,
                            color = AccentAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    AccentAmber.copy(alpha = 0.14f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    user.email.ifBlank { "no email" },
                    color = TextWhiteMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Text(
                    "${user.studentClass.ifBlank { "—" }} • ${user.xp} XP • active ${if (user.lastActiveAt > 0) DateTimeUtils.formatDate(user.lastActiveAt) else "never"}",
                    color = TextWhiteSecondary,
                    fontSize = 10.sp
                )
            }
            if (canManageRoles) {
                Text(
                    "Roles",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(BrandIndigo.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable { onPromote() }
                )
            }
        }
    }
}

// ══════════════════════ SUPER ADMIN MANAGEMENT ══════════════════════

data class AdminManagementUiState(
    val admins: List<AdminUser> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false
)

class AdminManagementViewModel @JvmOverloads constructor(
    private val adminRepo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AdminManagementUiState())
    val uiState: StateFlow<AdminManagementUiState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            adminRepo.getAllAdminsFlow().collect { r ->
                _state.value = when (r) {
                    is Resource.Loading -> AdminManagementUiState()
                    is Resource.Error -> AdminManagementUiState(loading = false, error = r.message)
                    is Resource.Empty -> AdminManagementUiState(loading = false, isEmpty = true)
                    is Resource.Success -> AdminManagementUiState(admins = r.data, loading = false)
                }
            }
        }
    }

    suspend fun save(admin: AdminUser, actorUid: String, actorIsSuper: Boolean): Result<Unit> =
        adminRepo.saveAdmin(admin, actorUid, actorIsSuper)

    suspend fun remove(adminId: String, actorIsSuper: Boolean): Result<Unit> =
        adminRepo.removeAdmin(adminId, actorIsSuper)
}

@Composable
fun AdminSuperManagementScreen(navController: NavController) {
    val viewModel: AdminManagementViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    AdminGate(navController = navController, title = "Admin Management", subtitle = "Create and revoke staff access") { authorized, sess ->
        val isSuper = authorized.admin.role == Constants.ROLE_SUPER_ADMIN
        var editing by remember { mutableStateOf<AdminUser?>(null) }
        var creating by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isSuper) "Super admins can grant/revoke access" else "Only a super admin can change access",
                    color = TextWhiteMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isSuper) {
                    com.mindnova.edutopia.core.components.EdutopiaPrimaryButton(
                        text = "+ Invite",
                        onClick = { creating = true },
                        modifier = Modifier.size(width = 110.dp, height = 38.dp)
                    )
                }
            }

            ResourceView(
                resource = if (state.error != null) Resource.Error(state.error!!)
                else if (state.loading) Resource.Loading
                else if (state.admins.isEmpty()) Resource.Empty()
                else Resource.Success(state.admins),
                emptyTitle = "No extra admins yet",
                emptyDescription = "The super admin is configured via Firestore directly; invite staff members here.",
                onRetry = null,
                content = { admins ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(admins.size) { idx ->
                            val admin = admins[idx]
                            AdminListRow(
                                title = admin.name.ifBlank { admin.email },
                                caption = "${admin.email} • role ${admin.role} • ${admin.permissions.size} permissions",
                                trailing = admin.status,
                                trailingColor = if (admin.status == "active") AccentEmerald else AccentRose,
                                onClick = { if (isSuper) editing = admin },
                                onDelete = if (isSuper && admin.role != Constants.ROLE_SUPER_ADMIN) {
                                    {
                                        scope.launch {
                                            viewModel.remove(admin.uid, isSuper)
                                                .onSuccess {
                                                    sess.notify("Admin access revoked")
                                                    sess.audit("REVOKE_ADMIN", "Admin", admin.uid, "Removed admin ${admin.email}")
                                                }
                                                .onFailure { e -> sess.notify("Failed: ${e.localizedMessage}") }
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }
            )
        }

        if (creating || editing != null) {
            val initial = editing ?: AdminUser(
                uid = "", name = "", email = "", role = Constants.ROLE_ADMIN,
                permissions = AdminRepository.ALL_PERMISSIONS.take(6).toList()
            )
            var name by remember { mutableStateOf(initial.name) }
            var email by remember { mutableStateOf(initial.email) }
            var uid by remember { mutableStateOf(initial.uid) }
            var isSuper by remember { mutableStateOf(initial.role == Constants.ROLE_SUPER_ADMIN) }
            var perms by remember { mutableStateOf(initial.permissions.toSet()) }
            var formError by remember { mutableStateOf<String?>(null) }

            AdminEditorDialog(
                title = if (editing == null) "Invite / promote admin" else "Edit admin",
                onDismiss = { creating = false; editing = null },
                onSave = {
                    when {
                        uid.isBlank() -> formError = "The admin UID must be the Firebase UID of an existing account."
                        email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                            formError = "Enter a valid email or leave it blank."
                        perms.isEmpty() -> formError = "Grant at least one permission."
                        else -> {
                            val admin = initial.copy(
                                uid = uid.trim(),
                                name = name.trim(),
                                email = email.trim().lowercase(),
                                role = if (isSuper) Constants.ROLE_SUPER_ADMIN else Constants.ROLE_ADMIN,
                                permissions = perms.toList(),
                                status = "active"
                            )
                            scope.launch {
                                viewModel.save(admin, authorized.admin.uid, isSuper)
                                    .onSuccess {
                                        sess.notify("Admin saved")
                                        sess.audit(
                                            if (editing == null) "CREATE_ADMIN" else "UPDATE_ADMIN",
                                            "Admin", admin.uid,
                                            "Role ${admin.role}, ${admin.permissions.size} permissions"
                                        )
                                        creating = false; editing = null
                                    }
                                    .onFailure { e -> formError = e.localizedMessage ?: "Save failed" }
                            }
                        }
                    }
                }
            ) {
                Text("Admin UID (must match their signed-in account)", color = TextWhiteSecondary, fontSize = 11.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = uid,
                    onValueChange = { uid = it; formError = null },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextWhitePrimary, fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(BrandIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Name", color = TextWhiteSecondary, fontSize = 11.sp)
                        androidx.compose.foundation.text.BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextWhitePrimary, fontSize = 14.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(BrandIndigo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Email", color = TextWhiteSecondary, fontSize = 11.sp)
                        androidx.compose.foundation.text.BasicTextField(
                            value = email,
                            onValueChange = { email = it; formError = null },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextWhitePrimary, fontSize = 14.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(BrandIndigo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = isSuper,
                        onCheckedChange = { isSuper = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                            checkedColor = AccentAmber
                        )
                    )
                    Text("Grant SUPER ADMIN (can manage other admins)", color = TextWhiteSecondary, fontSize = 12.sp)
                }
                Text("Permissions", color = TextWhiteSecondary, fontSize = 11.sp)
                AdminRepository.ALL_PERMISSIONS.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { key ->
                            val selected = key in perms
                            Text(
                                key,
                                color = if (selected) AccentCyan else TextWhiteMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        perms = if (selected) perms - key else perms + key
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                if (formError != null) {
                    Text(formError!!, color = AccentRose, fontSize = 12.sp)
                }
                Text(
                    "Client-side gate is UI only — Firestore rules block non-super-admin role writes.",
                    color = TextWhiteMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ══════════════════════ AUDIT LOGS ══════════════════════

class AdminAuditLogsViewModel @JvmOverloads constructor(
    private val repo: AuditLogRepository = AuditLogRepository()
) : ViewModel() {
    val logs: StateFlow<Resource<List<AuditLog>>> = repo.getAuditLogsFlow(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)
}

@Composable
fun AdminAuditLogsScreen(navController: NavController) {
    val viewModel: AdminAuditLogsViewModel = viewModel()
    val resource by viewModel.logs.collectAsStateWithLifecycle()

    AdminGate(navController = navController, title = "Audit Logs", subtitle = "Sensitive admin actions") { _, _ ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            ResourceView(
                resource = resource,
                emptyTitle = "No admin actions recorded yet",
                emptyDescription = "Creates, updates and deletions by staff are logged here automatically.",
                onRetry = null,
                content = { items ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items.size) { idx ->
                            val log = items[idx]
                            EdutopiaCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0xFF18233C)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.History,
                                        null,
                                        tint = TextWhiteMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.size(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            log.action.replace("_", " "),
                                            color = AccentCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${log.targetType} ${log.targetId} — ${log.details}",
                                            color = TextWhiteSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 2
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            DateTimeUtils.formatDateTime(log.timestamp),
                                            color = TextWhiteMuted,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            log.adminEmail,
                                            color = TextWhiteMuted,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
