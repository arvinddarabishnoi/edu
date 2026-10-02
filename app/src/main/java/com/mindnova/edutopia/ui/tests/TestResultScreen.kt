package com.mindnova.edutopia.ui.tests

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.mindnova.edutopia.core.components.EdutopiaGradientCard
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.SecondaryButton
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.ChemistryColor
import com.mindnova.edutopia.core.theme.MathematicsColor
import com.mindnova.edutopia.core.theme.PhysicsColor
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.DateTimeUtils
import com.mindnova.edutopia.data.models.SubjectPerformance
import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.data.repository.TestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TestResultUiState(
    val result: TestResult? = null,
    val loading: Boolean = true,
    val error: String? = null
)

class TestResultViewModel @JvmOverloads constructor(
    private val testRepo: TestRepository = TestRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TestResultUiState())
    val state: StateFlow<TestResultUiState> = _state.asStateFlow()

    fun load(resultId: String) {
        _state.value = TestResultUiState()
        viewModelScope.launch {
            testRepo.getResultById(resultId).fold(
                onSuccess = { r ->
                    if (r == null) {
                        _state.value = _state.value.copy(
                            loading = false,
                            error = "This result document was not found. It may have been removed."
                        )
                    } else {
                        _state.value = _state.value.copy(result = r, loading = false)
                    }
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.localizedMessage ?: "Failed to load result."
                    )
                }
            )
        }
    }
}

@Composable
fun TestResultScreen(
    navController: NavController,
    resultId: String,
    viewModel: TestResultViewModel = viewModel()
) {
    LaunchedEffect(resultId) { viewModel.load(resultId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(title = "Result", onBack = { navController.popBackStack() })

        when {
            state.loading -> LoadingStateView()
            state.error != null -> ErrorStateView(errorMessage = state.error, onRetry = { viewModel.load(resultId) })
            else -> {
                val r = state.result ?: return@Column
                TestResultContent(
                    result = r,
                    onReview = {
                        navController.navigate(Screen.TestReview.createRoute(r.id, r.testId))
                    },
                    onBackToTests = {
                        navController.navigate(Screen.TestsList.route) {
                            popUpTo(Screen.StudentHome.route) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}

/** Stateless result UI (testable without Firebase). */
@Composable
internal fun TestResultContent(
    result: TestResult,
    onReview: () -> Unit,
    onBackToTests: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Score hero
        EdutopiaGradientCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    result.testTitle,
                    color = TextWhitePrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${result.score}",
                        color = AccentCyan,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        " / ${result.maxScore}",
                        color = TextWhiteMuted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    "${result.accuracy}% accuracy • ${DateTimeUtils.formatTimerCountdown(result.timeSpentSeconds)} taken",
                    color = TextWhiteSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CountStat("Correct", result.correctCount, AccentEmerald, Icons.Default.CheckCircle, Modifier.weight(1f))
            CountStat("Incorrect", result.incorrectCount, AccentRose, Icons.Default.Close, Modifier.weight(1f))
            CountStat("Skipped", result.unattemptedCount, TextWhiteMuted, Icons.Default.RemoveCircleOutline, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Subject-wise Performance",
            color = TextWhitePrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        SubjectRow("Physics", result.physicsScore, PhysicsColor)
        Spacer(Modifier.height(8.dp))
        SubjectRow("Chemistry", result.chemistryScore, ChemistryColor)
        Spacer(Modifier.height(8.dp))
        SubjectRow("Mathematics", result.mathsScore, MathematicsColor)

        if (result.weakTopicsIdentified.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Topics needing attention",
                color = TextWhitePrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            EdutopiaCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AccentRose.copy(alpha = 0.35f)
            ) {
                Column {
                    result.weakTopicsIdentified.forEach { topic ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentRose)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(topic, color = TextWhiteSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Honest note about rank/percentile — never show fabricated values.
        Text(
            text = "Rank & percentile are only published after the full cohort's attempts " +
                "are processed. Score, accuracy and subject analysis above are exact.",
            color = TextWhiteMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .padding(12.dp)
        )

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(text = "Back to Tests", onClick = onBackToTests, modifier = Modifier.weight(1f))
            EdutopiaPrimaryButton(text = "Review Answers", onClick = onReview, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun CountStat(
    label: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    EdutopiaCard(modifier = modifier, backgroundColor = Color(0xFF18233C)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text("$count", color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextWhiteMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SubjectRow(subject: String, perf: SubjectPerformance, color: Color) {
    EdutopiaCard(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF18233C)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(subject, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${perf.score}/${perf.maxScore}",
                    color = TextWhitePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E293B))
            ) {
                val frac = if (perf.maxScore > 0) {
                    (perf.score.toFloat() / perf.maxScore.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "✓ ${perf.correctCount}  ✗ ${perf.incorrectCount}  – ${perf.unattemptedCount}   •   ${perf.accuracyPercentage}% accuracy (attempted)",
                color = TextWhiteMuted,
                fontSize = 11.sp
            )
        }
    }
}
