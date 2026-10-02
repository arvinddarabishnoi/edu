package com.mindnova.edutopia.ui.leaderboard

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
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import com.mindnova.edutopia.core.components.ProfileAvatar
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Rank rows with proper tie handling: equal points share the same displayed
 * rank, and the next distinct score jumps accordingly (1, 2, 2, 4).
 */
data class RankedUser(val user: User, val rank: Int, val isMe: Boolean)

internal fun rankUsers(users: List<User>, currentUid: String?): List<RankedUser> {
    val sorted = users.sortedWith(
        compareByDescending<User> { it.points }
            .thenByDescending { it.xp }
            .thenBy { it.uid } // stable tie-break, deterministic
    )
    var lastPoints: Long? = null
    var lastRank = 0
    return sorted.mapIndexed { index, user ->
        val rank = if (lastPoints != null && user.points == lastPoints) lastRank else index + 1
        lastPoints = user.points
        lastRank = rank
        RankedUser(user, rank, user.uid == currentUid)
    }
}

data class LeaderboardUiState(
    val ranked: List<RankedUser> = emptyList(),
    val me: RankedUser? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false
)

class LeaderboardViewModel @JvmOverloads constructor(
    private val userRepo: UserRepository = UserRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    val uiState: StateFlow<LeaderboardUiState> = userRepo.getLeaderboardFlow(50)
        .map { resource ->
            when (resource) {
                is Resource.Loading -> LeaderboardUiState()
                is Resource.Error -> LeaderboardUiState(loading = false, error = resource.message)
                is Resource.Empty -> LeaderboardUiState(loading = false, isEmpty = true)
                is Resource.Success -> {
                    val me = authRepo.currentUserId
                    val ranked = rankUsers(resource.data, me)
                    LeaderboardUiState(
                        ranked = ranked,
                        me = ranked.firstOrNull { it.isMe },
                        loading = false
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeaderboardUiState())
}

@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: LeaderboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        AppTopBar(
            title = "Rankings",
            subtitle = "All-time leaderboard by points"
        )

        when {
            state.loading -> LoadingStateView()
            state.error != null -> ErrorStateView(
                errorMessage = state.error,
                onRetry = { /* live flow retries automatically on reconnect */ }
            )
            state.isEmpty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = TextWhiteMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "The leaderboard is empty — students appear as soon as they start earning points.",
                        color = TextWhiteMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            }
            else -> {
                // Podium for top 3
                val top3 = state.ranked.take(3)
                if (top3.size >= 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        PodiumCard(top3[1], 2, Modifier.weight(1f))
                        PodiumCard(top3[0], 1, Modifier.weight(1.1f))
                        PodiumCard(top3[2], 3, Modifier.weight(1f))
                    }
                }

                state.me?.let { me ->
                    if (me.rank > 3) {
                        EdutopiaCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            backgroundColor = BrandIndigo.copy(alpha = 0.12f),
                            borderColor = BrandIndigo.copy(alpha = 0.4f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("#${me.rank}", color = AccentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(14.dp))
                                ProfileAvatar(name = me.user.name, photoUrl = me.user.photoUrl, size = 34.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("You", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        me.user.name.ifBlank { "Student" },
                                        color = TextWhitePrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                Text("${me.user.points} pts", color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.ranked.drop(3), key = { it.user.uid }) { entry ->
                        RankRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumCard(entry: RankedUser, place: Int, modifier: Modifier = Modifier) {
    val medal = when (place) {
        1 -> AccentAmber
        2 -> Color(0xFF94A3B8)
        else -> Color(0xFFB45309)
    }
    EdutopiaCard(
        modifier = modifier,
        backgroundColor = Color(0xFF18233C),
        borderColor = medal.copy(alpha = 0.45f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (place == 1) "🥇" else if (place == 2) "🥈" else "🥉",
                fontSize = 22.sp
            )
            Spacer(Modifier.height(6.dp))
            ProfileAvatar(name = entry.user.name, photoUrl = entry.user.photoUrl, size = 40.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                entry.user.name.ifBlank { "Student" }.split(" ").take(2).joinToString(" "),
                color = TextWhitePrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text("${entry.user.points} pts", color = medal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Lv ${entry.user.level}", color = TextWhiteMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun RankRow(entry: RankedUser) {
    EdutopiaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (entry.isMe) BrandIndigo.copy(alpha = 0.14f) else Color(0xFF18233C),
        borderColor = if (entry.isMe) BrandIndigo.copy(alpha = 0.5f) else Color(0xFF2B3C62)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${entry.rank}",
                color = if (entry.rank <= 3) AccentAmber else TextWhiteSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(42.dp)
            )
            ProfileAvatar(name = entry.user.name, photoUrl = entry.user.photoUrl, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.user.name.ifBlank { "Student" },
                    color = TextWhitePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.user.studentClass.ifBlank { "JEE Aspirant" },
                        color = TextWhiteMuted,
                        fontSize = 11.sp
                    )
                    if (entry.user.streak > 1) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(" ${entry.user.streak}", color = AccentAmber, fontSize = 10.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.user.points} pts", color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${entry.user.xp} XP", color = TextWhiteSecondary, fontSize = 10.sp)
            }
        }
    }
}
