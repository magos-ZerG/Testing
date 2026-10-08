package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.Topic
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.repository.TopicRepository
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.validation.TopicValidator
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class RenameTopicUseCase(
    private val topicRepository: TopicRepository,
    private val topicValidator: TopicValidator,
    private val clockProvider: ClockProvider,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase
) {
    suspend operator fun invoke(topicId: TopicId, newTitle: String): DomainResult<Topic> {
        val topic = topicRepository.getById(topicId)
            ?: return DomainResult.Failure(DomainError.TopicNotFound)

        val normalizedTitle = newTitle.trim()
        when (val validation = topicValidator.validateTitle(normalizedTitle)) {
            is DomainResult.Failure -> return validation
            is DomainResult.Success -> Unit
        }

        if (topic.title != normalizedTitle && topicRepository.existsByTitle(normalizedTitle)) {
            return DomainResult.Failure(DomainError.TopicTitleAlreadyExists)
        }

        val updated = topic.copy(
            title = normalizedTitle,
            updatedAt = clockProvider.now()
        )

        topicRepository.save(updated)
        syncAfterMutationUseCase()
        return DomainResult.Success(updated)
    }
}
