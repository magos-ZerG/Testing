package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.Topic
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.repository.TopicRepository

class GetTopicByIdUseCase(
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(topicId: TopicId): Topic? = topicRepository.getById(topicId)
}
