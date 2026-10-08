package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.FlashcardBestResult
import com.z23u184.studymate.domain.repository.FlashcardRepository

class SaveFlashcardBestResultUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase,
) {
    suspend operator fun invoke(result: FlashcardBestResult): FlashcardBestResult {
        val saved = flashcardRepository.saveBestResultIfBetter(result)
        syncAfterMutationUseCase()
        return saved
    }
}
