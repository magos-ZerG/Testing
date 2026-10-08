package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.StudyTask
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskStatus
import com.z23u184.studymate.domain.repository.TaskRepository
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.TaskStatusTransitionPolicy
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ChangeTaskStatusUseCase(
    private val taskRepository: TaskRepository,
    private val taskStatusTransitionPolicy: TaskStatusTransitionPolicy,
    private val clockProvider: ClockProvider,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase
) {
    suspend operator fun invoke(taskId: TaskId, newStatus: TaskStatus): DomainResult<StudyTask> {
        val task = taskRepository.getById(taskId)
            ?: return DomainResult.Failure(DomainError.TaskNotFound)

        if (!taskStatusTransitionPolicy.canMove(task.status, newStatus)) {
            return DomainResult.Failure(DomainError.InvalidStatusTransition)
        }

        val now = clockProvider.now()
        val updated = task.copy(
            status = newStatus,
            updatedAt = now
        )

        taskRepository.save(updated)
        syncAfterMutationUseCase()
        return DomainResult.Success(updated)
    }
}
