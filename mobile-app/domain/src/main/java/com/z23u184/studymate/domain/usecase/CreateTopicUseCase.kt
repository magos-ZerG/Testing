package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.Topic
import com.z23u184.studymate.domain.repository.TopicRepository
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.IdGenerator
import com.z23u184.studymate.domain.validation.TopicValidator
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CreateTopicUseCase(
    private val topicRepository: TopicRepository,
    private val topicValidator: TopicValidator,
    private val idGenerator: IdGenerator,
    private val clockProvider: ClockProvider,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase
) {
    suspend operator fun invoke(title: String): DomainResult<Topic> {
        val normalizedTitle = title.trim()

        when (val validation = topicValidator.validateTitle(normalizedTitle)) {
            is DomainResult.Failure -> return validation
            is DomainResult.Success -> Unit
        }

        if (topicRepository.existsByTitle(normalizedTitle)) {
            return DomainResult.Failure(DomainError.TopicTitleAlreadyExists)
        }

        val now = clockProvider.now()
        val topic = Topic(
            id = idGenerator.newTopicId(),
            title = normalizedTitle,
            createdAt = now,
            updatedAt = now
        )

        topicRepository.save(topic)
        syncAfterMutationUseCase()
        return DomainResult.Success(topic)
    }
}
