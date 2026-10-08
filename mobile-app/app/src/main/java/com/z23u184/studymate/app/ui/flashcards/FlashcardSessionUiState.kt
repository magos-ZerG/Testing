package com.z23u184.studymate.app.ui.flashcards

import com.z23u184.studymate.app.util.UiText

data class FlashcardSessionUiState(
    val topicId: String = "",
    val topicTitle: String = "",
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val cards: List<FlashcardCardUi> = emptyList(),
    val currentIndex: Int = -1,
    val isStarted: Boolean = false,
    val isAnswerVisible: Boolean = false,
    val elapsedMs: Long = 0L,
    val previousBest: FlashcardBestResultUi? = null,
    val finishDialog: FlashcardFinishDialogUi? = null,
) {
    val currentCard: FlashcardCardUi? get() = cards.getOrNull(currentIndex)
    val progress: Pair<Int, Int> get() = ((currentIndex + 1).coerceAtLeast(0)) to cards.size
    val canStart: Boolean get() = !isLoading && cards.isNotEmpty() && !isStarted
}

data class FlashcardCardUi(
    val taskId: String,
    val question: String,
    val answer: String,
)

data class FlashcardBestResultUi(
    val questionsCount: Int,
    val durationMs: Long,
)

data class FlashcardFinishDialogUi(
    val questionsCount: Int,
    val durationMs: Long,
    val previousBest: FlashcardBestResultUi?,
    val newBest: FlashcardBestResultUi,
)
