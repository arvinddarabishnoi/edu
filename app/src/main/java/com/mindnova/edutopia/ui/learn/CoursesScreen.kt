package com.mindnova.edutopia.ui.learn

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mindnova.edutopia.core.components.EdutopiaGradientCard
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.SearchField
import com.mindnova.edutopia.core.components.SubjectChip
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.ChemistryColor
import com.mindnova.edutopia.core.theme.MathematicsColor
import com.mindnova.edutopia.core.theme.PhysicsColor
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.Series
import com.mindnova.edutopia.data.repository.LectureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CoursesUiState(
    val query: String = "",
    val subject: String = "All",
    val targetClass: String = "All",
    val series: List<Series> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class CoursesViewModel @JvmOverloads constructor(
    private val lectureRepo: LectureRepository = LectureRepository()
) : ViewModel() {

    private val subjectFilter = MutableStateFlow("All")
    private val classFilter = MutableStateFlow("All")
    private val query = MutableStateFlow("")
    private val tick = MutableStateFlow(0)

    val uiState: StateFlow<CoursesUiState> = combine(
        lectureRepo.getSeriesFlow(),
        combine(subjectFilter, classFilter, query, tick) { s, c, q, t ->
            Quad(s, c, q, t)
        }
    ) { resource, filters ->
        val (subject, cls, q, _) = filters
        when (resource) {
            is Resource.Loading -> CoursesUiState(query = q, subject = subject, targetClass = cls, loading = true)
            is Resource.Error -> CoursesUiState(
                query = q, subject = subject, targetClass = cls,
                loading = false, error = resource.message
            )
            is Resource.Empty -> CoursesUiState(query = q, subject = subject, targetClass = cls, loading = false)
            is Resource.Success -> {
                val filtered = resource.data
                    .filter { subject == "All" || it.subject == subject }
                    .filter { cls == "All" || it.targetClass == cls || it.targetClass == "All" }
                    .filter {
                        q.isBlank() ||
                            it.title.contains(q, ignoreCase = true) ||
                            it.chapter.contains(q, ignoreCase = true)
                    }
                CoursesUiState(
                    query = q, subject = subject, targetClass = cls,
                    series = filtered, loading = false, error = null
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoursesUiState())

    fun setSubject(value: String) { subjectFilter.value = value }
    fun setClass(value: String) { classFilter.value = value }
    fun setQuery(value: String) { query.value = value }
    fun refresh() { tick.value = tick.value + 1 }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

@Composable
fun CoursesScreen(
    navController: NavController,
    viewModel: CoursesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Text(
            text = "Learn",
            color = TextWhitePrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        SearchField(
            query = uiState.query,
            onQueryChange = viewModel::setQuery,
            placeholder = "Search series or chapter",
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SubjectChip(
                    subject = "All",
                    color = BrandIndigo,
                    selected = uiState.subject == "All",
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
                    selected = uiState.subject == name,
                    onClick = { viewModel.setSubject(name) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("All", "Class 11", "Class 12", "Dropper")) { cls ->
                val selected = uiState.targetClass == cls
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) BrandIndigo else Color(0xFF1E293B))
                        .clickable { viewModel.setClass(cls) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        cls,
                        color = if (selected) Color.White else TextWhiteSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            uiState.loading -> LoadingStateView()
            uiState.error != null -> ErrorStateView(
                errorMessage = uiState.error,
                onRetry = { viewModel.refresh() }
            )
            uiState.series.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = TextWhiteMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No series match these filters yet.",
                        color = TextWhiteMuted,
                        fontSize = 13.sp
                    )
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.series, key = { it.id }) { series ->
                    SeriesCard(series = series) {
                        navController.navigate(Screen.SeriesDetail.createRoute(series.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesCard(series: Series, onClick: () -> Unit) {
    val subjectColor = when (series.subject) {
        "Physics" -> PhysicsColor
        "Chemistry" -> ChemistryColor
        "Mathematics" -> MathematicsColor
        else -> BrandIndigo
    }

    EdutopiaGradientCard(
        modifier = Modifier.fillMaxWidth(),
        gradientBrush = Brush.horizontalGradient(
            listOf(subjectColor.copy(alpha = 0.14f), Color(0xFF0F172A))
        ),
        borderColor = subjectColor.copy(alpha = 0.3f),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(subjectColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = subjectColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    series.title,
                    color = TextWhitePrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${series.subject} • ${series.chapter}",
                    color = TextWhiteSecondary,
                    fontSize = 12.sp
                )
                Text(
                    "${series.lectureCount} lectures • ${series.targetClass}",
                    color = TextWhiteMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = "Open series",
                tint = subjectColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
