package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.StudyTask
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskStatus
import com.z23u184.studymate.domain.repository.TaskRepository
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.TaskStatusTransitionPolicy
import com.z23u184.studymate.domain.validation.TaskValidator
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class UpdateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val taskValidator: TaskValidator,
    private val clockProvider: ClockProvider,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase,
    private val taskStatusTransitionPolicy: TaskStatusTransitionPolicy
) {
    suspend operator fun invoke(
        taskId: TaskId,
        title: String,
        description: String,
        status: TaskStatus,
        deadlineAt: Instant?
    ): DomainResult<StudyTask> {
        val task = taskRepository.getById(taskId)
            ?: return DomainResult.Failure(DomainError.TaskNotFound)

        val normalizedTitle = title.trim()
        val normalizedDescription = description.trim()

        when (val titleValidation = taskValidator.validateTitle(normalizedTitle)) {
            is DomainResult.Failure -> return titleValidation
            is DomainResult.Success -> Unit
        }

        when (val descriptionValidation = taskValidator.validateDescription(normalizedDescription)) {
            is DomainResult.Failure -> return descriptionValidation
            is DomainResult.Success -> Unit
        }

        if (!taskStatusTransitionPolicy.canMove(task.status, status)) {
            return DomainResult.Failure(DomainError.InvalidStatusTransition)
        }

        val updated = task.copy(
            title = normalizedTitle,
            description = normalizedDescription,
            status = status,
            deadlineAt = deadlineAt,
            updatedAt = clockProvider.now()
        )

        taskRepository.save(updated)
        syncAfterMutationUseCase()
        return DomainResult.Success(updated)
    }
}
