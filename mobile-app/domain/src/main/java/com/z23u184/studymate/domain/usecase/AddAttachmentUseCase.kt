package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.Attachment
import com.z23u184.studymate.domain.model.AttachmentOwner
import com.z23u184.studymate.domain.model.AttachmentUploadState
import com.z23u184.studymate.domain.repository.AttachmentRepository
import com.z23u184.studymate.domain.repository.TaskRepository
import com.z23u184.studymate.domain.repository.TaskSolutionRepository
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.IdGenerator
import com.z23u184.studymate.domain.validation.AttachmentValidator
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AddAttachmentUseCase(
    private val taskRepository: TaskRepository,
    private val taskSolutionRepository: TaskSolutionRepository,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentValidator: AttachmentValidator,
    private val idGenerator: IdGenerator,
    private val clockProvider: ClockProvider,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase
) {
    suspend operator fun invoke(
        owner: AttachmentOwner,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        localPath: String? = null
    ): DomainResult<Attachment> {
        when (val validation = attachmentValidator.validate(fileName, mimeType, sizeBytes)) {
            is DomainResult.Failure -> return validation
            is DomainResult.Success -> Unit
        }

        when (owner) {
            is AttachmentOwner.TaskDescription -> {
                taskRepository.getById(owner.taskId)
                    ?: return DomainResult.Failure(DomainError.AttachmentOwnerNotFound)
            }
            is AttachmentOwner.TaskSolution -> {
                taskSolutionRepository.getByTaskId(owner.taskId)
                    ?: return DomainResult.Failure(DomainError.AttachmentOwnerNotFound)
            }
        }

        val attachment = Attachment(
            id = idGenerator.newAttachmentId(),
            owner = owner,
            fileName = fileName.trim(),
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            createdAt = clockProvider.now(),
            localPath = localPath,
            uploadState = if (localPath == null) AttachmentUploadState.NOT_REQUIRED else AttachmentUploadState.PENDING_UPLOAD
        )

        attachmentRepository.save(attachment)
        syncAfterMutationUseCase()
        return DomainResult.Success(attachment)
    }
}
