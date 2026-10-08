package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.repository.TopicRepository

class DeleteTopicUseCase(
    private val topicRepository: TopicRepository,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase,
) {
    suspend operator fun invoke(topicId: TopicId): DomainResult<Unit> {
        val topic = topicRepository.getById(topicId)
            ?: return DomainResult.Failure(DomainError.TopicNotFound)

        topicRepository.delete(topic.id)
        syncAfterMutationUseCase()

        return DomainResult.Success(Unit)
    }
}
