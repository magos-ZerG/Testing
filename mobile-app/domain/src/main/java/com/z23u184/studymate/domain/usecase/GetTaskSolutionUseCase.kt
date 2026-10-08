package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskSolution
import com.z23u184.studymate.domain.repository.TaskSolutionRepository

class GetTaskSolutionUseCase(
    private val taskSolutionRepository: TaskSolutionRepository
) {
    suspend operator fun invoke(taskId: TaskId): TaskSolution? = taskSolutionRepository.getByTaskId(taskId)
}
