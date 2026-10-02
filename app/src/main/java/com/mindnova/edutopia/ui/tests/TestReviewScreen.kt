package com.mindnova.edutopia.ui.tests

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.AppTopBar
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.SurfaceBorderDark
import com.mindnova.edutopia.core.theme.SurfaceDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.data.models.Question
import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.data.repository.TestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val result: TestResult? = null,
    val questions: List<Question> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class TestReviewViewModel @JvmOverloads constructor(
    private val testRepo: TestRepository = TestRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    fun load(resultId: String, testId: String) {
        _state.value = ReviewUiState()
        viewModelScope.launch {
            val result = testRepo.getResultById(resultId)
            val questions = testRepo.getQuestionsForTest(testId)
            result.fold(
                onSuccess = { r ->
                    if (r == null) {
                        _state.value = ReviewUiState(
                            loading = false,
                            error = "Result not found. It may have been deleted."
                        )
                    } else {
                        questions.fold(
                            onSuccess = { qs ->
                                _state.value = ReviewUiState(
                                    result = r,
                                    questions = qs.sortedBy { it.order },
                                    loading = false
                                )
                            },
                            onFailure = { e ->
                                _state.value = ReviewUiState(
                                    result = r,
                                    loading = false,
                                    error = "Questions could not be loaded: ${e.localizedMessage}"
                                )
                            }
                        )
                    }
                },
                onFailure = { e ->
                    _state.value = ReviewUiState(
                        loading = false,
                        error = e.localizedMessage ?: "Failed to load result."
                    )
                }
            )
        }
    }
}

private enum class ReviewFilter(val label: String) {
    All("All"), Correct("Correct"), Incorrect("Incorrect"), Unattempted("Unattempted")
}

@Composable
fun TestReviewScreen(
    navController: NavController,
    resultId: String,
    testId: String,
    viewModel: TestReviewViewModel = viewModel()
) {
    LaunchedEffect(resultId, testId) { viewModel.load(resultId, testId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(ReviewFilter.All) }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(
            title = "Review ${state.result?.testTitle?.let { "· $it" } ?: ""}",
            onBack = { navController.popBackStack() }
        )

        when {
            state.loading -> LoadingStateView()
            state.error != null && state.result == null -> ErrorStateView(
                errorMessage = state.error,
                onRetry = { viewModel.load(resultId, testId) }
            )
            else -> {
                val result = state.result ?: return@Column
                val answers = result.answers

                val visible = state.questions.filter { q ->
                    val chosen = answers[q.id]?.trim()?.uppercase()
                    when (filter) {
                        ReviewFilter.All -> true
                        ReviewFilter.Correct -> chosen != null && chosen == q.correctAnswer.trim().uppercase()
                        ReviewFilter.Incorrect -> chosen != null && chosen != q.correctAnswer.trim().uppercase()
                        ReviewFilter.Unattempted -> chosen.isNullOrBlank()
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReviewFilter.entries.forEach { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = {
                                Text(
                                    if (f == ReviewFilter.All) "All (${state.questions.size})" else f.label,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandIndigo.copy(alpha = 0.3f),
                                selectedLabelColor = TextWhitePrimary,
                                containerColor = SurfaceDark,
                                labelColor = TextWhiteSecondary
                            )
                        )
                    }
                }

                if (state.error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.error!!,
                        color = AccentRose,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (visible.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No questions in this category.",
                            color = TextWhiteMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visible, key = { it.id }) { q ->
                            QuestionReviewCard(
                                question = q,
                                userAnswer = answers[q.id]
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionReviewCard(question: Question, userAnswer: String?) {
    val correct = question.correctAnswer.trim().uppercase()
    val chosen = userAnswer?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
    val isCorrect = chosen == correct
    val attempted = chosen != null

    val accent = when {
        !attempted -> TextWhiteMuted
        isCorrect -> AccentEmerald
        else -> AccentRose
    }
    val statusIcon = when {
        !attempted -> Icons.Default.RemoveCircleOutline
        isCorrect -> Icons.Default.CheckCircle
        else -> Icons.Default.Close
    }
    val statusText = when {
        !attempted -> "Unattempted"
        isCorrect -> "Correct (+${question.marks})"
        else -> "Incorrect (-${question.negativeMarks})"
    }

    EdutopiaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF18233C),
        borderColor = accent.copy(alpha = 0.4f)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Q${question.questionNumber} • ${question.subject}",
                    color = TextWhiteMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(statusIcon, contentDescription = statusText, tint = accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(statusText, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                question.questionText,
                color = TextWhitePrimary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(12.dp))

            listOf("A", "B", "C", "D").forEach { key ->
                val optionText = question.options[key] ?: return@forEach
                val isRight = key == correct
                val isChosen = key == chosen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isRight -> AccentEmerald.copy(alpha = 0.10f)
                                isChosen -> AccentRose.copy(alpha = 0.10f)
                                else -> Color.Transparent
                            }
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$key.",
                        color = if (isRight) AccentEmerald else if (isChosen) AccentRose else TextWhiteMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        optionText,
                        color = TextWhitePrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isChosen) {
                        Text(
                            if (isRight) "Your pick" else "Your pick ✗",
                            color = if (isRight) AccentEmerald else AccentRose,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (question.explanation.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            "Explanation",
                            color = TextWhiteSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            question.explanation,
                            color = TextWhiteSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
