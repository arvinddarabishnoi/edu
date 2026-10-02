package com.mindnova.edutopia.ui.student

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.DateTimeUtils
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.Announcement
import com.mindnova.edutopia.data.repository.AnnouncementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnnouncementsUiState(
    val items: List<Announcement> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false
)

class AnnouncementsViewModel @JvmOverloads constructor(
    private val repo: AnnouncementRepository = AnnouncementRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AnnouncementsUiState())
    val state: StateFlow<AnnouncementsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAnnouncementsFlow().collect { resource ->
                _state.value = when (resource) {
                    is Resource.Loading -> AnnouncementsUiState()
                    is Resource.Error -> AnnouncementsUiState(loading = false, error = resource.message)
                    is Resource.Empty -> AnnouncementsUiState(loading = false, isEmpty = true)
                    is Resource.Success -> AnnouncementsUiState(items = resource.data, loading = false)
                }
            }
        }
    }

    fun retry() {
        _state.value = AnnouncementsUiState(loading = true)
        viewModelScope.launch {
            repo.getAnnouncementsFlow().collect { resource ->
                _state.value = when (resource) {
                    is Resource.Error -> AnnouncementsUiState(loading = false, error = resource.message)
                    is Resource.Empty -> AnnouncementsUiState(loading = false, isEmpty = true)
                    is Resource.Success -> AnnouncementsUiState(items = resource.data, loading = false)
                    is Resource.Loading -> _state.value
                }
            }
        }
    }
}

@Composable
fun AnnouncementsScreen(
    navController: NavController,
    viewModel: AnnouncementsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(title = "Announcements", onBack = { navController.popBackStack() })

        when {
            state.loading -> LoadingStateView()
            state.error != null -> ErrorStateView(errorMessage = state.error, onRetry = { viewModel.retry() })
            state.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = null,
                        tint = TextWhiteMuted,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No announcements yet.",
                        color = TextWhiteMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items, key = { it.id }) { a ->
                    val priorityColor = when (a.priority) {
                        "high" -> AccentRose
                        "low" -> TextWhiteMuted
                        else -> AccentCyan
                    }
                    EdutopiaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF18233C),
                        borderColor = priorityColor.copy(alpha = 0.35f)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(priorityColor)
                                )
                                Spacer(Modifier.height(0.dp))
                                Text(
                                    "  ${a.title}",
                                    color = TextWhitePrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (a.priority == "high") {
                                    Text(
                                        " URGENT",
                                        color = AccentRose,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                a.description,
                                color = TextWhiteSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                DateTimeUtils.formatDateTime(a.createdAt),
                                color = TextWhiteMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
