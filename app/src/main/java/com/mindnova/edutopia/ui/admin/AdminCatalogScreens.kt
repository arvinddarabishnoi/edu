package com.mindnova.edutopia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.ResourceView
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.DateTimeUtils
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.YouTubeUtils
import com.mindnova.edutopia.data.models.Batch
import com.mindnova.edutopia.data.models.DailyGoal
import com.mindnova.edutopia.data.models.GoalLecture
import com.mindnova.edutopia.data.models.GoalTest
import com.mindnova.edutopia.data.models.Lecture
import com.mindnova.edutopia.data.models.Series
import com.mindnova.edutopia.data.repository.BatchRepository
import com.mindnova.edutopia.data.repository.DailyGoalRepository
import com.mindnova.edutopia.data.repository.LectureRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
private fun OptionSelector(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { opt ->
            val isSelected = selected == opt
            Text(
                opt,
                color = if (isSelected) AccentCyan else TextWhiteMuted,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .background(
                        if (isSelected) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

// ══════════════════════ SERIES ══════════════════════

class AdminSeriesViewModel @JvmOverloads constructor(
    private val repo: LectureRepository = LectureRepository()
) : ViewModel() {
    val items: StateFlow<Resource<List<Series>>> = repo.getSeriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun save(s: Series): Result<String> = repo.saveSeries(s)
    suspend fun delete(id: String): Result<Unit> = repo.deleteSeries(id)
}

@Composable
fun AdminSeriesScreen(navController: NavController) {
    val viewModel: AdminSeriesViewModel = viewModel()
    val resource by viewModel.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Series?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Series?>(null) }

    AdminGate(navController = navController, title = "Series & Chapters", subtitle = "Course containers") { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                EdutopiaPrimaryButton(
                    text = "+ New series",
                    onClick = { creating = true },
                    modifier = Modifier.width(150.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "No series yet",
                emptyDescription = "Create a series so lectures can be grouped by chapter.",
                onRetry = null,
                content = { list ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val s = list[idx]
                            AdminListRow(
                                title = s.title,
                                caption = "${s.subject} • ${s.chapter} • ${s.targetClass} • ${s.lectureCount} lectures • order ${s.order}",
                                onClick = { editing = s },
                                onDelete = { deleting = s }
                            )
                        }
                    }
                }
            )
        }

        (editing ?: if (creating) Series() else null)?.let { initial ->
            var title by remember { mutableStateOf(initial.title) }
            var subject by remember { mutableStateOf(initial.subject.ifBlank { "Physics" }) }
            var chapter by remember { mutableStateOf(initial.chapter) }
            var description by remember { mutableStateOf(initial.description) }
            var targetClass by remember { mutableStateOf(initial.targetClass) }
            var order by remember { mutableStateOf(initial.order.toString()) }
            var error by remember { mutableStateOf<String?>(null) }

            AdminEditorDialog(
                title = if (initial.id.isBlank()) "New series" else "Edit series",
                onDismiss = { creating = false; editing = null },
                onSave = {
                    val o = order.trim().toIntOrNull()
                    when {
                        title.isBlank() -> error = "Title is required."
                        o == null || o < 1 -> error = "Order must be a positive number."
                        else -> scope.launch {
                            viewModel.save(
                                initial.copy(
                                    title = title.trim(),
                                    subject = subject,
                                    chapter = chapter.trim(),
                                    description = description.trim(),
                                    targetClass = targetClass,
                                    order = o
                                )
                            ).fold(
                                onSuccess = {
                                    sess.notify("Series saved")
                                    sess.audit(
                                        if (initial.id.isBlank()) "CREATE_SERIES" else "UPDATE_SERIES",
                                        "Series", it, title.trim()
                                    )
                                    creating = false; editing = null
                                },
                                onFailure = { e -> error = e.localizedMessage }
                            )
                        }
                    }
                }
            ) {
                if (error != null) Text(error!!, color = AccentRose, fontSize = 12.sp)
                AdminFormField("Title", title) { title = it; error = null }
                Text("Subject", color = TextWhiteSecondary, fontSize = 11.sp)
                OptionSelector(listOf("Physics", "Chemistry", "Mathematics"), subject) { subject = it }
                AdminFormField("Chapter", chapter) { chapter = it }
                AdminFormField("Description", description, { description = it }, singleLine = false)
                Text("Target class", color = TextWhiteSecondary, fontSize = 11.sp)
                OptionSelector(listOf("All", "Class 11", "Class 12", "Dropper"), targetClass) { targetClass = it }
                AdminFormField("Order", order) { order = it; error = null }
            }
        }

        deleting?.let { target ->
            AdminDeleteConfirm(
                itemName = target.title,
                extraWarning = "All lectures in this series will also be removed.",
                onConfirm = {
                    scope.launch {
                        viewModel.delete(target.id).fold(
                            onSuccess = {
                                sess.notify("Series and its lectures deleted")
                                sess.audit("DELETE_SERIES", "Series", target.id, target.title)
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

// ══════════════════════ LECTURES ══════════════════════

class AdminLecturesViewModel @JvmOverloads constructor(
    private val repo: LectureRepository = LectureRepository()
) : ViewModel() {
    val lectures: StateFlow<Resource<List<Lecture>>> = repo.getAllLecturesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    val series: StateFlow<Resource<List<Series>>> = repo.getSeriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun save(l: Lecture): Result<String> = repo.saveLecture(l)
    suspend fun delete(id: String): Result<Unit> = repo.deleteLecture(id)
}

@Composable
fun AdminLecturesScreen(navController: NavController) {
    val viewModel: AdminLecturesViewModel = viewModel()
    val resource by viewModel.lectures.collectAsStateWithLifecycle()
    val seriesRes by viewModel.series.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Lecture?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Lecture?>(null) }

    AdminGate(navController = navController, title = "Lectures", subtitle = "YouTube-backed video content") { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                EdutopiaPrimaryButton(
                    text = "+ New lecture",
                    onClick = { creating = true },
                    modifier = Modifier.width(150.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "No lectures yet",
                emptyDescription = "Add the first lecture to a series.",
                onRetry = null,
                content = { list ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val l = list[idx]
                            AdminListRow(
                                title = "#${l.lectureNumber} ${l.title}",
                                caption = "${l.subject} • ${l.seriesName.ifBlank { l.seriesId }} • ${l.duration} • ${l.views} views • ${l.publishStatus}",
                                trailing = if (l.publishStatus == "published") "LIVE" else "DRAFT",
                                trailingColor = if (l.publishStatus == "published") AccentEmerald else AccentAmber,
                                onClick = { editing = l },
                                onDelete = { deleting = l }
                            )
                        }
                    }
                }
            )
        }

        val seriesList = (seriesRes as? Resource.Success)?.data.orEmpty()

        (editing ?: if (creating) Lecture() else null)?.let { initial ->
            var title by remember { mutableStateOf(initial.title) }
            var videoUrl by remember { mutableStateOf(initial.videoUrl) }
            var chapter by remember { mutableStateOf(initial.chapter) }
            var description by remember { mutableStateOf(initial.description) }
            var duration by remember { mutableStateOf(initial.duration) }
            var number by remember { mutableStateOf(initial.lectureNumber.toString()) }
            var seriesId by remember { mutableStateOf(initial.seriesId) }
            var published by remember { mutableStateOf(initial.publishStatus == "published") }
            var error by remember { mutableStateOf<String?>(null) }

            val seriesOptions = listOf("") + seriesList.map { it.id }

            AdminEditorDialog(
                title = if (initial.id.isBlank()) "New lecture" else "Edit lecture",
                onDismiss = { creating = false; editing = null },
                onSave = {
                    val n = number.trim().toIntOrNull()
                    val invalidVideo = videoUrl.isNotBlank() && YouTubeUtils.extractVideoId(videoUrl).isBlank()
                    when {
                        title.isBlank() -> error = "Title is required."
                        videoUrl.isBlank() -> error = "YouTube URL (or 11-char video id) is required."
                        invalidVideo -> error = "Could not read a video id from that URL."
                        n == null || n < 1 -> error = "Lecture number must be positive."
                        seriesId.isBlank() -> error = "Choose a series for this lecture."
                        else -> scope.launch {
                            val seriesName = seriesList.firstOrNull { it.id == seriesId }?.title ?: ""
                            viewModel.save(
                                initial.copy(
                                    title = title.trim(),
                                    videoUrl = videoUrl.trim(),
                                    chapter = chapter.trim(),
                                    description = description.trim(),
                                    duration = duration.trim().ifBlank { "45 mins" },
                                    lectureNumber = n,
                                    seriesId = seriesId,
                                    seriesName = seriesName,
                                    subject = seriesList.firstOrNull { it.id == seriesId }?.subject ?: initial.subject,
                                    publishStatus = if (published) "published" else "draft"
                                )
                            ).fold(
                                onSuccess = {
                                    sess.notify(
                                        if (initial.id.isBlank()) "Lecture created (series count updated)"
                                        else "Lecture updated (count unchanged)"
                                    )
                                    sess.audit(
                                        if (initial.id.isBlank()) "ADD_LECTURE" else "UPDATE_LECTURE",
                                        "Lecture", it, title.trim()
                                    )
                                    creating = false; editing = null
                                },
                                onFailure = { e -> error = e.localizedMessage }
                            )
                        }
                    }
                }
            ) {
                if (error != null) Text(error!!, color = AccentRose, fontSize = 12.sp)
                Text("Series", color = TextWhiteSecondary, fontSize = 11.sp)
                if (seriesList.isEmpty()) {
                    Text(
                        "No series exist yet — create a series first.",
                        color = AccentAmber,
                        fontSize = 11.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        seriesList.forEach { s ->
                            val sel = seriesId == s.id
                            Text(
                                "${s.title} (${s.subject} • ${s.lectureCount} lectures)",
                                color = if (sel) AccentCyan else TextWhiteMuted,
                                fontSize = 11.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .background(
                                        if (sel) AccentCyan.copy(alpha = 0.1f) else Color(0xFF0F172A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { seriesId = s.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                AdminFormField("Title", title) { title = it; error = null }
                AdminFormField("YouTube URL or video id", videoUrl) { videoUrl = it; error = null }
                AdminFormField("Chapter", chapter) { chapter = it }
                AdminFormField("Lecture number", number) { number = it; error = null }
                AdminFormField("Duration label", duration) { duration = it }
                AdminFormField("Description", description, { description = it }, singleLine = false)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = published,
                        onCheckedChange = { published = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                            checkedColor = AccentEmerald
                        )
                    )
                    Text("Published (visible to students)", color = TextWhiteSecondary, fontSize = 12.sp)
                }
            }
        }

        deleting?.let { target ->
            AdminDeleteConfirm(
                itemName = target.title,
                extraWarning = "The series lecture count will be decremented and viewer progress for this lecture removed.",
                onConfirm = {
                    scope.launch {
                        viewModel.delete(target.id).fold(
                            onSuccess = {
                                sess.notify("Lecture deleted")
                                sess.audit("DELETE_LECTURE", "Lecture", target.id, target.title)
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

// ══════════════════════ BATCHES ══════════════════════

class AdminBatchesViewModel @JvmOverloads constructor(
    private val repo: BatchRepository = BatchRepository()
) : ViewModel() {
    val items: StateFlow<Resource<List<Batch>>> = repo.getAllBatchesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun save(b: Batch): Result<String> = repo.saveBatch(b)
    suspend fun delete(id: String): Result<Unit> = repo.deleteBatch(id)
}

@Composable
fun AdminBatchesScreen(navController: NavController) {
    val viewModel: AdminBatchesViewModel = viewModel()
    val resource by viewModel.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Batch?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Batch?>(null) }

    AdminGate(navController = navController, title = "Batches", subtitle = "Cohorts & enrollment") { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                EdutopiaPrimaryButton(
                    text = "+ New batch",
                    onClick = { creating = true },
                    modifier = Modifier.width(150.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "No batches yet",
                emptyDescription = "Students join batches by code or admin assignment.",
                onRetry = null,
                content = { list ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val b = list[idx]
                            AdminListRow(
                                title = b.name,
                                caption = "${b.targetClass} • ${b.accessType} • ${b.studentCount} students • code ${b.joinCode.ifBlank { "—" }}",
                                trailing = b.status.uppercase(),
                                trailingColor = when (b.status) {
                                    "active" -> AccentEmerald
                                    "upcoming" -> AccentAmber
                                    else -> TextWhiteMuted
                                },
                                onClick = { editing = b },
                                onDelete = { deleting = b }
                            )
                        }
                    }
                }
            )
        }

        (editing ?: if (creating) Batch() else null)?.let { initial ->
            var name by remember { mutableStateOf(initial.name) }
            var description by remember { mutableStateOf(initial.description) }
            var targetClass by remember { mutableStateOf(initial.targetClass) }
            var access by remember { mutableStateOf(initial.accessType) }
            var status by remember { mutableStateOf(initial.status) }
            var joinCode by remember { mutableStateOf(initial.joinCode) }
            var error by remember { mutableStateOf<String?>(null) }

            AdminEditorDialog(
                title = if (initial.id.isBlank()) "New batch" else "Edit batch",
                onDismiss = { creating = false; editing = null },
                onSave = {
                    when {
                        name.isBlank() -> error = "Batch name is required."
                        joinCode.isNotBlank() && joinCode.trim().length < 4 ->
                            error = "Join code must be at least 4 characters (or leave blank to auto-generate)."
                        else -> scope.launch {
                            viewModel.save(
                                initial.copy(
                                    name = name.trim(),
                                    description = description.trim(),
                                    targetClass = targetClass,
                                    accessType = access,
                                    status = status,
                                    joinCode = joinCode.trim()
                                )
                            ).fold(
                                onSuccess = {
                                    sess.notify("Batch saved")
                                    sess.audit(
                                        if (initial.id.isBlank()) "CREATE_BATCH" else "UPDATE_BATCH",
                                        "Batch", it, name.trim()
                                    )
                                    creating = false; editing = null
                                },
                                onFailure = { e -> error = e.localizedMessage }
                            )
                        }
                    }
                }
            ) {
                if (error != null) Text(error!!, color = AccentRose, fontSize = 12.sp)
                AdminFormField("Batch name", name) { name = it; error = null }
                AdminFormField("Description", description, { description = it }, singleLine = false)
                Text("Target class", color = TextWhiteSecondary, fontSize = 11.sp)
                OptionSelector(listOf("Class 11", "Class 12", "Dropper", "All"), targetClass) { targetClass = it }
                Text("Access", color = TextWhiteSecondary, fontSize = 11.sp)
                OptionSelector(listOf("Free", "Invite Only", "Premium"), access) { access = it }
                Text("Status", color = TextWhiteSecondary, fontSize = 11.sp)
                OptionSelector(listOf("active", "upcoming", "completed"), status) { status = it }
                AdminFormField("Join code (blank = auto)", joinCode) { joinCode = it.uppercase(); error = null }
                Text(
                    "Student count is maintained automatically on enrollment — never edit it here.",
                    color = TextWhiteMuted,
                    fontSize = 10.sp
                )
            }
        }

        deleting?.let { target ->
            AdminDeleteConfirm(
                itemName = target.name,
                extraWarning = "All ${target.studentCount} enrolled students will be un-assigned from this batch.",
                onConfirm = {
                    scope.launch {
                        viewModel.delete(target.id).fold(
                            onSuccess = {
                                sess.notify("Batch deleted; members un-assigned")
                                sess.audit("DELETE_BATCH", "Batch", target.id, target.name)
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

// ══════════════════════ DAILY GOALS ══════════════════════

class AdminDailyGoalsViewModel @JvmOverloads constructor(
    private val repo: DailyGoalRepository = DailyGoalRepository()
) : ViewModel() {
    val items: StateFlow<Resource<List<DailyGoal>>> = repo.getAllGoalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun save(g: DailyGoal): Result<String> = repo.saveDailyGoal(g)
    suspend fun delete(id: String): Result<Unit> = repo.deleteDailyGoal(id)
}

@Composable
fun AdminDailyGoalsScreen(navController: NavController) {
    val viewModel: AdminDailyGoalsViewModel = viewModel()
    val resource by viewModel.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<DailyGoal?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DailyGoal?>(null) }

    AdminGate(navController = navController, title = "Daily Goals", subtitle = "One lecture + one test per day") { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                EdutopiaPrimaryButton(
                    text = "+ New goal",
                    onClick = { creating = true },
                    modifier = Modifier.width(150.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "No daily goals configured",
                emptyDescription = "Students see today's goal on their home screen. Until you add one, the home hero shows nothing rather than fake content.",
                onRetry = null,
                content = { list ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val g = list[idx]
                            AdminListRow(
                                title = "Goal ${g.dateKey.ifBlank { "undated" }}",
                                caption = "Lecture: ${g.lecture.title.ifBlank { "—" }} • Test: ${g.test.title.ifBlank { "—" }}",
                                trailing = if (g.isActive) "ACTIVE" else "OFF",
                                trailingColor = if (g.isActive) AccentEmerald else TextWhiteMuted,
                                onClick = { editing = g },
                                onDelete = { deleting = g }
                            )
                        }
                    }
                }
            )
        }

        (editing ?: if (creating) DailyGoal() else null)?.let { initial ->
            var dateKey by remember { mutableStateOf(initial.dateKey.ifBlank {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
            }) }
            var lectureTitle by remember { mutableStateOf(initial.lecture.title) }
            var lectureVideoUrl by remember { mutableStateOf(initial.lecture.videoUrl) }
            var lectureSubject by remember { mutableStateOf(initial.lecture.subject.ifBlank { "Physics" }) }
            var lectureChapter by remember { mutableStateOf(initial.lecture.chapter) }
            var testId by remember { mutableStateOf(initial.test.id) }
            var testTitle by remember { mutableStateOf(initial.test.title) }
            var testDuration by remember { mutableStateOf(initial.test.durationMinutes.toString()) }
            var testStart by remember { mutableStateOf(initial.test.scheduledStartTime) }
            var active by remember { mutableStateOf(initial.isActive) }
            var error by remember { mutableStateOf<String?>(null) }

            AdminEditorDialog(
                title = if (initial.id.isBlank()) "New daily goal" else "Edit daily goal",
                onDismiss = { creating = false; editing = null },
                onSave = {
                    val dur = testDuration.trim().toIntOrNull()
                    when {
                        !Regex("""\d{4}-\d{2}-\d{2}""").matches(dateKey.trim()) ->
                            error = "Date must be yyyy-MM-dd."
                        lectureTitle.isBlank() -> error = "Lecture title is required."
                        dur == null || dur < 1 -> error = "Test duration must be positive."
                        else -> scope.launch {
                            viewModel.save(
                                initial.copy(
                                    dateKey = dateKey.trim(),
                                    lecture = GoalLecture(
                                        title = lectureTitle.trim(),
                                        subject = lectureSubject,
                                        chapter = lectureChapter.trim(),
                                        videoUrl = lectureVideoUrl.trim()
                                    ),
                                    test = GoalTest(
                                        id = testId.trim(),
                                        title = testTitle.trim(),
                                        durationMinutes = dur,
                                        scheduledStartTime = testStart.trim().ifBlank { "7:00 PM" }
                                    ),
                                    isActive = active
                                )
                            ).fold(
                                onSuccess = {
                                    sess.notify("Daily goal saved")
                                    sess.audit("SAVE_DAILY_GOAL", "DailyGoal", it, dateKey.trim())
                                    creating = false; editing = null
                                },
                                onFailure = { e -> error = e.localizedMessage }
                            )
                        }
                    }
                }
            ) {
                if (error != null) Text(error!!, color = AccentRose, fontSize = 12.sp)
                AdminFormField("Date (yyyy-MM-dd)", dateKey) { dateKey = it; error = null }
                AdminFormField("Lecture title", lectureTitle) { lectureTitle = it; error = null }
                Text("Lecture subject", color = TextWhiteSecondary, fontSize = 11.sp)
                OptionSelector(listOf("Physics", "Chemistry", "Mathematics"), lectureSubject) { lectureSubject = it }
                AdminFormField("Lecture chapter", lectureChapter) { lectureChapter = it }
                AdminFormField("Lecture YouTube URL (optional)", lectureVideoUrl) { lectureVideoUrl = it }
                AdminFormField("Linked test ID (optional)", testId) { testId = it }
                AdminFormField("Test title", testTitle) { testTitle = it }
                AdminFormField("Test duration (min)", testDuration) { testDuration = it; error = null }
                AdminFormField("Scheduled start label", testStart) { testStart = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = active,
                        onCheckedChange = { active = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                            checkedColor = AccentEmerald
                        )
                    )
                    Text("Active (today's students see this)", color = TextWhiteSecondary, fontSize = 12.sp)
                }
            }
        }

        deleting?.let { target ->
            AdminDeleteConfirm(
                itemName = "Daily goal ${target.dateKey}",
                onConfirm = {
                    scope.launch {
                        viewModel.delete(target.id).fold(
                            onSuccess = {
                                sess.notify("Goal deleted")
                                sess.audit("DELETE_DAILY_GOAL", "DailyGoal", target.id, target.dateKey)
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
