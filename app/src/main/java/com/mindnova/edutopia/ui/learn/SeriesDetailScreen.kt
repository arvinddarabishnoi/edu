package com.mindnova.edutopia.ui.learn

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.mindnova.edutopia.core.components.SkeletonCard
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.ChemistryColor
import com.mindnova.edutopia.core.theme.MathematicsColor
import com.mindnova.edutopia.core.theme.PhysicsColor
import com.mindnova.edutopia.core.theme.SurfaceBorderDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.LectureRepository
import com.mindnova.edutopia.data.models.Lecture
import com.mindnova.edutopia.data.models.Series
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SeriesDetailUiState(
    val series: Series? = null,
    val lectures: List<Lecture> = emptyList(),
    val completedIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null,
    val notFound: Boolean = false
)

class SeriesDetailViewModel @JvmOverloads constructor(
    private val lectureRepo: LectureRepository = LectureRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(SeriesDetailUiState())
    val state: StateFlow<SeriesDetailUiState> = _state

    private var loadedId: String = ""

    fun init(id: String) {
        if (id.isBlank() || id == loadedId) return
        loadedId = id
        _state.value = SeriesDetailUiState()
        viewModelScope.launch { loadSeries(id) }
    }

    private fun loadSeries(id: String) {
        viewModelScope.launch {
            var seriesResult: Resource<List<com.mindnova.edutopia.data.models.Series>> = Resource.Loading
            var lecturesResult: Resource<List<Lecture>> = Resource.Loading
            var completed: Set<String> = emptySet()
            var errors = mutableListOf<String>()

            val jobs = listOf(
                launch {
                    lectureRepo.getSeriesFlow().collect { r ->
                        seriesResult = r
                        if (r is Resource.Error) errors += r.message
                        refresh(id, seriesResult, lecturesResult, completed, errors)
                    }
                },
                launch {
                    lectureRepo.getLecturesForSeriesFlow(id).collect { r ->
                        lecturesResult = r
                        if (r is Resource.Error) errors += r.message
                        refresh(id, seriesResult, lecturesResult, completed, errors)
                    }
                },
                launch {
                    val uid = authRepo.currentUserId
                    if (uid == null) {
                        completed = emptySet()
                        refresh(id, seriesResult, lecturesResult, completed, errors)
                    } else {
                        lectureRepo.getCompletedLectureIdsFlow(uid).collect { r ->
                            when (r) {
                                is Resource.Success -> completed = r.data
                                is Resource.Error -> errors += r.message
                                else -> Unit
                            }
                            refresh(id, seriesResult, lecturesResult, completed, errors)
                        }
                    }
                }
            )
        }
    }

    private fun refresh(
        id: String,
        seriesResult: Resource<List<com.mindnova.edutopia.data.models.Series>>,
        lecturesResult: Resource<List<Lecture>>,
        completed: Set<String>,
        errors: List<String>
    ) {
        val error = errors.firstOrNull()
        val series = (seriesResult as? Resource.Success)?.data?.firstOrNull { it.id == id }
        val lectures = (lecturesResult as? Resource.Success)?.data.orEmpty()
        _state.value = when {
            error != null && series == null && lectures.isEmpty() ->
                SeriesDetailUiState(loading = false, error = error)
            lecturesResult is Resource.Loading || seriesResult is Resource.Loading ->
                SeriesDetailUiState(loading = true)
            else -> SeriesDetailUiState(
                series = series,
                lectures = lectures.sortedWith(
                    compareBy<Lecture> { it.publishStatus != "published" }.thenBy { it.lectureNumber }
                ),
                completedIds = completed,
                loading = false,
                notFound = series == null && lecturesResult !is Resource.Loading
            )
        }
    }
}

@Composable
fun SeriesDetailScreen(
    navController: NavController,
    seriesId: String,
    viewModel: SeriesDetailViewModel = viewModel()
) {
    LaunchedEffect(seriesId) { viewModel.init(seriesId) }
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(title = "Series", onBack = { navController.popBackStack() })

        when {
            uiState.loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonCard(height = 120.dp)
                    SkeletonCard()
                    SkeletonCard()
                }
            }
            uiState.error != null -> ErrorStateView(
                errorMessage = uiState.error,
                onRetry = { viewModel.init(seriesId) }
            )
            uiState.notFound || uiState.series == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "This series is no longer available. It may have been removed or unpublished.",
                    color = TextWhiteMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(32.dp)
                )
            }
            else -> {
                val series = uiState.series!!
                val subjectColor = when (series.subject) {
                    "Physics" -> PhysicsColor
                    "Chemistry" -> ChemistryColor
                    "Mathematics" -> MathematicsColor
                    else -> BrandIndigo
                }
                val completedCount = uiState.lectures.count { it.id in uiState.completedIds }

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(subjectColor.copy(alpha = 0.35f), Color(0xFF0F172A))
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Text(
                                    series.title,
                                    color = TextWhitePrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${series.subject} • ${series.chapter} • ${series.targetClass}",
                                    color = TextWhiteSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (series.description.isNotBlank()) {
                        item {
                            Text(
                                series.description,
                                color = TextWhiteSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    item {
                        EdutopiaCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Your progress",
                                        color = TextWhiteMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "$completedCount of ${uiState.lectures.size} completed",
                                        color = TextWhitePrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "${(if (uiState.lectures.isEmpty()) 0 else completedCount * 100 / uiState.lectures.size)}%",
                                    color = AccentEmerald,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    itemsIndexed(uiState.lectures, key = { _, it -> it.id }) { index, lecture ->
                        val completed = lecture.id in uiState.completedIds
                        LectureRow(
                            number = index + 1,
                            lecture = lecture,
                            completed = completed,
                            onClick = {
                                navController.navigate(Screen.LecturePlayer.createRoute(lecture.id))
                            }
                        )
                    }

                    if (uiState.lectures.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No lectures published in this series yet.",
                                    color = TextWhiteMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LectureRow(
    number: Int,
    lecture: Lecture,
    completed: Boolean,
    onClick: () -> Unit
) {
    EdutopiaCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (completed) AccentEmerald.copy(alpha = 0.4f) else SurfaceBorderDark,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (completed) AccentEmerald.copy(alpha = 0.15f)
                        else BrandIndigo.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (completed) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = AccentEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        "$number",
                        color = BrandIndigo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    lecture.title,
                    color = TextWhitePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${lecture.duration} • ${lecture.chapter.ifBlank { lecture.subject }}",
                    color = TextWhiteMuted,
                    fontSize = 11.sp
                )
            }
            if (lecture.publishStatus == "draft") {
                Icon(Icons.Default.Lock, "Draft — not published", tint = TextWhiteMuted, modifier = Modifier.size(16.dp))
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play lecture",
                    tint = AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
