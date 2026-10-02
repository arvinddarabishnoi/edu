package com.mindnova.edutopia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.ResourceView
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.DateTimeUtils
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.Announcement
import com.mindnova.edutopia.data.models.BannerItem
import com.mindnova.edutopia.data.models.InAppMessage
import com.mindnova.edutopia.data.repository.AnnouncementRepository
import com.mindnova.edutopia.data.repository.BannerRepository
import com.mindnova.edutopia.data.repository.MessageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun AdminFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    Column {
        Text(label, color = TextWhiteSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 5,
            textStyle = TextStyle(color = TextWhitePrimary, fontSize = 14.sp),
            cursorBrush = SolidColor(BrandIndigo),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                .padding(12.dp)
        )
    }
}

// ══════════════════════ BANNERS ══════════════════════

class AdminBannersViewModel @JvmOverloads constructor(
    private val repo: BannerRepository = BannerRepository()
) : ViewModel() {
    val items: StateFlow<Resource<List<BannerItem>>> = repo.getAllBannersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun save(banner: BannerItem): Result<String> = repo.saveBanner(banner)
    suspend fun delete(id: String): Result<Unit> = repo.deleteBanner(id)
}

@Composable
fun AdminBannersScreen(navController: NavController) {
    val viewModel: AdminBannersViewModel = viewModel()
    val resource by viewModel.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<BannerItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<BannerItem?>(null) }

    AdminGate(navController = navController, title = "Banners", subtitle = "Home-screen promotional banners") { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                com.mindnova.edutopia.core.components.EdutopiaPrimaryButton(
                    text = "+ New banner",
                    onClick = { creating = true },
                    modifier = Modifier.width(150.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "No banners yet",
                emptyDescription = "Banners appear in the student home carousel.",
                onRetry = null,
                content = { list ->
                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val b = list[idx]
                            AdminListRow(
                                title = b.title,
                                caption = "priority ${b.priority} • ${DateTimeUtils.formatDate(b.startDate)} → ${if (b.endDate == Long.MAX_VALUE) "open-ended" else DateTimeUtils.formatDate(b.endDate)}",
                                trailing = if (b.active) "ACTIVE" else "OFF",
                                trailingColor = if (b.active) AccentEmerald else TextWhiteMuted,
                                onClick = { editing = b },
                                onDelete = { deleting = b }
                            )
                        }
                    }
                }
            )
        }

        val inDialog = editing ?: if (creating) BannerItem() else null
        inDialog?.let { initial ->
            BannerEditor(
                initial = initial,
                onDismiss = { creating = false; editing = null },
                onSave = { updated ->
                    scope.launch {
                        viewModel.save(updated).fold(
                            onSuccess = {
                                sess.notify(if (initial.id.isBlank()) "Banner created" else "Banner updated")
                                sess.audit(
                                    if (initial.id.isBlank()) "CREATE_BANNER" else "UPDATE_BANNER",
                                    "Banner", it, updated.title
                                )
                                creating = false; editing = null
                            },
                            onFailure = { e -> sess.notify("Save failed: ${e.localizedMessage}") }
                        )
                    }
                }
            )
        }

        deleting?.let { target ->
            AdminDeleteConfirm(
                itemName = target.title,
                onConfirm = {
                    scope.launch {
                        viewModel.delete(target.id).fold(
                            onSuccess = {
                                sess.notify("Banner deleted")
                                sess.audit("DELETE_BANNER", "Banner", target.id, target.title)
                            },
                            onFailure = { e -> sess.notify("Delete failed: ${e.localizedMessage}") }
                        )
                        deleting = null
                    }
                },
                onDismiss = { deleting = null }
            )
        }
    }
}

@Composable
private fun BannerEditor(
    initial: BannerItem,
    onDismiss: () -> Unit,
    onSave: (BannerItem) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var description by remember { mutableStateOf(initial.description) }
    var imageUrl by remember { mutableStateOf(initial.imageUrl) }
    var clickUrl by remember { mutableStateOf(initial.clickUrl) }
    var priority by remember { mutableStateOf(initial.priority.toString()) }
    var active by remember { mutableStateOf(initial.active) }
    var error by remember { mutableStateOf<String?>(null) }

    AdminEditorDialog(
        title = if (initial.id.isBlank()) "Create banner" else "Edit banner",
        onDismiss = onDismiss,
        onSave = {
            val p = priority.trim().toIntOrNull()
            when {
                title.isBlank() -> error = "Title is required."
                imageUrl.isBlank() -> error = "Image URL is required."
                p == null || p < 1 || p > 100 -> error = "Priority must be a number between 1 and 100."
                else -> onSave(
                    initial.copy(
                        title = title.trim(),
                        description = description.trim(),
                        imageUrl = imageUrl.trim(),
                        clickUrl = clickUrl.trim(),
                        priority = p,
                        active = active
                    )
                )
            }
        }
    ) {
        if (error != null) Text(error!!, color = AccentRose, fontSize = 12.sp)
        AdminFormField("Title", title) { title = it; error = null }
        AdminFormField("Description", description, { description = it }, singleLine = false)
        AdminFormField("Image URL (https)", imageUrl) { imageUrl = it }
        AdminFormField("Click-through URL (optional)", clickUrl) { clickUrl = it }
        AdminFormField("Priority (1-100)", priority) { priority = it; error = null }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = active,
                onCheckedChange = { active = it },
                colors = CheckboxDefaults.colors(checkedColor = BrandIndigo)
            )
            Text("Active", color = TextWhiteSecondary, fontSize = 13.sp)
        }
    }
}

// ══════════════════════ ANNOUNCEMENTS ══════════════════════

class AdminAnnouncementsViewModel @JvmOverloads constructor(
    private val repo: AnnouncementRepository = AnnouncementRepository()
) : ViewModel() {
    val items: StateFlow<Resource<List<Announcement>>> = repo.getAnnouncementsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun save(a: Announcement): Result<String> = repo.createAnnouncement(a)
    suspend fun delete(id: String): Result<Unit> = repo.deleteAnnouncement(id)
}

@Composable
fun AnnouncementsAdminScreen(navController: NavController) {
    val viewModel: AdminAnnouncementsViewModel = viewModel()
    val resource by viewModel.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Announcement?>(null) }

    AdminGate(navController = navController, title = "Announcements", subtitle = "Broadcast updates to students") { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                com.mindnova.edutopia.core.components.EdutopiaPrimaryButton(
                    text = "+ New announcement",
                    onClick = { creating = true },
                    modifier = Modifier.width(190.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "No announcements yet",
                emptyDescription = "Published announcements show on the student home screen.",
                onRetry = null,
                content = { list ->
                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val a = list[idx]
                            AdminListRow(
                                title = a.title,
                                caption = "${a.targetAudience} • ${a.priority} • ${DateTimeUtils.formatDateTime(a.createdAt)}",
                                trailing = a.priority.uppercase(),
                                trailingColor = when (a.priority) {
                                    "high" -> AccentRose
                                    "low" -> TextWhiteMuted
                                    else -> AccentCyan
                                },
                                onDelete = { deleting = a }
                            )
                        }
                    }
                }
            )
        }

        if (creating) {
            AnnouncementEditor(
                onDismiss = { creating = false },
                onSave = { a ->
                    scope.launch {
                        viewModel.save(a).fold(
                            onSuccess = {
                                sess.notify("Announcement published")
                                sess.audit("CREATE_ANNOUNCEMENT", "Announcement", it, a.title)
                                creating = false
                            },
                            onFailure = { e -> sess.notify("Failed: ${e.localizedMessage}") }
                        )
                    }
                }
            )
        }

        deleting?.let { target ->
            AdminDeleteConfirm(
                itemName = target.title,
                onConfirm = {
                    scope.launch {
                        viewModel.delete(target.id).fold(
                            onSuccess = {
                                sess.notify("Announcement deleted")
                                sess.audit("DELETE_ANNOUNCEMENT", "Announcement", target.id, target.title)
                            },
                            onFailure = { e -> sess.notify("Failed: ${e.localizedMessage}") }
                        )
                        deleting = null
                    }
                },
                onDismiss = { deleting = null }
            )
        }
    }
}

@Composable
private fun AnnouncementEditor(onDismiss: () -> Unit, onSave: (Announcement) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("all") }
    var priority by remember { mutableStateOf("normal") }
    var error by remember { mutableStateOf<String?>(null) }

    AdminEditorDialog(
        title = "New announcement",
        onDismiss = onDismiss,
        onSave = {
            when {
                title.isBlank() -> error = "Title is required."
                description.isBlank() -> error = "Message body is required."
                else -> onSave(
                    Announcement(
                        title = title.trim(),
                        description = description.trim(),
                        targetAudience = audience,
                        priority = priority,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    ) {
        if (error != null) Text(error!!, color = AccentRose, fontSize = 12.sp)
        AdminFormField("Title", title) { title = it; error = null }
        AdminFormField("Message", description, { description = it }, singleLine = false)
        Column {
            Text("Audience", color = TextWhiteSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all", "Class 11", "Class 12", "Dropper").forEach { opt ->
                    val selected = audience == opt
                    Text(
                        opt,
                        color = if (selected) AccentCyan else TextWhiteMuted,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .background(
                                if (selected) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { audience = opt }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Column {
            Text("Priority", color = TextWhiteSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("high" to AccentRose, "normal" to AccentCyan, "low" to TextWhiteMuted).forEach { (opt, c) ->
                    val selected = priority == opt
                    Text(
                        opt,
                        color = if (selected) c else TextWhiteMuted,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .background(
                                if (selected) c.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { priority = opt }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════ MESSAGING ══════════════════════

class AdminMessagingViewModel @JvmOverloads constructor(
    private val repo: MessageRepository = MessageRepository()
) : ViewModel() {
    val items: StateFlow<Resource<List<InAppMessage>>> = repo.getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun send(m: InAppMessage): Result<String> = repo.sendMessage(m)
}

@Composable
fun AdminMessagingScreen(navController: NavController) {
    val viewModel: AdminMessagingViewModel = viewModel()
    val resource by viewModel.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var composerOpen by remember { mutableStateOf(false) }

    AdminGate(navController = navController, title = "In-App Messaging", subtitle = "Targeted broadcasts") { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                com.mindnova.edutopia.core.components.EdutopiaPrimaryButton(
                    text = "Compose message",
                    onClick = { composerOpen = true },
                    modifier = Modifier.width(170.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "No messages sent yet",
                emptyDescription = "Messages reach students matching the chosen target.",
                onRetry = null,
                content = { list ->
                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val m = list[idx]
                            EdutopiaCard(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF18233C)) {
                                Column {
                                    Text(m.title, color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(m.message, color = TextWhiteSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                                    Text(
                                        "to ${m.targetType}:${m.targetValue.ifBlank { "—" }} • ${DateTimeUtils.formatDateTime(m.createdAt)}",
                                        color = TextWhiteMuted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        if (composerOpen) {
            MessageComposer(
                onDismiss = { composerOpen = false },
                onSend = { msg ->
                    scope.launch {
                        viewModel.send(msg).fold(
                            onSuccess = {
                                sess.notify("Message sent")
                                sess.audit("SEND_MESSAGE", "Message", it, "${msg.targetType}:${msg.targetValue}")
                                composerOpen = false
                            },
                            onFailure = { e -> sess.notify("Send failed: ${e.localizedMessage}") }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun MessageComposer(onDismiss: () -> Unit, onSend: (InAppMessage) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf("all") }
    var targetValue by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AdminEditorDialog(
        title = "Compose message",
        onDismiss = onDismiss,
        onSave = {
            when {
                title.isBlank() -> error = "Title required."
                body.isBlank() -> error = "Message body required."
                targetType != "all" && targetValue.isBlank() -> error = "Target value required for ${targetType}."
                else -> onSend(
                    InAppMessage(
                        title = title.trim(),
                        message = body.trim(),
                        targetType = targetType,
                        targetValue = targetValue.trim(),
                        senderName = "MINDNOVA Admin"
                    )
                )
            }
        }
    ) {
        if (error != null) Text(error!!, color = AccentRose, fontSize = 12.sp)
        AdminFormField("Title", title) { title = it; error = null }
        AdminFormField("Message", body, { body = it }, singleLine = false)
        Column {
            Text("Target", color = TextWhiteSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all", "class", "batch", "user").forEach { opt ->
                    val selected = targetType == opt
                    Text(
                        opt,
                        color = if (selected) AccentCyan else TextWhiteMuted,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .background(
                                if (selected) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { targetType = opt }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        if (targetType != "all") {
            AdminFormField(
                label = when (targetType) {
                    "class" -> "Class (e.g. Class 12)"
                    "batch" -> "Batch ID"
                    else -> "User UID"
                },
                value = targetValue,
                onValueChange = { targetValue = it; error = null }
            )
        }
    }
}
