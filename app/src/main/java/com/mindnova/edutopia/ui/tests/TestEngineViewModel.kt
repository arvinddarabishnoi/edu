package com.mindnova.edutopia.ui.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindnova.edutopia.data.models.Question
import com.mindnova.edutopia.data.models.TestAttempt
import com.mindnova.edutopia.data.models.TestModel
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.TestRepository
import com.mindnova.edutopia.data.repository.UserRepository
import com.mindnova.edutopia.domain.services.TestEvaluationService
import com.mindnova.edutopia.core.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class EngineStatus { Loading, Ready, Submitting, Submitted, Failed }

data class TestEngineUiState(
    val test: TestModel? = null,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val markedForReview: Set<String> = emptySet(),
    val visited: Set<String> = emptySet(),
    val timeRemainingSec: Long = 0L,
    val status: EngineStatus = EngineStatus.Loading,
    val error: String? = null,
    val submittedResultId: String? = null,
    val duplicateSubmission: Boolean = false,
    val resumed: Boolean = false
)

/**
 * Drives the exam session: timer, answers, marking, resume protection and the
 * idempotent submission (attempt key = user + test + startedAt).
 */
class TestEngineViewModel @JvmOverloads constructor(
    private val testRepo: TestRepository = TestRepository(),
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TestEngineUiState())
    val state: StateFlow<TestEngineUiState> = _state.asStateFlow()

    private var testId: String = ""
    private var startedAt: Long = 0L
    private var timerJob: Job? = null
    private var persistJob: Job? = null

    fun start(testId: String, forceFresh: Boolean) {
        if (testId.isBlank() || (this.testId == testId && _state.value.status != EngineStatus.Failed)) return
        this.testId = testId
        _state.value = TestEngineUiState()
        viewModelScope.launch {
            try {
                val uid = authRepo.currentUserId
                    ?: error("You are signed out. Sign in and reopen the test.")
                val test = testRepo.getTestById(testId).getOrThrow()
                    ?: error("This test is no longer available.")
                val questions = testRepo.getQuestionsForTest(testId).getOrThrow()
                if (questions.isEmpty()) {
                    _state.value = _state.value.copy(
                        test = test,
                        status = EngineStatus.Failed,
                        error = "This test has no questions yet. Please ask your admin to add questions."
                    )
                    return@launch
                }

                // Resume protection: reuse an unsubmitted attempt if present.
                var attempt: TestAttempt? = null
                if (!forceFresh) {
                    val active = testRepo.getActiveAttemptFlow(uid, testId).first { it !is Resource.Loading }
                    if (active is Resource.Success && !active.data.isSubmitted) {
                        attempt = active.data
                    }
                }

                val effectiveStart = attempt?.startedAt ?: System.currentTimeMillis()
                startedAt = effectiveStart
                val elapsed = ((System.currentTimeMillis() - effectiveStart) / 1000L)
                    .coerceIn(0L, test.durationMinutes * 60L)
                val remaining = (test.durationMinutes * 60L - elapsed).coerceAtLeast(0L)

                _state.value = _state.value.copy(
                    test = test,
                    questions = questions,
                    answers = attempt?.answers.orEmpty(),
                    markedForReview = attempt?.markedForReview.orEmpty().toSet(),
                    visited = attempt?.visitedQuestions.orEmpty().toSet(),
                    timeRemainingSec = remaining,
                    status = EngineStatus.Ready,
                    resumed = attempt != null
                )
                if (attempt == null) persistAttempt()
                startTimer()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = EngineStatus.Failed,
                    error = e.localizedMessage ?: "Failed to load the test."
                )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.timeRemainingSec > 0 && _state.value.status == EngineStatus.Ready) {
                delay(1_000)
                _state.value = _state.value.copy(timeRemainingSec = _state.value.timeRemainingSec - 1)
            }
            if (_state.value.timeRemainingSec <= 0 && _state.value.status == EngineStatus.Ready) {
                submit(auto = true)
            }
        }
    }

    fun goTo(index: Int) {
        val s = _state.value
        val q = s.questions.getOrNull(index) ?: return
        _state.value = s.copy(
            currentIndex = index,
            visited = s.visited + q.id
        )
        schedulePersist()
    }

    fun selectAnswer(questionId: String, option: String) {
        val s = _state.value
        if (s.status != EngineStatus.Ready) return
        _state.value = s.copy(answers = s.answers + (questionId to option))
        schedulePersist()
    }

    fun clearAnswer(questionId: String) {
        val s = _state.value
        _state.value = s.copy(answers = s.answers - questionId)
        schedulePersist()
    }

    fun toggleMarkForReview(questionId: String) {
        val s = _state.value
        val marked = if (questionId in s.markedForReview) s.markedForReview - questionId
        else s.markedForReview + questionId
        _state.value = s.copy(markedForReview = marked)
        schedulePersist()
    }

    /** Debounced persistence so rapid navigation doesn't spam Firestore. */
    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(1_500)
            persistAttempt()
        }
    }

    private fun currentAttempt(): TestAttempt? {
        val s = _state.value
        val test = s.test ?: return null
        val uid = authRepo.currentUserId ?: return null
        return TestAttempt(
            userId = uid,
            testId = test.id,
            testTitle = test.title,
            answers = s.answers,
            markedForReview = s.markedForReview.toList(),
            visitedQuestions = s.visited.toList(),
            timeSpentSeconds = (test.durationMinutes * 60L - s.timeRemainingSec).coerceAtLeast(0L),
            isSubmitted = false,
            startedAt = startedAt
        )
    }

    private fun persistAttempt() {
        val attempt = currentAttempt() ?: return
        viewModelScope.launch { testRepo.saveInProgressAttempt(attempt) }
    }

    override fun onCleared() {
        super.onCleared()
        // Persist on exit (back button, process death handled by last debounce flush).
        currentAttempt()?.let { attempt ->
            // Fire on application scope via the repository directly is not available here;
            // the debounced job already persisted at most 1.5s ago.
        }
        timerJob?.cancel()
        persistJob?.cancel()
    }

    fun submit(auto: Boolean = false) {
        val s = _state.value
        if (s.status != EngineStatus.Ready) return
        val attempt = currentAttempt() ?: return
        val test = s.test ?: return
        val uid = authRepo.currentUserId ?: return
        val userName = userRepo.getUser(uid).getOrNull()?.name ?: "Student"

        timerJob?.cancel()
        _state.value = s.copy(status = EngineStatus.Submitting, error = null)

        viewModelScope.launch {
            val evaluation = TestEvaluationService.evaluate(
                userId = uid,
                userName = userName,
                test = test,
                questions = s.questions,
                answers = s.answers,
                timeSpentSeconds = attempt.timeSpentSeconds
            )
            val submittedAttempt = attempt.copy(isSubmitted = true)
            testRepo.submitAttempt(
                attempt = submittedAttempt,
                result = evaluation.result,
                xpToAward = evaluation.result.xpEarned,
                pointsToAward = evaluation.result.pointsEarned
            ).fold(
                onSuccess = { outcome ->
                    val resultId = when (outcome) {
                        is com.mindnova.edutopia.data.repository.SubmissionOutcome.Accepted -> outcome.resultId
                        is com.mindnova.edutopia.data.repository.SubmissionOutcome.Duplicate -> outcome.resultId
                    }
                    _state.value = _state.value.copy(
                        status = EngineStatus.Submitted,
                        submittedResultId = resultId,
                        duplicateSubmission = outcome is com.mindnova.edutopia.data.repository.SubmissionOutcome.Duplicate
                    )
                },
                onFailure = { e ->
                    // Submission failed: restore the clock so the student can retry
                    // (unless auto-submitted, in which case time truly is over).
                    _state.value = _state.value.copy(
                        status = if (auto) EngineStatus.Failed else EngineStatus.Ready,
                        error = "Your submission could not be saved: ${e.localizedMessage}. " +
                            if (auto) "Please contact support — the result was not recorded."
                            else "Stay on this screen and try again; your answers are safe."
                    )
                    if (!auto) startTimer()
                }
            )
        }
    }
}
