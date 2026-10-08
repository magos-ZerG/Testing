package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.AttachmentOwner
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.repository.AttachmentRepository
import com.z23u184.studymate.domain.repository.TaskRepository
import com.z23u184.studymate.domain.repository.TaskSolutionRepository

class DeleteTaskSolutionUseCase(
    private val taskRepository: TaskRepository,
    private val taskSolutionRepository: TaskSolutionRepository,
    private val attachmentRepository: AttachmentRepository,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase
) {
    suspend operator fun invoke(taskId: TaskId): DomainResult<Unit> {
        val task = taskRepository.getById(taskId)
            ?: return DomainResult.Failure(DomainError.TaskNotFound)

        val solution = taskSolutionRepository.getByTaskId(task.id)
            ?: return DomainResult.Failure(DomainError.SolutionNotFound)

        taskSolutionRepository.deleteByTaskId(solution.taskId)
        attachmentRepository.deleteByOwner(AttachmentOwner.TaskSolution(solution.taskId))
        syncAfterMutationUseCase()
        return DomainResult.Success(Unit)
    }
}
