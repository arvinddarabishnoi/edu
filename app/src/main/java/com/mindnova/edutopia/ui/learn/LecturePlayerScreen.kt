package com.mindnova.edutopia.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.South
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.SubjectChip
import com.mindnova.edutopia.core.components.YouTubePlayerView
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.ChemistryColor
import com.mindnova.edutopia.core.theme.MathematicsColor
import com.mindnova.edutopia.core.theme.PhysicsColor
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.core.utils.YouTubeUtils
import com.mindnova.edutopia.data.models.Lecture
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.LectureCompletionOutcome
import com.mindnova.edutopia.data.repository.LectureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LecturePlayerUiState(
    val lecture: Lecture? = null,
    val siblings: List<Lecture> = emptyList(),
    val isCompleted: Boolean = false,
    val markingMessage: String? = null,
    val loading: Boolean = true,
    val error: String? = null
)

class LecturePlayerViewModel @JvmOverloads constructor(
    private val lectureRepo: LectureRepository = LectureRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(LecturePlayerUiState())
    val state: StateFlow<LecturePlayerUiState> = _state.asStateFlow()

    private var loadedId: String = ""

    fun init(lectureId: String) {
        if (lectureId.isBlank() || lectureId == loadedId) return
        loadedId = lectureId
        _state.value = LecturePlayerUiState()
        viewModelScope.launch {
            try {
                val lecture = lectureRepo.getLecture(lectureId).getOrThrow()
                if (lecture == null) {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "This lecture no longer exists or was removed."
                    )
                    return@launch
                }
                val siblings = if (lecture.seriesId.isNotBlank()) {
                    lectureRepo.getLecturesForSeriesFlow(lecture.seriesId)
                        .first { it !is com.mindnova.edutopia.core.utils.Resource.Loading }
                        .let { r ->
                            if (r is com.mindnova.edutopia.core.utils.Resource.Success) r.data else emptyList()
                        }
                } else {
                    emptyList()
                }
                val uid = authRepo.currentUserId
                val completed = if (uid != null) {
                    lectureRepo.getCompletedLectureIds(uid).getOrDefault(emptySet())
                } else {
                    emptySet()
                }
                _state.value = _state.value.copy(
                    lecture = lecture,
                    siblings = siblings,
                    isCompleted = completed.contains(lectureId),
                    loading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.localizedMessage ?: "Could not load this lecture."
                )
            }
        }
    }

    fun markCompleted() {
        val lecture = _state.value.lecture ?: return
        val uid = authRepo.currentUserId
        if (uid == null) {
            _state.value = _state.value.copy(markingMessage = "Sign in to save your progress.")
            return
        }
        if (_state.value.isCompleted) {
            _state.value = _state.value.copy(markingMessage = "Already marked completed — XP awarded once.")
            return
        }
        viewModelScope.launch {
            lectureRepo.markLectureCompleted(uid, lecture.id).fold(
                onSuccess = { outcome ->
                    _state.value = _state.value.copy(
                        isCompleted = true,
                        markingMessage = when (outcome) {
                            is LectureCompletionOutcome.Awarded ->
                                "+${Constants.XP_PER_LECTURE} XP — lecture completed!"
                            is LectureCompletionOutcome.AlreadyCompleted ->
                                "Already completed; no duplicate XP."
                        }
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        markingMessage = "Could not save completion: ${e.localizedMessage}"
                    )
                }
            )
        }
    }

    fun prevId(): String? = neighborId(-1)
    fun nextId(): String? = neighborId(+1)

    private fun neighborId(offset: Int): String? {
        val current = _state.value.lecture ?: return null
        val ordered = _state.value.siblings.sortedBy { it.lectureNumber }
        val idx = ordered.indexOfFirst { it.id == current.id }
        if (idx < 0) return null
        return ordered.getOrNull(idx + offset)?.id
    }
}

@Composable
fun LecturePlayerScreen(
    navController: NavController,
    lectureId: String,
    viewModel: LecturePlayerViewModel = viewModel()
) {
    LaunchedEffect(lectureId) { viewModel.init(lectureId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .imePadding()
    ) {
        AppTopBar(
            title = state.lecture?.title ?: "Lecture",
            onBack = { navController.popBackStack() }
        )

        when {
            state.loading -> LoadingStateView()
            state.error != null -> ErrorStateView(
                title = "Lecture unavailable",
                errorMessage = state.error,
                onRetry = { viewModel.init(lectureId) }
            )
            else -> {
                val lecture = state.lecture ?: return@Column
                val videoId = YouTubeUtils.extractVideoId(
                    lecture.videoUrl.ifBlank { lecture.videoId }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (videoId.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Video unavailable",
                                    color = TextWhitePrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "This lecture has no valid YouTube link yet. Please contact your batch admin.",
                                    color = TextWhiteMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        YouTubePlayerView(
                            videoId = videoId,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Lecture ${lecture.lectureNumber}",
                                color = TextWhiteMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("•", color = TextWhiteMuted, fontSize = 12.sp)
                            Text(lecture.duration, color = TextWhiteMuted, fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            SubjectChip(
                                subject = lecture.subject.ifBlank { "General" },
                                color = when (lecture.subject) {
                                    "Physics" -> PhysicsColor
                                    "Chemistry" -> ChemistryColor
                                    "Mathematics" -> MathematicsColor
                                    else -> BrandIndigo
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            lecture.title,
                            color = TextWhitePrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 27.sp
                        )
                        if (lecture.seriesName.isNotBlank() || lecture.chapter.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                lecture.seriesName.ifBlank { lecture.chapter },
                                color = TextWhiteSecondary,
                                fontSize = 13.sp
                            )
                        }

                        if (lecture.description.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            EdutopiaCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(
                                        "About this lecture",
                                        color = TextWhitePrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        lecture.description,
                                        color = TextWhiteSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        if (state.isCompleted) {
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Completed",
                                    color = AccentEmerald,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (state.markingMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.markingMessage!!,
                                color = TextWhiteSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val prev = viewModel.prevId()
                            val next = viewModel.nextId()
                            OutlinedButton(
                                onClick = { prev?.let { navController.navigate(com.mindnova.edutopia.core.navigation.Screen.LecturePlayer.createRoute(it)) } },
                                enabled = prev != null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhiteSecondary)
                            ) {
                                Icon(Icons.Default.North, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Previous")
                            }
                            OutlinedButton(
                                onClick = { next?.let { navController.navigate(com.mindnova.edutopia.core.navigation.Screen.LecturePlayer.createRoute(it)) } },
                                enabled = next != null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhiteSecondary)
                            ) {
                                Text("Next")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.South, null, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        com.mindnova.edutopia.core.components.EdutopiaPrimaryButton(
                            text = if (state.isCompleted) "Completed ✓" else "Mark as Completed (+${Constants.XP_PER_LECTURE} XP)",
                            onClick = { viewModel.markCompleted() },
                            enabled = !state.isCompleted,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
