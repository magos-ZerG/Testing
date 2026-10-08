package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.repository.FlashcardRepository

class GetFlashcardsByTopicUseCase(
    private val flashcardRepository: FlashcardRepository,
) {
    suspend operator fun invoke(topicId: TopicId) = flashcardRepository.getCardsByTopic(topicId)
}
