package com.mindnova.edutopia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.firstSettled
import com.mindnova.edutopia.data.models.Announcement
import com.mindnova.edutopia.data.models.DailyGoal
import com.mindnova.edutopia.data.models.Series
import com.mindnova.edutopia.data.models.TestResult
import com.mindnova.edutopia.data.models.User
import com.mindnova.edutopia.data.repository.AnnouncementRepository
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.BatchRepository
import com.mindnova.edutopia.data.repository.DailyGoalRepository
import com.mindnova.edutopia.data.repository.LectureRepository
import com.mindnova.edutopia.data.repository.TestRepository
import com.mindnova.edutopia.data.repository.UserRepository
import com.mindnova.edutopia.domain.services.GamificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: User? = null,
    val batchName: String? = null,
    val dailyGoal: DailyGoal? = null,
    val announcements: List<Announcement> = emptyList(),
    val recentSeries: List<Series> = emptyList(),
    val recentResults: List<TestResult> = emptyList(),
    val topStudents: List<User> = emptyList(),
    val levelProgress: Float = 0f,
    val xpToNextLevel: Long = 0L,
    val loading: Boolean = true,
    val error: String? = null
)

class HomeViewModel @JvmOverloads constructor(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val dailyGoalRepo: DailyGoalRepository = DailyGoalRepository(),
    private val announcementRepo: AnnouncementRepository = AnnouncementRepository(),
    private val batchRepo: BatchRepository = BatchRepository(),
    private val lectureRepo: LectureRepository = LectureRepository(),
    private val testRepo: TestRepository = TestRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            val userId = authRepo.currentUserId
            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "You are signed out. Please sign in again."
                )
                return@launch
            }

            // User profile: a real error must be visible, not an empty dashboard.
            when (val userResource = userRepo.getUserFlow(userId).firstSettled()) {
                is Resource.Loading -> Unit // unreachable: firstSettled suspends past Loading
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = userResource.message)
                    return@launch
                }
                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Your profile document is missing. Open Profile Setup to complete it."
                    )
                    return@launch
                }
                is Resource.Success -> {
                    val user = userResource.data
                    val (progress, remaining) = GamificationService.calculateLevelProgress(user.xp)

                    // Secondary sections: each failure is non-fatal but surfaced as
                    // a warning-free "not available" (section hidden), never faked.
                    val dailyGoal = dailyGoalRepo.getTodayGoalFlow().firstSettled().let {
                        (it as? Resource.Success)?.data
                    }
                    val announcements = announcementRepo.getAnnouncementsFlow().firstSettled().let {
                        (it as? Resource.Success)?.data.orEmpty()
                    }
                    val series = lectureRepo.getSeriesFlow().firstSettled().let {
                        (it as? Resource.Success)?.data.orEmpty()
                    }
                    val results = testRepo.getResultsForUserFlow(userId).firstSettled().let {
                        (it as? Resource.Success)?.data.orEmpty()
                    }
                    val topStudents = userRepo.getLeaderboardFlow(10).firstSettled().let {
                        (it as? Resource.Success)?.data?.take(3).orEmpty()
                    }
                    val batchName = if (user.batchId.isNotBlank()) {
                        batchRepo.getBatchById(user.batchId).getOrNull()?.name
                    } else null

                    // Keep the profile fresh (streak / lastActive) once per entry.
                    userRepo.updateLastActiveAndStreak(userId)

                    _uiState.value = HomeUiState(
                        user = user,
                        batchName = batchName,
                        dailyGoal = dailyGoal,
                        announcements = announcements.take(3),
                        recentSeries = series.take(6),
                        recentResults = results.take(3),
                        topStudents = topStudents,
                        levelProgress = progress,
                        xpToNextLevel = remaining,
                        loading = false,
                        error = null
                    )
                }
            }
        }
    }
}
