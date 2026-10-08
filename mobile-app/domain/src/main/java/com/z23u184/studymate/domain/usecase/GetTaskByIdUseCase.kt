package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.StudyTask
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.repository.TaskRepository

class GetTaskByIdUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: TaskId): StudyTask? = taskRepository.getById(taskId)
}
