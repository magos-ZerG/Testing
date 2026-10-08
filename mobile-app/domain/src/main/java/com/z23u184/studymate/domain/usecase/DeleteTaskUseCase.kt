package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.repository.TaskRepository

class DeleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase
) {
    suspend operator fun invoke(taskId: TaskId): DomainResult<Unit> {
        val task = taskRepository.getById(taskId)
            ?: return DomainResult.Failure(DomainError.TaskNotFound)

        taskRepository.delete(task.id)

        syncAfterMutationUseCase()
        return DomainResult.Success(Unit)
    }
}
