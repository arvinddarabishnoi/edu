package com.mindnova.edutopia.ui.pyq

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.SkeletonCard
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.SurfaceBorderDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.PyqQuestion
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.PyqRepository
import com.mindnova.edutopia.data.repository.PyqSubmitOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PyqPracticeUiState(
    val questions: List<PyqQuestion> = emptyList(),
    val index: Int = 0,
    val selectedAnswer: String? = null,
    val revealed: Boolean = false,
    val lastOutcome: String? = null,
    val solvedCount: Int = 0,
    val loading: Boolean = true,
    val error: String? = null
)

class PyqPracticeViewModel @JvmOverloads constructor(
    private val pyqRepo: PyqRepository = PyqRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(PyqPracticeUiState())
    val state: StateFlow<PyqPracticeUiState> = _state.asStateFlow()

    private var started = false

    fun start(subject: String, year: Int) {
        if (started) return
        started = true
        viewModelScope.launch {
            val resource = pyqRepo.getPyqsFlow(examType = "All")
                .first { it !is Resource.Loading }
            when (resource) {
                is Resource.Success -> {
                    val filtered = resource.data
                        .filter { subject == "All" || it.subject == subject }
                        .filter { year == 0 || it.year == year }
                        .shuffled() // deterministic enough per session; practice order variety
                    _state.value = _state.value.copy(
                        questions = filtered,
                        loading = false
                    )
                }
                is Resource.Error -> _state.value = _state.value.copy(
                    loading = false, error = resource.message
                )
                is Resource.Empty -> _state.value = _state.value.copy(
                    questions = emptyList(), loading = false
                )
                else -> Unit
            }
        }
    }

    fun select(option: String) {
        if (_state.value.revealed) return
        _state.value = _state.value.copy(selectedAnswer = option)
    }

    fun checkAnswer() {
        val s = _state.value
        val q = s.questions.getOrNull(s.index) ?: return
        val chosen = s.selectedAnswer ?: return
        val isCorrect = chosen.trim().uppercase() == q.correctAnswer.trim().uppercase()

        viewModelScope.launch {
            val uid = authRepo.currentUserId
            val outcomeText = if (uid == null) {
                "Sign in to record progress"
            } else {
                pyqRepo.recordPyqAttempt(uid, q.id, isCorrect).fold(
                    onSuccess = { outcome ->
                        when (outcome) {
                            is PyqSubmitOutcome.Recorded -> "Correct! +${outcome.xpAwarded} XP"
                            is PyqSubmitOutcome.RepeatAttempt ->
                                if (isCorrect) "Correct — progress already recorded" else "Not quite"
                        }
                    },
                    onFailure = { e -> "Answer shown; progress not saved: ${e.localizedMessage}" }
                )
            }
            _state.value = _state.value.copy(
                revealed = true,
                lastOutcome = outcomeText,
                solvedCount = _state.value.solvedCount + if (isCorrect) 1 else 0
            )
        }
    }

    fun next() {
        val s = _state.value
        if (s.index < s.questions.size - 1) {
            _state.value = s.copy(
                index = s.index + 1,
                selectedAnswer = null,
                revealed = false,
                lastOutcome = null
            )
        }
    }

    fun retry() {
        started = false
        _state.value = PyqPracticeUiState()
        start("All", 0)
    }
}

@Composable
fun PyqPracticeScreen(
    navController: NavController,
    subject: String,
    year: Int,
    viewModel: PyqPracticeViewModel = viewModel()
) {
    LaunchedEffect(subject, year) { viewModel.start(subject, year) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(
            title = "PYQ Practice",
            subtitle = if (subject != "All" || year != 0)
                "$subject${if (year != 0) " • $year" else ""}" else "Mixed JEE",
            onBack = { navController.popBackStack() }
        )

        when {
            state.loading -> LoadingStateView()
            state.error != null -> ErrorStateView(errorMessage = state.error, onRetry = { viewModel.retry() })
            state.questions.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No PYQs match this selection yet.",
                    color = TextWhiteMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                val q = state.questions.getOrNull(state.index) ?: return@Column
                val progress = if (state.questions.isEmpty()) 0f
                else (state.index + 1f) / state.questions.size

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = AccentCyan,
                    trackColor = Color(0xFF1E293B)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Q ${state.index + 1}/${state.questions.size}",
                            color = TextWhiteMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetaChip("${q.year} ${q.shift}")
                            MetaChip(q.difficulty, when (q.difficulty) {
                                "Easy" -> AccentEmerald
                                "Hard" -> AccentRose
                                else -> AccentAmber
                            })
                            MetaChip("+${q.marks}/-${q.negativeMarks}")
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        q.questionText,
                        color = TextWhitePrimary,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        modifier = Modifier.animateContentSize()
                    )
                    if (q.chapter.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${q.subject} • ${q.chapter}",
                            color = TextWhiteMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    listOf("A", "B", "C", "D").forEach { key ->
                        val optionText = q.options[key] ?: return@forEach
                        val selected = state.selectedAnswer == key
                        val isCorrectOption = key == q.correctAnswer.trim().uppercase()
                        val borderColor = when {
                            state.revealed && isCorrectOption -> AccentEmerald
                            state.revealed && selected && !isCorrectOption -> AccentRose
                            selected -> BrandIndigo
                            else -> SurfaceBorderDark
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        state.revealed && isCorrectOption -> AccentEmerald.copy(alpha = 0.10f)
                                        state.revealed && selected && !isCorrectOption -> AccentRose.copy(alpha = 0.10f)
                                        selected -> BrandIndigo.copy(alpha = 0.10f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(1.2.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable(enabled = !state.revealed) { viewModel.select(key) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(borderColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    key,
                                    color = borderColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                optionText,
                                color = TextWhitePrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (state.revealed) {
                        Spacer(Modifier.height(14.dp))
                        EdutopiaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFF18233C),
                            borderColor = AccentCyan.copy(alpha = 0.3f)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.HelpOutline,
                                        null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Explanation",
                                        color = AccentCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    q.explanation.ifBlank { "No explanation provided for this question yet." },
                                    color = TextWhiteSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                                if (state.lastOutcome != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = AccentEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            state.lastOutcome!!,
                                            color = AccentEmerald,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    if (!state.revealed) {
                        EdutopiaPrimaryButton(
                            text = "Check Answer",
                            onClick = { viewModel.checkAnswer() },
                            enabled = state.selectedAnswer != null
                        )
                    } else if (state.index < state.questions.size - 1) {
                        EdutopiaPrimaryButton(text = "Next Question →", onClick = { viewModel.next() })
                    } else {
                        EdutopiaPrimaryButton(
                            text = "Finish session",
                            onClick = { navController.popBackStack() }
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, color: Color = TextWhiteSecondary) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
