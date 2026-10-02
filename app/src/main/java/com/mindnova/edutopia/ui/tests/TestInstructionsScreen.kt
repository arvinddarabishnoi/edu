package com.mindnova.edutopia.ui.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Quiz
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
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.StatCard
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.data.models.TestModel
import com.mindnova.edutopia.data.repository.TestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TestInstructionsUiState(
    val test: TestModel? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val notAvailable: String? = null
)

class TestInstructionsViewModel @JvmOverloads constructor(
    private val testRepo: TestRepository = TestRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TestInstructionsUiState())
    val state: StateFlow<TestInstructionsUiState> = _state.asStateFlow()

    fun load(testId: String) {
        _state.value = TestInstructionsUiState()
        viewModelScope.launch {
            testRepo.getTestById(testId).fold(
                onSuccess = { test ->
                    if (test == null) {
                        _state.value = _state.value.copy(
                            loading = false,
                            notAvailable = "This test no longer exists. It may have been deleted by an admin."
                        )
                    } else when (test.status) {
                        "published" -> _state.value = _state.value.copy(test = test, loading = false)
                        "upcoming" -> _state.value = _state.value.copy(
                            loading = false,
                            notAvailable = "This test opens on ${com.mindnova.edutopia.core.utils.DateTimeUtils.formatDate(test.startDate)}. It will appear here when it opens."
                        )
                        "closed" -> _state.value = _state.value.copy(
                            loading = false,
                            notAvailable = "This test has closed. No new attempts are accepted."
                        )
                        else -> _state.value = _state.value.copy(
                            loading = false,
                            notAvailable = "This test is not published yet."
                        )
                    }
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.localizedMessage ?: "Failed to load test details."
                    )
                }
            )
        }
    }
}

@Composable
fun TestInstructionsScreen(
    navController: NavController,
    testId: String,
    viewModel: TestInstructionsViewModel = viewModel()
) {
    LaunchedEffect(testId) { viewModel.load(testId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        AppTopBar(title = "Test Instructions", onBack = { navController.popBackStack() })

        when {
            state.loading -> LoadingStateView()
            state.error != null -> ErrorStateView(errorMessage = state.error, onRetry = { viewModel.load(testId) })
            state.notAvailable != null -> Box2(state.notAvailable!!)
            else -> {
                val test = state.test ?: return@Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        test.title,
                        color = TextWhitePrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 27.sp
                    )
                    if (test.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(test.description, color = TextWhiteSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = "Questions",
                            value = "${test.totalQuestions}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Duration",
                            value = "${test.durationMinutes} min",
                            valueColor = AccentCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Max Score",
                            value = "${test.totalQuestions * test.marksPerQuestion}",
                            valueColor = AccentAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    EdutopiaCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.HelpOutline,
                                    null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    "General Instructions",
                                    color = TextWhitePrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            defaultInstructions(test).forEachIndexed { i, line ->
                                Text(
                                    "  ${i + 1}.  $line",
                                    color = TextWhiteSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentAmber.copy(alpha = 0.1f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "The timer starts immediately after you tap Begin. It auto-submits at zero.",
                            color = AccentAmber,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    EdutopiaPrimaryButton(
                        text = "Begin Test",
                        onClick = {
                            navController.navigate(Screen.TestEngine.createRoute(test.id))
                        }
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun Box2(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Quiz,
            contentDescription = null,
            tint = TextWhiteMuted,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = TextWhiteSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

private fun defaultInstructions(test: TestModel): List<String> = listOf(
    "Each correct answer awards +${test.marksPerQuestion} marks; each wrong answer deducts ${test.negativeMarks} marks. Unattempted: 0.",
    "The question count shown in this summary is ${test.totalQuestions}; the live paper is loaded when you begin.",
    "You can navigate freely between questions and mark any question for review.",
    "Your answers are saved continuously — if the app closes, you can resume the same attempt.",
    "The test auto-submits when the timer reaches 00:00. Do not rely on this; submit manually when done.",
    if (test.instructions.isNotBlank()) test.instructions else "Negative marking applies as per JEE Main pattern."
).filter { it.isNotBlank() }
