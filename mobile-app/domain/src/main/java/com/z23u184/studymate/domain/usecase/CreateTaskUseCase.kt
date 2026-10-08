package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.StudyTask
import com.z23u184.studymate.domain.model.TaskStatus
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.repository.TaskRepository
import com.z23u184.studymate.domain.repository.TopicRepository
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.IdGenerator
import com.z23u184.studymate.domain.validation.TaskValidator
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class CreateTaskUseCase(
    private val topicRepository: TopicRepository,
    private val taskRepository: TaskRepository,
    private val taskValidator: TaskValidator,
    private val idGenerator: IdGenerator,
    private val clockProvider: ClockProvider,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase
) {
    suspend operator fun invoke(
        topicId: TopicId,
        title: String,
        description: String,
        status: TaskStatus = TaskStatus.PLANNED,
        deadlineAt: Instant? = null
    ): DomainResult<StudyTask> {
        val topic = topicRepository.getById(topicId)
            ?: return DomainResult.Failure(DomainError.TopicNotFound)

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

        val now = clockProvider.now()
        val task = StudyTask(
            id = idGenerator.newTaskId(),
            topicId = topic.id,
            title = normalizedTitle,
            description = normalizedDescription,
            status = status,
            createdAt = now,
            updatedAt = now,
            deadlineAt = deadlineAt
        )

        taskRepository.save(task)
        syncAfterMutationUseCase()
        return DomainResult.Success(task)
    }
}
