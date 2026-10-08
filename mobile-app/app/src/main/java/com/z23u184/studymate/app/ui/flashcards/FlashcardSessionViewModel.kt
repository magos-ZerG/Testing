package com.z23u184.studymate.app.ui.flashcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.navigation.NavArguments
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.domain.model.FlashcardBestResult
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.usecase.GetFlashcardBestResultUseCase
import com.z23u184.studymate.domain.usecase.GetFlashcardsByTopicUseCase
import com.z23u184.studymate.domain.usecase.GetTopicByIdUseCase
import com.z23u184.studymate.domain.usecase.SaveFlashcardBestResultUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class FlashcardSessionViewModel(
    savedStateHandle: SavedStateHandle,
    private val getTopicByIdUseCase: GetTopicByIdUseCase,
    private val getFlashcardsByTopicUseCase: GetFlashcardsByTopicUseCase,
    private val getFlashcardBestResultUseCase: GetFlashcardBestResultUseCase,
    private val saveFlashcardBestResultUseCase: SaveFlashcardBestResultUseCase,
    private val clockProvider: ClockProvider,
) : ViewModel() {
    private val topicId: String = savedStateHandle.get<String>(NavArguments.TOPIC_ID).orEmpty()
    private val _uiState = MutableStateFlow(FlashcardSessionUiState(topicId = topicId))
    val uiState: StateFlow<FlashcardSessionUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var startedAtEpochMs: Long = 0L
    private var tickerJob: Job? = null

    init {
        load()
    }

    fun load() {
        if (topicId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = UiText.StringResource(R.string.topic_not_defined),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val topic = getTopicByIdUseCase(TopicId(topicId)) ?: error("Тема не найдена")
                val cards = getFlashcardsByTopicUseCase(topic.id).map {
                    FlashcardCardUi(
                        taskId = it.taskId.value,
                        question = it.question,
                        answer = it.answer,
                    )
                }
                val previousBest = getFlashcardBestResultUseCase(topic.id)?.let {
                    FlashcardBestResultUi(it.questionsCount, it.durationMs)
                }
                FlashcardSessionUiState(
                    topicId = topicId,
                    topicTitle = topic.title,
                    isLoading = false,
                    cards = cards,
                    previousBest = previousBest,
                )
            }.onSuccess { _uiState.value = it }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.load_error),
                    )
                }
        }
    }

    fun onStartClick() {
        val cards = _uiState.value.cards
        if (cards.isEmpty()) return
        tickerJob?.cancel()
        startedAtEpochMs = clockProvider.now().toEpochMilliseconds()
        _uiState.value = _uiState.value.copy(
            cards = cards.shuffled(),
            currentIndex = 0,
            isStarted = true,
            isAnswerVisible = false,
            elapsedMs = 0L,
            finishDialog = null,
        )
        startTicker()
    }

    fun onCardClick() {
        val state = _uiState.value
        if (!state.isStarted || state.currentCard == null) return
        _uiState.value = state.copy(isAnswerVisible = true)
    }

    fun onNextClick() {
        val state = _uiState.value
        if (!state.isStarted || !state.isAnswerVisible) return
        if (state.currentIndex + 1 < state.cards.size) {
            _uiState.value = state.copy(
                currentIndex = state.currentIndex + 1,
                isAnswerVisible = false,
            )
        } else {
            finishSession()
        }
    }

    fun onFinishDialogDismiss() {
        _uiState.value = _uiState.value.copy(
            finishDialog = null,
            isStarted = false,
            currentIndex = -1,
            isAnswerVisible = false,
            elapsedMs = 0L,
        )
    }

    fun onBackClick() {
        viewModelScope.launch { _events.send(UiEvent.NavigateBack) }
    }

    private fun startTicker() {
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = (clockProvider.now().toEpochMilliseconds() - startedAtEpochMs).coerceAtLeast(0L)
                _uiState.value = _uiState.value.copy(elapsedMs = elapsed)
                delay(200L)
            }
        }
    }

    private fun finishSession() {
        tickerJob?.cancel()
        tickerJob = null
        val state = _uiState.value
        val now = clockProvider.now()
        val durationMs = (now.toEpochMilliseconds() - startedAtEpochMs).coerceAtLeast(0L)
        val questionsCount = state.cards.size
        val previousBest = state.previousBest

        viewModelScope.launch {
            runCatching {
                val saved = saveFlashcardBestResultUseCase(
                    FlashcardBestResult(
                        id = topicId,
                        topicId = TopicId(topicId),
                        questionsCount = questionsCount,
                        durationMs = durationMs,
                        completedAt = now,
                        updatedAt = now,
                    )
                )
                saved
            }.onSuccess { saved ->
                val newBest = FlashcardBestResultUi(saved.questionsCount, saved.durationMs)
                _uiState.value = _uiState.value.copy(
                    elapsedMs = durationMs,
                    previousBest = newBest,
                    finishDialog = FlashcardFinishDialogUi(
                        questionsCount = questionsCount,
                        durationMs = durationMs,
                        previousBest = previousBest,
                        newBest = newBest,
                    ),
                )
            }.onFailure {
                _events.send(UiEvent.ShowSnackbar(it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.load_error)))
            }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }
}
