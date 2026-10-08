package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.StudyTask
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.repository.TaskRepository

class GetTasksByTopicUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(topicId: TopicId): List<StudyTask> = taskRepository.getByTopicId(topicId)
}
