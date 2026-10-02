package com.mindnova.edutopia.ui.tests

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsTournament
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.SearchField
import com.mindnova.edutopia.core.components.SkeletonCard
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.PhysicsColor
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.TestModel
import com.mindnova.edutopia.data.repository.TestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TestsUiState(
    val tests: List<TestModel> = emptyList(),
    val query: String = "",
    val category: String = "All",
    val loading: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false
)

class TestsListViewModel @JvmOverloads constructor(
    private val testRepo: TestRepository = TestRepository()
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow("All")
    private val tick = MutableStateFlow(0)

    val uiState: StateFlow<TestsUiState> = combine(
        testRepo.getPublishedTestsFlow(),
        combine(query, category, tick) { q, c, t -> Triple(q, c, t) }
    ) { resource, (q, cat, _) ->
        when (resource) {
            is Resource.Loading -> TestsUiState(query = q, category = cat, loading = true)
            is Resource.Error -> TestsUiState(query = q, category = cat, loading = false, error = resource.message)
            is Resource.Empty -> TestsUiState(query = q, category = cat, loading = false, isEmpty = true)
            is Resource.Success -> {
                val filtered = resource.data
                    .filter { cat == "All" || it.category == cat }
                    .filter { q.isBlank() || it.title.contains(q, ignoreCase = true) || it.subject.contains(q, ignoreCase = true) }
                TestsUiState(
                    tests = filtered,
                    query = q,
                    category = cat,
                    loading = false,
                    isEmpty = filtered.isEmpty()
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TestsUiState())

    fun setQuery(value: String) { query.value = value }
    fun setCategory(value: String) { category.value = value }
    fun refresh() { tick.value = tick.value + 1 }
}

private val testCategories = listOf("All", "JEE Main AITS", "JEE Advanced AITS", "PYQ", "Custom Test")

@Composable
fun TestsListScreen(
    navController: NavController,
    viewModel: TestsListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Text(
            "Tests",
            color = TextWhitePrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // Quick access row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EdutopiaCard(
                modifier = Modifier.weight(1f),
                backgroundColor = AccentEmerald.copy(alpha = 0.1f),
                borderColor = AccentEmerald.copy(alpha = 0.3f),
                onClick = { navController.navigate(Screen.PyqList.route) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Quiz, null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(0.dp))
                    Text(
                        "  PYQ Practice",
                        color = TextWhitePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            EdutopiaCard(
                modifier = Modifier.weight(1f),
                backgroundColor = AccentAmber.copy(alpha = 0.1f),
                borderColor = AccentAmber.copy(alpha = 0.3f),
                onClick = { navController.navigate(Screen.TournamentList.route) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SportsTournament,
                        null,
                        tint = AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "  Tournaments",
                        color = TextWhitePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SearchField(
            query = uiState.query,
            onQueryChange = viewModel::setQuery,
            placeholder = "Search tests",
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(testCategories) { cat ->
                val selected = uiState.category == cat
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) AccentCyan.copy(alpha = 0.18f) else Color(0xFF1E293B),
                            androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .clickable { viewModel.setCategory(cat) }
                ) {
                    Text(
                        cat,
                        color = if (selected) AccentCyan else TextWhiteSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            uiState.loading -> Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkeletonCard()
                SkeletonCard()
                SkeletonCard()
            }
            uiState.error != null -> ErrorStateView(
                errorMessage = uiState.error,
                onRetry = { viewModel.refresh() }
            )
            uiState.isEmpty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TextWhiteMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (uiState.query.isNotBlank() || uiState.category != "All")
                            "No tests match your filters."
                        else "No published tests yet. Check back soon!",
                        color = TextWhiteMuted,
                        fontSize = 13.sp
                    )
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.tests, key = { it.id }) { test ->
                    TestListItem(
                        test = test,
                        onClick = { navController.navigate(Screen.TestInstructions.createRoute(test.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TestListItem(test: TestModel, onClick: () -> Unit) {
    EdutopiaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF18233C),
        onClick = onClick
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    test.title,
                    color = TextWhitePrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                Text(
                    test.category,
                    color = AccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            AccentCyan.copy(alpha = 0.12f),
                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(test.subject, color = PhysicsColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${test.totalQuestions} Qs", color = TextWhiteSecondary, fontSize = 12.sp)
                Text("${test.durationMinutes} min", color = TextWhiteSecondary, fontSize = 12.sp)
                Text(
                    "+${test.marksPerQuestion} / -${test.negativeMarks}",
                    color = AccentAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
