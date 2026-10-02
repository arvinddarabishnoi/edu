package com.mindnova.edutopia.ui.pyq

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
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
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.SkeletonCard
import com.mindnova.edutopia.core.components.SubjectChip
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.ChemistryColor
import com.mindnova.edutopia.core.theme.MathematicsColor
import com.mindnova.edutopia.core.theme.PhysicsColor
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.PyqQuestion
import com.mindnova.edutopia.data.repository.PyqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PyqFilters(
    val examType: String = "All",
    val subject: String = "All",
    val year: Int = 0,
    val shift: String = "All",
    val difficulty: String = "All",
    val chapter: String = ""
)

data class PyqListUiState(
    val filters: PyqFilters = PyqFilters(),
    val questions: List<PyqQuestion> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false
)

class PyqListViewModel @JvmOverloads constructor(
    private val pyqRepo: PyqRepository = PyqRepository()
) : ViewModel() {

    private val filters = MutableStateFlow(PyqFilters())
    private val tick = MutableStateFlow(0)

    val uiState: StateFlow<PyqListUiState> = combine(
        pyqRepo.getPyqsFlow(examType = "All"),
        combine(filters, tick) { f, t -> f to t }
    ) { resource, (f, _) ->
        when (resource) {
            is Resource.Loading -> PyqListUiState(filters = f, loading = true)
            is Resource.Error -> PyqListUiState(filters = f, loading = false, error = resource.message)
            is Resource.Empty -> PyqListUiState(filters = f, loading = false, isEmpty = true)
            is Resource.Success -> {
                val filtered = resource.data
                    .filter { f.examType == "All" || it.examType == f.examType }
                    .filter { f.subject == "All" || it.subject == f.subject }
                    .filter { f.year == 0 || it.year == f.year }
                    .filter { f.shift == "All" || it.shift == f.shift }
                    .filter { f.difficulty == "All" || it.difficulty == f.difficulty }
                    .filter {
                        f.chapter.isBlank() ||
                            it.chapter.contains(f.chapter, ignoreCase = true) ||
                            it.topic.contains(f.chapter, ignoreCase = true)
                    }
                PyqListUiState(
                    filters = f,
                    questions = filtered,
                    loading = false,
                    isEmpty = filtered.isEmpty()
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PyqListUiState())

    fun setExamType(v: String) { filters.value = filters.value.copy(examType = v) }
    fun setSubject(v: String) { filters.value = filters.value.copy(subject = v) }
    fun setYear(v: Int) { filters.value = filters.value.copy(year = v) }
    fun setShift(v: String) { filters.value = filters.value.copy(shift = v) }
    fun setDifficulty(v: String) { filters.value = filters.value.copy(difficulty = v) }
    fun setChapter(v: String) { filters.value = filters.value.copy(chapter = v) }
    fun refresh() { tick.value = tick.value + 1 }
}

@Composable
fun PyqListScreen(
    navController: NavController,
    viewModel: PyqListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(title = "PYQ Practice", onBack = { navController.popBackStack() })

        Text(
            "Filter previous-year JEE questions, then start a practice session. " +
                "Solving a question the first time earns XP — repeats are tracked but never farmable.",
            color = TextWhiteSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("All", "JEE Main", "JEE Advanced")) { exam ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (state.filters.examType == exam) BrandIndigo else Color(0xFF1E293B)
                        )
                        .clickable { viewModel.setExamType(exam) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        exam,
                        color = if (state.filters.examType == exam) Color.White else TextWhiteSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (state.filters.examType == exam) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SubjectChip(
                    subject = "All",
                    color = BrandIndigo,
                    selected = state.filters.subject == "All",
                    onClick = { viewModel.setSubject("All") }
                )
            }
            items(
                listOf(
                    "Physics" to PhysicsColor,
                    "Chemistry" to ChemistryColor,
                    "Mathematics" to MathematicsColor
                )
            ) { (name, color) ->
                SubjectChip(
                    subject = name,
                    color = color,
                    selected = state.filters.subject == name,
                    onClick = { viewModel.setSubject(name) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                YearChip("All Years", state.filters.year == 0) { viewModel.setYear(0) }
            }
            items(listOf(2025, 2024, 2023, 2022, 2021)) { y ->
                YearChip(y.toString(), state.filters.year == y) { viewModel.setYear(y) }
            }
        }

        Spacer(Modifier.height(14.dp))

        when {
            state.loading -> Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkeletonCard()
                SkeletonCard()
                SkeletonCard()
            }
            state.error != null -> ErrorStateView(
                errorMessage = state.error,
                onRetry = { viewModel.refresh() }
            )
            state.isEmpty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = TextWhiteMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No PYQs match these filters yet.",
                        color = TextWhiteMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${state.questions.size} questions",
                        color = TextWhiteSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    com.mindnova.edutopia.core.components.EdutopiaPrimaryButton(
                        text = "Start practice",
                        onClick = {
                            navController.navigate(
                                Screen.PyqPractice.createRoute(
                                    subject = state.filters.subject,
                                    year = state.filters.year
                                )
                            )
                        },
                        modifier = Modifier.width(150.dp),
                        height = 38.dp
                    )
                }
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.questions.take(40), key = { it.id }) { q ->
                        PyqMiniCard(q)
                    }
                }
            }
        }
    }
}

@Composable
private fun YearChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentCyan.copy(alpha = 0.2f) else Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) AccentCyan else TextWhiteSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PyqMiniCard(q: PyqQuestion) {
    EdutopiaCard(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF18233C)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    q.questionText,
                    color = TextWhitePrimary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${q.year} • ${q.shift} • ${q.chapter}",
                    color = TextWhiteMuted,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    q.difficulty,
                    color = when (q.difficulty) {
                        "Easy" -> AccentEmerald
                        "Hard" -> AccentRose
                        else -> AccentAmber
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Acc ${q.accuracy.toInt()}%",
                    color = TextWhiteSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
