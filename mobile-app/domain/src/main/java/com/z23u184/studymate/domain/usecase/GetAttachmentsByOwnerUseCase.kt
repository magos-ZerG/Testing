package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.Attachment
import com.z23u184.studymate.domain.model.AttachmentOwner
import com.z23u184.studymate.domain.repository.AttachmentRepository

class GetAttachmentsByOwnerUseCase(
    private val attachmentRepository: AttachmentRepository
) {
    suspend operator fun invoke(owner: AttachmentOwner): List<Attachment> = attachmentRepository.getByOwner(owner)
}
