package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.AttachmentId
import com.z23u184.studymate.domain.repository.AttachmentRepository

class DeleteAttachmentUseCase(
    private val attachmentRepository: AttachmentRepository,
    private val syncAfterMutationUseCase: SyncAfterMutationUseCase
) {
    suspend operator fun invoke(attachmentId: AttachmentId): DomainResult<Unit> {
        val attachment = attachmentRepository.getById(attachmentId)
            ?: return DomainResult.Failure(DomainError.AttachmentNotFound)

        attachmentRepository.delete(attachment.id)
        syncAfterMutationUseCase()
        return DomainResult.Success(Unit)
    }
}
