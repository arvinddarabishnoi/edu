package com.mindnova.edutopia.ui.tournament

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.AppTopBar
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.EdutopiaGradientCard
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.ProfileAvatar
import com.mindnova.edutopia.core.components.SecondaryButton
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.DateTimeUtils
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.Tournament
import com.mindnova.edutopia.data.models.TournamentParticipant
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.TournamentRepository
import com.mindnova.edutopia.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ─────────────────────────── List ───────────────────────────

data class TournamentListUiState(
    val tournaments: List<Tournament> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false
)

class TournamentListViewModel @JvmOverloads constructor(
    private val repo: TournamentRepository = TournamentRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TournamentListUiState())
    val state: StateFlow<TournamentListUiState> = _state

    private var loaded = false

    init {
        refresh()
    }

    fun refresh() {
        if (loaded) {
            _state.value = _state.value.copy(loading = true, error = null)
        }
        loaded = true
        viewModelScope.launch {
            repo.getTournamentsFlow().collect { resource ->
                _state.value = when (resource) {
                    is Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Error -> TournamentListUiState(loading = false, error = resource.message)
                    is Resource.Empty -> TournamentListUiState(loading = false, isEmpty = true)
                    is Resource.Success -> TournamentListUiState(
                        tournaments = resource.data,
                        loading = false
                    )
                }
            }
        }
    }
}

@Composable
fun TournamentListScreen(
    navController: NavController,
    viewModel: TournamentListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(title = "Tournaments", onBack = { navController.popBackStack() })
        when {
            state.loading -> LoadingStateView()
            state.error != null -> ErrorStateView(errorMessage = state.error, onRetry = { viewModel.refresh() })
            state.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No tournaments scheduled yet. Live tournaments announce big rewards!",
                    color = TextWhiteMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.tournaments, key = { it.id }) { t ->
                    TournamentCard(t) {
                        navController.navigate(Screen.TournamentDetail.createRoute(t.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentCard(tournament: Tournament, onClick: () -> Unit) {
    val statusColor = when (tournament.status) {
        "live" -> AccentRose
        "upcoming" -> AccentAmber
        else -> TextWhiteMuted
    }
    EdutopiaGradientCard(
        modifier = Modifier.fillMaxWidth(),
        gradientBrush = Brush.linearGradient(
            listOf(
                statusColor.copy(alpha = 0.16f),
                Color(0xFF0F172A)
            )
        ),
        borderColor = statusColor.copy(alpha = 0.35f),
        onClick = onClick
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tournament.title,
                    color = TextWhitePrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        tournament.status.uppercase(),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${tournament.totalQuestions} Qs • ${tournament.durationMinutes} min",
                    color = TextWhiteSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = AccentAmber, modifier = Modifier.size(13.dp))
                    Text(" ${tournament.prizePoolPoints} pts pool", color = AccentAmber, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, null, tint = TextWhiteSecondary, modifier = Modifier.size(13.dp))
                    Text(" ${tournament.participantCount}", color = TextWhiteSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// ─────────────────────────── Detail ───────────────────────────

data class TournamentDetailUiState(
    val tournament: Tournament? = null,
    val leaderboard: List<TournamentParticipant> = emptyList(),
    val joined: Boolean = false,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class TournamentDetailViewModel @JvmOverloads constructor(
    private val repo: TournamentRepository = TournamentRepository(),
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val tournamentIdFlow = MutableStateFlow("")
    private val _state = MutableStateFlow(TournamentDetailUiState())
    val state: StateFlow<TournamentDetailUiState> = _state

    fun load(id: String) {
        if (id.isBlank() || tournamentIdFlow.value == id) return
        tournamentIdFlow.value = id
        _state.value = TournamentDetailUiState()
        viewModelScope.launch {
            try {
                val t = repo.getTournament(id).getOrThrow()
                if (t == null) {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "This tournament no longer exists."
                    )
                    return@launch
                }
                _state.value = _state.value.copy(tournament = t, loading = false)
                repo.getTournamentLeaderboardFlow(id).collect { resource ->
                    when (resource) {
                        is Resource.Success -> _state.value = _state.value.copy(
                            leaderboard = rankParticipants(resource.data),
                            message = null
                        )
                        is Resource.Error -> _state.value = _state.value.copy(
                            error = resource.message
                        )
                        is Resource.Empty -> _state.value = _state.value.copy(leaderboard = emptyList())
                        is Resource.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.localizedMessage ?: "Failed to load tournament."
                )
            }
        }
    }

    fun join() {
        val s = _state.value
        val t = s.tournament ?: return
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            val user = userRepo.getUser(uid).getOrNull()
            repo.registerParticipant(
                tournamentId = t.id,
                userId = uid,
                userName = user?.name ?: "Student",
                userClass = user?.studentClass ?: ""
            ).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        busy = false,
                        joined = true,
                        message = "You're registered! The test starts at ${DateTimeUtils.formatDateTime(t.startTime)}."
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        busy = false,
                        message = "Could not register you: ${e.localizedMessage}"
                    )
                }
            )
        }
    }
}

/** Rank with tie handling by (score desc, time asc). Equal score+time -> equal rank. */
internal fun rankParticipants(list: List<TournamentParticipant>): List<TournamentParticipant> {
    val sorted = list.sortedWith(
        compareByDescending<TournamentParticipant> { it.score }
            .thenBy { it.timeTakenSeconds }
    )
    var lastKey: Pair<Int, Long>? = null
    var lastRank = 0
    return sorted.mapIndexed { index, p ->
        val key = p.score to p.timeTakenSeconds
        val rank = if (key == lastKey) lastRank else index + 1
        lastKey = key
        lastRank = rank
        p.copy(rank = rank)
    }
}

@Composable
fun TournamentDetailScreen(
    navController: NavController,
    tournamentId: String,
    viewModel: TournamentDetailViewModel = viewModel()
) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(title = "Tournament", onBack = { navController.popBackStack() })

        when {
            state.loading -> LoadingStateView()
            state.tournament == null -> ErrorStateView(
                errorMessage = state.error ?: "Tournament unavailable.",
                onRetry = { viewModel.load(tournamentId) }
            )
            else -> {
                val t = state.tournament!!
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        EdutopiaGradientCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    t.status.uppercase(),
                                    color = AccentAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    t.title,
                                    color = TextWhitePrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    t.description.ifBlank { t.entryRequirements },
                                    color = TextWhiteSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    InfoBit("Starts", DateTimeUtils.formatDateTime(t.startTime))
                                    InfoBit("Questions", "${t.totalQuestions}")
                                    InfoBit("Duration", "${t.durationMinutes} min")
                                }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            EdutopiaPrimaryButton(
                                text = if (state.joined) "Registered ✓" else "Register",
                                onClick = { viewModel.join() },
                                enabled = !state.joined && !state.busy,
                                isLoading = state.busy,
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryButton(
                                text = "Attempt Test",
                                onClick = {
                                    if (t.testId.isNotBlank()) {
                                        navController.navigate(Screen.TestInstructions.createRoute(t.testId))
                                    } else {
                                        state.message
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (state.message != null) {
                        item {
                            Text(
                                state.message!!,
                                color = AccentCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (state.error != null) {
                        item {
                            Text("⚠ ${state.error}", color = AccentRose, fontSize = 12.sp)
                        }
                    }

                    item {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Live Standings",
                            color = TextWhitePrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Ties share the same rank; faster completion breaks ties.",
                            color = TextWhiteMuted,
                            fontSize = 11.sp
                        )
                    }

                    if (state.leaderboard.isEmpty()) {
                        item {
                            EdutopiaCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        null,
                                        tint = TextWhiteMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "No entries yet. Be the first to register!",
                                        color = TextWhiteMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.leaderboard, key = { it.userId }) { p ->
                            EdutopiaCard(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF18233C)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "#${p.rank}",
                                        color = if (p.rank <= 3) AccentAmber else TextWhiteSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    ProfileAvatar(name = p.userName, size = 34.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            p.userName,
                                            color = TextWhitePrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            p.userClass.ifBlank { "JEE Aspirant" },
                                            color = TextWhiteMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        "${p.score}",
                                        color = AccentEmerald,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBit(label: String, value: String) {
    Column {
        Text(label, color = TextWhiteMuted, fontSize = 10.sp)
        Text(value, color = TextWhitePrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
