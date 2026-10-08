package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskSolution
import com.z23u184.studymate.domain.repository.TaskRepository
import com.z23u184.studymate.domain.repository.TaskSolutionRepository
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.IdGenerator
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SaveTaskSolutionUseCase(
    private val taskRepository: TaskRepository,
    private val taskSolutionRepository: TaskSolutionRepository,
    private val idGenerator: IdGenerator,
    private val clockProvider: ClockProvider,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase
) {
    suspend operator fun invoke(taskId: TaskId, content: String): DomainResult<TaskSolution> {
        val task = taskRepository.getById(taskId)
            ?: return DomainResult.Failure(DomainError.TaskNotFound)

        val now = clockProvider.now()
        val normalizedContent = content.trim()
        val existing = taskSolutionRepository.getByTaskId(task.id)

        val solution = existing?.copy(
            content = normalizedContent,
            updatedAt = now
        ) ?: TaskSolution(
            id = idGenerator.newTaskSolutionId(),
            taskId = task.id,
            content = normalizedContent,
            updatedAt = now
        )

        taskSolutionRepository.save(solution)
        syncAfterMutationUseCase()
        return DomainResult.Success(solution)
    }
}
