package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.Topic
import com.z23u184.studymate.domain.repository.TopicRepository

class GetTopicsUseCase(
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(): List<Topic> = topicRepository.getAll()
}
