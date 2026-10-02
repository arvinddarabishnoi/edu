package com.mindnova.edutopia.ui.tests

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.PaletteAnswered
import com.mindnova.edutopia.core.theme.PaletteAnsweredMarked
import com.mindnova.edutopia.core.theme.PaletteMarkedReview
import com.mindnova.edutopia.core.theme.PaletteNotAnswered
import com.mindnova.edutopia.core.theme.PaletteNotVisited
import com.mindnova.edutopia.core.theme.SurfaceDark
import com.mindnova.edutopia.core.theme.SurfaceDarkCard
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.DateTimeUtils
import com.mindnova.edutopia.data.models.Question

@Composable
fun TestEngineScreen(
    navController: NavController,
    testId: String,
    viewModel: TestEngineViewModel = viewModel()
) {
    LaunchedEffect(testId) {
        if (testId.isNotBlank()) {
            viewModel.start(testId, forceFresh = false)
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Navigate to result once submitted.
    LaunchedEffect(state.submittedResultId) {
        state.submittedResultId?.let { resultId ->
            navController.navigate(Screen.TestResult.createRoute(resultId)) {
                popUpTo(Screen.TestsList.route) { inclusive = false }
            }
        }
    }

    when (state.status) {
        EngineStatus.Loading -> LoadingStateView(message = "Preparing your exam…")

        EngineStatus.Failed -> ErrorStateView(
            title = if (state.questions.isNotEmpty()) "Submission failed" else "Cannot open test",
            errorMessage = state.error,
            onRetry = {
                // Retry re-enters the exam (attempt is persisted) or leaves if
                // the test itself could not be opened.
                if (state.questions.isNotEmpty()) {
                    viewModel.start(testId, forceFresh = false)
                } else {
                    navController.popBackStack()
                }
            }
        )

        else -> {
            val test = state.test ?: return
            val questions = state.questions
            val currentQ = questions.getOrNull(state.currentIndex)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark)
            ) {
                // ── Exam header with timer ─────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            test.title,
                            color = TextWhitePrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            "Q ${state.currentIndex + 1} of ${questions.size}",
                            color = TextWhiteMuted,
                            fontSize = 11.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val timeColor = when {
                            state.timeRemainingSec <= 60 -> AccentRose
                            state.timeRemainingSec <= 300 -> AccentAmber
                            else -> AccentCyan
                        }
                        Text(
                            text = DateTimeUtils.formatTimerCountdown(state.timeRemainingSec),
                            color = timeColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.status == EngineStatus.Submitting) {
                            Text(
                                "Submitting…",
                                color = TextWhiteMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = {
                        if (test.durationMinutes <= 0) 0f
                        else 1f - (state.timeRemainingSec.toFloat() / (test.durationMinutes * 60f))
                    },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = AccentRose,
                    trackColor = SurfaceDarkCard
                )

                // ── Question ───────────────────────────────────────────
                if (currentQ != null) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Question ${currentQ.questionNumber}",
                                color = AccentCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                currentQ.subject,
                                color = TextWhiteMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                "+${currentQ.marks} / -${currentQ.negativeMarks}",
                                color = AccentAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            currentQ.questionText,
                            color = TextWhitePrimary,
                            fontSize = 15.sp,
                            lineHeight = 23.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        listOf("A", "B", "C", "D").forEach { key ->
                            val optionText = currentQ.options[key] ?: return@forEach
                            val selected = state.answers[currentQ.id] == key
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) BrandIndigo.copy(alpha = 0.14f) else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) BrandIndigo else Color(0xFF2B3C62),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = state.status == EngineStatus.Ready) {
                                        viewModel.selectAnswer(currentQ.id, key)
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) BrandIndigo else Color(0xFF18233C)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        key,
                                        color = if (selected) Color.White else TextWhiteSecondary,
                                        fontSize = 13.sp,
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

                        if (state.answers.containsKey(currentQ.id)) {
                            TextButton(
                                onClick = { viewModel.clearAnswer(currentQ.id) },
                                enabled = state.status == EngineStatus.Ready
                            ) {
                                Text("Clear response", color = TextWhiteMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // ── Footer controls ────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.goTo(state.currentIndex - 1) },
                        enabled = state.currentIndex > 0
                    ) {
                        Text("Prev", color = if (state.currentIndex > 0) AccentCyan else TextWhiteMuted)
                    }
                    TextButton(
                        onClick = { currentQ?.let { viewModel.toggleMarkForReview(it.id) } },
                        enabled = state.status == EngineStatus.Ready && currentQ != null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val marked = currentQ != null && currentQ.id in state.markedForReview
                            Icon(
                                Icons.Default.Flag,
                                null,
                                tint = if (marked) AccentAmber else TextWhiteMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (marked) "Unmark" else "Mark",
                                color = TextWhiteSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (state.currentIndex < questions.size - 1) {
                        EdutopiaPrimaryButton(
                            text = "Save & Next",
                            onClick = { viewModel.goTo(state.currentIndex + 1) },
                            modifier = Modifier.width(140.dp),
                            height = 42.dp
                        )
                    } else {
                        var showSubmitSheet by remember { mutableStateOf(false) }
                        EdutopiaPrimaryButton(
                            text = "Submit Test",
                            onClick = { showSubmitSheet = true },
                            enabled = state.status == EngineStatus.Ready,
                            modifier = Modifier.width(140.dp),
                            height = 42.dp
                        )
                        if (showSubmitSheet) {
                            SubmitConfirmationDialog(
                                questions = questions,
                                state = state,
                                onConfirm = {
                                    showSubmitSheet = false
                                    viewModel.submit(auto = false)
                                },
                                onDismiss = { showSubmitSheet = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmitConfirmationDialog(
    questions: List<Question>,
    state: TestEngineUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val attempted = questions.count { state.answers.containsKey(it.id) }
    val unanswered = questions.size - attempted
    val marked = questions.count { it.id in state.markedForReview }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDarkCard,
        title = {
            Text("Submit test?", color = TextWhitePrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Please review before you submit. You cannot change answers after submission.",
                    color = TextWhiteSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryPill("Attempted", "$attempted", PaletteAnswered, Modifier.weight(1f))
                    SummaryPill("Unanswered", "$unanswered", PaletteNotAnswered, Modifier.weight(1f))
                    SummaryPill("Marked", "$marked", PaletteMarkedReview, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF2B3C62))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Questions you marked for review: " +
                        if (marked == 0) "none"
                        else "double-check them in the palette before submitting.",
                    color = TextWhiteMuted,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes, submit", color = AccentEmerald, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep solving", color = TextWhiteMuted)
            }
        }
    )
}

@Composable
private fun SummaryPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextWhiteMuted, fontSize = 10.sp)
        }
    }
}
