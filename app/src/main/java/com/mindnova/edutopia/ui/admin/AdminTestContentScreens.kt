package com.mindnova.edutopia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.AppTopBar
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.ResourceView
import com.mindnova.edutopia.core.components.SecondaryButton
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.SurfaceBorderDark
import com.mindnova.edutopia.core.theme.SurfaceDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.PyqQuestion
import com.mindnova.edutopia.data.models.Question
import com.mindnova.edutopia.data.models.TestModel
import com.mindnova.edutopia.data.models.Tournament
import com.mindnova.edutopia.data.repository.PyqRepository
import com.mindnova.edutopia.data.repository.TestRepository
import com.mindnova.edutopia.data.repository.TournamentRepository
import com.mindnova.edutopia.domain.services.JsonQuestionValidator
import com.mindnova.edutopia.domain.services.JsonValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ══════════════════════ TESTS ══════════════════════

class AdminTestsViewModel @JvmOverloads constructor(
    private val repo: TestRepository = TestRepository()
) : ViewModel() {
    val items: StateFlow<Resource<List<TestModel>>> = repo.getAllTestsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun save(t: TestModel): Result<String> = repo.saveTest(t)
    suspend fun delete(id: String): Result<Unit> = repo.deleteTest(id)
}

@Composable
fun AdminTestsScreen(navController: NavController) {
    val viewModel: AdminTestsViewModel = viewModel()
    val resource by viewModel.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<TestModel?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TestModel?>(null) }

    AdminGate(
        navController = navController,
        title = "Tests & AITS",
        subtitle = "Papers, questions and imports"
    ) { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                EdutopiaPrimaryButton(
                    text = "+ New test",
                    onClick = { creating = true },
                    modifier = Modifier.width(150.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "No tests created yet",
                emptyDescription = "Create a test, then add questions manually or import JSON.",
                onRetry = null,
                content = { list ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val t = list[idx]
                            EdutopiaCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0xFF18233C),
                                onClick = { editing = t }
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            t.title,
                                            color = TextWhitePrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        Text(
                                            t.status.uppercase(),
                                            color = when (t.status) {
                                                "published" -> AccentEmerald
                                                "draft" -> AccentAmber
                                                else -> TextWhiteMuted
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${t.category} • ${t.subject} • ${t.totalQuestions} Qs • ${t.durationMinutes} min • +${t.marksPerQuestion}/-${t.negativeMarks}",
                                        color = TextWhiteMuted,
                                        fontSize = 11.sp
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MiniAction("Questions") {
                                            navController.navigate(Screen.AdminQuestionEditor.createRoute(t.id))
                                        }
                                        MiniAction("Import JSON") {
                                            navController.navigate(Screen.AdminJsonImporter.createRoute(t.id))
                                        }
                                        MiniAction("Delete", danger = true) { deleting = t }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        (editing ?: if (creating) TestModel() else null)?.let { initial ->
            var title by remember { mutableStateOf(initial.title) }
            var description by remember { mutableStateOf(initial.description) }
            var category by remember { mutableStateOf(initial.category) }
            var subject by remember { mutableStateOf(initial.subject) }
            var duration by remember { mutableStateOf(initial.durationMinutes.toString()) }
            var marks by remember { mutableStateOf(initial.marksPerQuestion.toString()) }
            var negative by remember { mutableStateOf(initial.negativeMarks.toString()) }
            var status by remember { mutableStateOf(initial.status) }
            var error by remember { mutableStateOf<String?>(null) }

            AdminEditorDialog(
                title = if (initial.id.isBlank()) "New test" else "Edit test",
                onDismiss = { creating = false; editing = null },
                onSave = {
                    val d = duration.trim().toIntOrNull()
                    val m = marks.trim().toIntOrNull()
                    val n = negative.trim().toIntOrNull()
                    when {
                        title.isBlank() -> error = "Title is required."
                        d == null || d < 1 -> error = "Duration must be at least 1 minute."
                        m == null || m < 1 || m > 20 -> error = "Marks per question: 1..20."
                        n == null || n < 0 || n > m -> error = "Negative marks: 0..marksPerQuestion."
                        else -> scope.launch {
                            viewModel.save(
                                initial.copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    category = category,
                                    subject = subject,
                                    durationMinutes = d,
                                    marksPerQuestion = m,
                                    negativeMarks = n,
                                    status = status
                                )
                            ).fold(
                                onSuccess = {
                                    sess.notify("Test saved")
                                    sess.audit(
                                        if (initial.id.isBlank()) "CREATE_TEST" else "UPDATE_TEST",
                                        "Test", it, title.trim()
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
                AdminFormField("Description", description, { description = it }, singleLine = false)
                Text("Category", color = TextWhiteSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("JEE Main AITS", "JEE Advanced AITS", "PYQ", "Custom Test").forEach { opt ->
                        val sel = category == opt
                        Text(
                            opt,
                            color = if (sel) AccentCyan else TextWhiteMuted,
                            fontSize = 10.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .background(
                                    if (sel) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { category = opt }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
                Text("Subject", color = TextWhiteSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "Physics", "Chemistry", "Mathematics").forEach { opt ->
                        val sel = subject == opt
                        Text(
                            opt,
                            color = if (sel) AccentCyan else TextWhiteMuted,
                            fontSize = 10.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .background(
                                    if (sel) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { subject = opt }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        AdminFormField("Duration (min)", duration) { duration = it; error = null }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AdminFormField("+Marks", marks) { marks = it; error = null }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AdminFormField("-Negative", negative) { negative = it; error = null }
                    }
                }
                Text("Status", color = TextWhiteSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("draft", "published", "upcoming", "closed").forEach { opt ->
                        val sel = status == opt
                        Text(
                            opt,
                            color = if (sel) AccentEmerald else TextWhiteMuted,
                            fontSize = 10.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .background(
                                    if (sel) AccentEmerald.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { status = opt }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        deleting?.let { target ->
            AdminDeleteConfirm(
                itemName = target.title,
                extraWarning = "Its questions and in-progress attempts will be cleaned up. Published results are kept for history.",
                onConfirm = {
                    scope.launch {
                        viewModel.delete(target.id).fold(
                            onSuccess = {
                                sess.notify("Test and question set deleted")
                                sess.audit("DELETE_TEST", "Test", target.id, target.title)
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
private fun MiniAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        label,
        color = if (danger) AccentRose else AccentCyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(
                (if (danger) AccentRose else AccentCyan).copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

// ══════════════════════ QUESTION EDITOR ══════════════════════

class AdminQuestionEditorViewModel @JvmOverloads constructor(
    private val repo: TestRepository = TestRepository()
) : ViewModel() {

    private val _questions = MutableStateFlow<Resource<List<Question>>>(Resource.Loading)
    val questions: StateFlow<Resource<List<Question>>> = _questions

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private var testId: String = ""

    fun init(testId: String) {
        if (this.testId == testId) return
        this.testId = testId
        refresh(testId)
    }

    fun refresh(testId: String) {
        _questions.value = Resource.Loading
        viewModelScope.launch {
            repo.getQuestionsForTestFlow(testId).collect { _questions.value = it }
        }
    }

    suspend fun persist(testId: String, questions: List<Question>): Result<Unit> {
        _saving.value = true
        val res = repo.saveQuestions(testId, questions)
        _saving.value = false
        return res
    }

    fun draftId(index: Int): String = "draft_$index"
}

@Composable
fun AdminQuestionEditorScreen(navController: NavController, testId: String) {
    val viewModel: AdminQuestionEditorViewModel = viewModel()
    val resource by viewModel.questions.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }

    androidx.compose.runtime.LaunchedEffect(testId) { viewModel.init(testId) }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(
            title = "Questions",
            subtitle = if (saving) "Saving…" else "Add, edit, reorder — saved as a set",
            onBack = { navController.popBackStack() }
        )

        ResourceView(
            resource = resource,
            emptyTitle = "No questions yet",
            emptyDescription = "Add a question manually or bulk-import JSON for this test.",
            onRetry = { viewModel.refresh(testId) },
            content = { list ->
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EdutopiaPrimaryButton(
                            text = "+ Add question",
                            onClick = { creating = true },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        SecondaryButton(
                            text = "JSON Import",
                            onClick = { navController.navigate(Screen.AdminJsonImporter.createRoute(testId)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val q = list[idx]
                            QuestionAdminRow(
                                index = idx + 1,
                                question = q,
                                onEdit = { editingIndex = idx },
                                onDelete = { deleteIndex = idx }
                            )
                        }
                    }
                }
            }
        )
    }

    val current = (resource as? Resource.Success)?.data.orEmpty()

    if (creating) {
        QuestionFormDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { q ->
                val updated = current + q.copy(id = viewModel.draftId(current.size))
                scope.launch {
                    viewModel.persist(testId, updated).fold(
                        onSuccess = { creating = false },
                        onFailure = { /* surfaced via flow error */ }
                    )
                }
            }
        )
    }

    editingIndex?.let { idx ->
        val q = current.getOrNull(idx)
        if (q != null) {
            QuestionFormDialog(
                initial = q,
                onDismiss = { editingIndex = null },
                onSave = { updatedQ ->
                    val updated = current.toMutableList().also { it[idx] = updatedQ }
                    scope.launch {
                        viewModel.persist(testId, updated)
                        editingIndex = null
                    }
                }
            )
        }
    }

    deleteIndex?.let { idx ->
        val q = current.getOrNull(idx)
        if (q != null) {
            AdminDeleteConfirm(
                itemName = "Question ${idx + 1}",
                extraWarning = "The remaining questions are renumbered automatically.",
                onConfirm = {
                    val updated = current.filterIndexed { i, _ -> i != idx }
                    scope.launch {
                        viewModel.persist(testId, updated)
                        deleteIndex = null
                    }
                },
                onDismiss = { deleteIndex = null }
            )
        }
    }
}

@Composable
private fun QuestionAdminRow(
    index: Int,
    question: Question,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    EdutopiaCard(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF18233C), onClick = onEdit) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Q$index",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${question.subject} • ${question.difficulty} • +${question.marks}/-${question.negativeMarks}",
                    color = TextWhiteMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Ans: ${question.correctAnswer}",
                    color = AccentEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                question.questionText,
                color = TextWhitePrimary,
                fontSize = 13.sp,
                maxLines = 2,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniAction("Edit", onClick = onEdit)
                MiniAction("Delete", danger = true, onClick = onDelete)
            }
        }
    }
}

@Composable
internal fun QuestionFormDialog(
    initial: Question?,
    onDismiss: () -> Unit,
    onSave: (Question) -> Unit
) {
    var text by remember { mutableStateOf(initial?.questionText ?: "") }
    var optA by remember { mutableStateOf(initial?.options?.get("A") ?: "") }
    var optB by remember { mutableStateOf(initial?.options?.get("B") ?: "") }
    var optC by remember { mutableStateOf(initial?.options?.get("C") ?: "") }
    var optD by remember { mutableStateOf(initial?.options?.get("D") ?: "") }
    var answer by remember { mutableStateOf(initial?.correctAnswer ?: "A") }
    var subject by remember { mutableStateOf(initial?.subject ?: "Physics") }
    var chapter by remember { mutableStateOf(initial?.chapter ?: "") }
    var topic by remember { mutableStateOf(initial?.topic ?: "") }
    var difficulty by remember { mutableStateOf(initial?.difficulty ?: "Medium") }
    var marks by remember { mutableStateOf((initial?.marks ?: 4).toString()) }
    var negative by remember { mutableStateOf((initial?.negativeMarks ?: 1).toString()) }
    var explanation by remember { mutableStateOf(initial?.explanation ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AdminEditorDialog(
        title = if (initial == null) "New question" else "Edit question",
        onDismiss = onDismiss,
        onSave = {
            val m = marks.trim().toIntOrNull()
            val n = negative.trim().toIntOrNull()
            when {
                text.isBlank() -> error = "Question text is required."
                listOf(optA, optB, optC, optD).any { it.isBlank() } ->
                    error = "All four options are required."
                answer !in listOf("A", "B", "C", "D") -> error = "Correct answer must be A/B/C/D."
                m == null || m < 1 || m > 20 -> error = "Marks: 1..20."
                n == null || n < 0 || n > m -> error = "Negative marks: 0..marks."
                else -> onSave(
                    (initial ?: Question()).copy(
                        questionText = text.trim(),
                        options = mapOf(
                            "A" to optA.trim(),
                            "B" to optB.trim(),
                            "C" to optC.trim(),
                            "D" to optD.trim()
                        ),
                        correctAnswer = answer,
                        subject = subject,
                        chapter = chapter.trim(),
                        topic = topic.trim(),
                        difficulty = difficulty,
                        marks = m,
                        negativeMarks = n,
                        explanation = explanation.trim(),
                        questionType = "Single Correct MCQ"
                    )
                )
            }
        }
    ) {
        if (error != null) Text(error!!, color = AccentRose, fontSize = 12.sp)
        AdminFormField("Question text", text, { text = it; error = null }, singleLine = false)
        AdminFormField("Option A", optA) { optA = it }
        AdminFormField("Option B", optB) { optB = it }
        AdminFormField("Option C", optC) { optC = it }
        AdminFormField("Option D", optD) { optD = it }
        Column {
            Text("Correct answer", color = TextWhiteSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("A", "B", "C", "D").forEach { opt ->
                    val sel = answer == opt
                    Text(
                        opt,
                        color = if (sel) Color.White else TextWhiteMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) AccentEmerald else Color(0xFF0F172A))
                            .clickable { answer = opt }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Column {
            Text("Subject", color = TextWhiteSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Physics", "Chemistry", "Mathematics").forEach { opt ->
                    val sel = subject == opt
                    Text(
                        opt,
                        color = if (sel) AccentCyan else TextWhiteMuted,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(
                                if (sel) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { subject = opt }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
        Column {
            Text("Difficulty", color = TextWhiteSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Easy", "Medium", "Hard").forEach { opt ->
                    val sel = difficulty == opt
                    Text(
                        opt,
                        color = if (sel) AccentAmber else TextWhiteMuted,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(
                                if (sel) AccentAmber.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { difficulty = opt }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
        AdminFormField("Chapter (optional)", chapter) { chapter = it }
        AdminFormField("Topic (optional)", topic) { topic = it }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                AdminFormField("Marks", marks) { marks = it; error = null }
            }
            Column(modifier = Modifier.weight(1f)) {
                AdminFormField("Negative", negative) { negative = it; error = null }
            }
        }
        AdminFormField("Explanation (optional)", explanation, { explanation = it }, singleLine = false)
    }
}

// ══════════════════════ JSON IMPORTER ══════════════════════

class AdminJsonImporterViewModel @JvmOverloads constructor(
    private val repo: TestRepository = TestRepository()
) : ViewModel() {

    private val _validation = MutableStateFlow<JsonValidationResult?>(null)
    val validation: StateFlow<JsonValidationResult?> = _validation

    fun validate(raw: String) {
        _validation.value = if (raw.isBlank()) null else JsonQuestionValidator.validateJson(raw)
    }

    suspend fun import(testId: String, questions: List<Question>, append: Boolean): Result<Unit> {
        val target = if (append) {
            val existing = repo.getQuestionsForTest(testId).getOrNull().orEmpty()
            existing + questions
        } else {
            questions
        }
        return repo.saveQuestions(testId, target)
    }

    suspend fun count(testId: String): Int =
        repo.getQuestionsForTest(testId).getOrNull()?.size ?: 0
}

@Composable
fun AdminJsonImporterScreen(navController: NavController, testId: String) {
    val viewModel: AdminJsonImporterViewModel = viewModel()
    val validation by viewModel.validation.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var raw by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false)}
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(
            title = "JSON Question Import",
            subtitle = "Strict validation — every item must pass before import",
            onBack = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Paste a JSON array of questions or a single question object. " +
                    "Required fields: question, options (A-D non-blank), correctAnswer (A-D). " +
                    "Optional: subject (Physics/Chemistry/Mathematics), difficulty (Easy/Medium/Hard), " +
                    "marks (1-20), negativeMarks (0..marks), explanation, chapter, topic.",
                color = TextWhiteMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = raw,
                    onValueChange = {
                        raw = it
                        message = null
                        viewModel.validate(it)
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextWhitePrimary, fontSize = 12.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(BrandIndigo),
                    modifier = Modifier.fillMaxSize()
                )
                if (raw.isBlank()) {
                    Text("[ { \"question\": … } ]", color = TextWhiteMuted, fontSize = 12.sp)
                }
            }

            validation?.let { v ->
                if (v.globalError != null) {
                    Text("⚠ ${v.globalError}", color = AccentRose, fontSize = 12.sp)
                } else {
                    Text(
                        "${v.totalCount} items • ${v.validCount} valid • ${v.invalidCount} invalid",
                        color = if (v.invalidCount == 0) AccentEmerald else AccentRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(v.items.size) { idx ->
                            val item = v.items[idx]
                            EdutopiaCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = if (item.isValid) Color(0xFF18233C) else AccentRose.copy(alpha = 0.08f),
                                borderColor = if (item.isValid) SurfaceBorderDark else AccentRose.copy(alpha = 0.4f)
                            ) {
                                Column {
                                    Text(
                                        "Item #${item.index}" + (item.question?.let { " • ${it.subject} • ${it.difficulty}" } ?: ""),
                                        color = TextWhiteSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        item.question?.questionText ?: item.rawJson.take(120),
                                        color = TextWhitePrimary,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        lineHeight = 16.sp
                                    )
                                    if (!item.isValid) {
                                        item.errors.forEach { err ->
                                            Text("✗ $err", color = AccentRose, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (v.allValid) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            EdutopiaPrimaryButton(
                                text = if (busy) "Importing…" else "Import ${v.validCount} questions (replace)",
                                onClick = {
                                    if (busy) return@EdutopiaPrimaryButton
                                    scope.launch {
                                        busy = true
                                        val qs = v.items.mapNotNull { it.question }
                                        viewModel.import(testId, qs, append = false).fold(
                                            onSuccess = {
                                                message = "Imported ${qs.size} questions (replaced existing set)."
                                                isError = false
                                            },
                                            onFailure = { e ->
                                                message = "Import failed: ${e.localizedMessage}"
                                                isError = true
                                            }
                                        )
                                        busy = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !busy
                            )
                            SecondaryButton(
                                text = "Append",
                                onClick = {
                                    if (busy) return@SecondaryButton
                                    scope.launch {
                                        busy = true
                                        val qs = v.items.mapNotNull { it.question }
                                        viewModel.import(testId, qs, append = true).fold(
                                            onSuccess = {
                                                message = "Appended ${qs.size} questions after the existing set."
                                                isError = false
                                            },
                                            onFailure = { e ->
                                                message = "Import failed: ${e.localizedMessage}"
                                                isError = true
                                            }
                                        )
                                        busy = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !busy
                            )
                        }
                    } else {
                        Text(
                            "Fix the ${v.invalidCount} invalid item(s) — import unlocks only when every item passes validation.",
                            color = AccentAmber,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (message != null) {
                Text(
                    message!!,
                    color = if (isError) AccentRose else AccentEmerald,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

// ══════════════════════ PYQ BANK ══════════════════════

class AdminPyqViewModel @JvmOverloads constructor(
    private val repo: PyqRepository = PyqRepository()
) : ViewModel() {
    val items: StateFlow<Resource<List<PyqQuestion>>> = repo.getAllPyqsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun save(p: PyqQuestion): Result<String> = repo.savePyq(p)
    suspend fun delete(id: String): Result<Unit> = repo.deletePyq(id)
}

@Composable
fun AdminPyqScreen(navController: NavController) {
    val viewModel: AdminPyqViewModel = viewModel()
    val resource by viewModel.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<PyqQuestion?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PyqQuestion?>(null) }

    AdminGate(navController = navController, title = "PYQ Bank", subtitle = "Previous-year questions") { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                EdutopiaPrimaryButton(
                    text = "+ New PYQ",
                    onClick = { creating = true },
                    modifier = Modifier.width(150.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "PYQ bank is empty",
                emptyDescription = "Add previous-year questions for practice sessions.",
                onRetry = null,
                content = { list ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val p = list[idx]
                            AdminListRow(
                                title = p.questionText.take(80),
                                caption = "${p.examType} ${p.year} ${p.shift} • ${p.subject} • ${p.chapter} • " +
                                    "${p.solvedCount} students • ${p.totalAttempts} attempts • acc ${p.accuracy}%",
                                onClick = { editing = p },
                                onDelete = { deleting = p }
                            )
                        }
                    }
                }
            )
        }

        (editing ?: if (creating) PyqQuestion() else null)?.let { initial ->
            var questionText by remember { mutableStateOf(initial.questionText) }
            var examType by remember { mutableStateOf(initial.examType) }
            var year by remember { mutableStateOf(initial.year.toString()) }
            var shift by remember { mutableStateOf(initial.shift) }
            var subject by remember { mutableStateOf(initial.subject) }
            var chapter by remember { mutableStateOf(initial.chapter) }
            var difficulty by remember { mutableStateOf(initial.difficulty) }
            var optA by remember { mutableStateOf(initial.options["A"] ?: "") }
            var optB by remember { mutableStateOf(initial.options["B"] ?: "") }
            var optC by remember { mutableStateOf(initial.options["C"] ?: "") }
            var optD by remember { mutableStateOf(initial.options["D"] ?: "") }
            var answer by remember { mutableStateOf(initial.correctAnswer) }
            var explanation by remember { mutableStateOf(initial.explanation) }
            var error by remember { mutableStateOf<String?>(null) }

            AdminEditorDialog(
                title = if (initial.id.isBlank()) "New PYQ" else "Edit PYQ",
                onDismiss = { creating = false; editing = null },
                onSave = {
                    val y = year.trim().toIntOrNull()
                    when {
                        questionText.isBlank() -> error = "Question text required."
                        listOf(optA, optB, optC, optD).any { it.isBlank() } -> error = "All options required."
                        answer !in listOf("A", "B", "C", "D") -> error = "Answer must be A-D."
                        y == null || y < 1990 || y > 2100 -> error = "Invalid year."
                        else -> scope.launch {
                            viewModel.save(
                                initial.copy(
                                    questionText = questionText.trim(),
                                    examType = examType,
                                    year = y,
                                    shift = shift,
                                    subject = subject,
                                    chapter = chapter.trim(),
                                    difficulty = difficulty,
                                    options = mapOf(
                                        "A" to optA.trim(), "B" to optB.trim(),
                                        "C" to optC.trim(), "D" to optD.trim()
                                    ),
                                    correctAnswer = answer,
                                    explanation = explanation.trim()
                                )
                            ).fold(
                                onSuccess = {
                                    sess.notify("PYQ saved")
                                    sess.audit(
                                        if (initial.id.isBlank()) "CREATE_PYQ" else "UPDATE_PYQ",
                                        "Pyq", it, questionText.take(60)
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
                AdminFormField("Question", questionText, { questionText = it; error = null }, singleLine = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Exam", color = TextWhiteSecondary, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("JEE Main", "JEE Advanced").forEach { opt ->
                                Text(
                                    opt,
                                    color = if (examType == opt) AccentCyan else TextWhiteMuted,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(
                                            if (examType == opt) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { examType = opt }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AdminFormField("Year", year) { year = it; error = null }
                    }
                }
                AdminFormField("Shift", shift) { shift = it }
                Text("Subject", color = TextWhiteSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Physics", "Chemistry", "Mathematics").forEach { opt ->
                        Text(
                            opt,
                            color = if (subject == opt) AccentCyan else TextWhiteMuted,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(
                                    if (subject == opt) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { subject = opt }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                AdminFormField("Chapter", chapter) { chapter = it }
                Text("Difficulty", color = TextWhiteSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Easy", "Medium", "Hard").forEach { opt ->
                        Text(
                            opt,
                            color = if (difficulty == opt) AccentAmber else TextWhiteMuted,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(
                                    if (difficulty == opt) AccentAmber.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { difficulty = opt }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                AdminFormField("Option A", optA) { optA = it }
                AdminFormField("Option B", optB) { optB = it }
                AdminFormField("Option C", optC) { optC = it }
                AdminFormField("Option D", optD) { optD = it }
                Text("Correct answer", color = TextWhiteSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("A", "B", "C", "D").forEach { opt ->
                        Text(
                            opt,
                            color = if (answer == opt) Color.White else TextWhiteMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    if (answer == opt) AccentEmerald else Color(0xFF0F172A),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { answer = opt }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
                AdminFormField("Explanation", explanation, { explanation = it }, singleLine = false)
            }
        }

        deleting?.let { target ->
            AdminDeleteConfirm(
                itemName = target.questionText.take(60),
                extraWarning = "Per-student attempt history for this question is also removed.",
                onConfirm = {
                    scope.launch {
                        viewModel.delete(target.id).fold(
                            onSuccess = {
                                sess.notify("PYQ deleted")
                                sess.audit("DELETE_PYQ", "Pyq", target.id, target.questionText.take(60))
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

// ══════════════════════ TOURNAMENTS ══════════════════════

class AdminTournamentsViewModel @JvmOverloads constructor(
    private val repo: TournamentRepository = TournamentRepository()
) : ViewModel() {
    val items: StateFlow<Resource<List<Tournament>>> = repo.getTournamentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    suspend fun save(t: Tournament): Result<String> = repo.saveTournament(t)
    suspend fun delete(id: String): Result<Unit> = repo.deleteTournament(id)
}

@Composable
fun AdminTournamentsScreen(navController: NavController) {
    val viewModel: AdminTournamentsViewModel = viewModel()
    val resource by viewModel.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Tournament?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Tournament?>(null) }

    AdminGate(navController = navController, title = "Tournaments", subtitle = "Scheduled live events") { _, sess ->
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                EdutopiaPrimaryButton(
                    text = "+ New tournament",
                    onClick = { creating = true },
                    modifier = Modifier.width(190.dp),
                    height = 38.dp
                )
            }
            ResourceView(
                resource = resource,
                emptyTitle = "No tournaments yet",
                emptyDescription = "Schedule a live event and attach a test to it.",
                onRetry = null,
                content = { list ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list.size) { idx ->
                            val t = list[idx]
                            AdminListRow(
                                title = t.title,
                                caption = "${t.totalQuestions} Qs • ${t.durationMinutes} min • ${t.participantCount} participants • prize ${t.prizePoolPoints} pts",
                                trailing = t.status.uppercase(),
                                trailingColor = when (t.status) {
                                    "live" -> AccentRose
                                    "upcoming" -> AccentAmber
                                    else -> TextWhiteMuted
                                },
                                onClick = { editing = t },
                                onDelete = { deleting = t }
                            )
                        }
                    }
                }
            )
        }

        (editing ?: if (creating) Tournament() else null)?.let { initial ->
            var title by remember { mutableStateOf(initial.title) }
            var description by remember { mutableStateOf(initial.description) }
            var testId by remember { mutableStateOf(initial.testId) }
            var duration by remember { mutableStateOf(initial.durationMinutes.toString()) }
            var questions by remember { mutableStateOf(initial.totalQuestions.toString()) }
            var xp by remember { mutableStateOf(initial.xpReward.toString()) }
            var status by remember { mutableStateOf(initial.status) }
            var error by remember { mutableStateOf<String?>(null) }

            AdminEditorDialog(
                title = if (initial.id.isBlank()) "New tournament" else "Edit tournament",
                onDismiss = { creating = false; editing = null },
                onSave = {
                    val d = duration.trim().toIntOrNull()
                    val q = questions.trim().toIntOrNull()
                    val x = xp.trim().toLongOrNull()
                    when {
                        title.isBlank() -> error = "Title required."
                        d == null || d < 1 -> error = "Duration must be positive."
                        q == null || q < 1 -> error = "Question count must be positive."
                        x == null || x < 0 -> error = "Invalid XP reward."
                        else -> scope.launch {
                            viewModel.save(
                                initial.copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    testId = testId.trim(),
                                    durationMinutes = d,
                                    totalQuestions = q,
                                    xpReward = x,
                                    status = status
                                )
                            ).fold(
                                onSuccess = {
                                    sess.notify("Tournament saved")
                                    sess.audit(
                                        if (initial.id.isBlank()) "CREATE_TOURNAMENT" else "UPDATE_TOURNAMENT",
                                        "Tournament", it, title.trim()
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
                AdminFormField("Description", description, { description = it }, singleLine = false)
                AdminFormField("Linked test ID (optional)", testId) { testId = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        AdminFormField("Duration min", duration) { duration = it; error = null }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AdminFormField("Questions", questions) { questions = it; error = null }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AdminFormField("XP reward", xp) { xp = it; error = null }
                    }
                }
                Text("Status", color = TextWhiteSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("upcoming", "live", "ended").forEach { opt ->
                        Text(
                            opt,
                            color = if (status == opt) AccentCyan else TextWhiteMuted,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(
                                    if (status == opt) AccentCyan.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { status = opt }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        deleting?.let { target ->
            AdminDeleteConfirm(
                itemName = target.title,
                extraWarning = "Participant records for this tournament are also removed.",
                onConfirm = {
                    scope.launch {
                        viewModel.delete(target.id).fold(
                            onSuccess = {
                                sess.notify("Tournament deleted")
                                sess.audit("DELETE_TOURNAMENT", "Tournament", target.id, target.title)
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
