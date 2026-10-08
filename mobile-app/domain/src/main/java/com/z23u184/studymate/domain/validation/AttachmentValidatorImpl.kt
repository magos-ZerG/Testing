package com.z23u184.studymate.domain.validation

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult

class DefaultAttachmentValidator(
    private val maxSizeBytes: Long = 25L * 1024L * 1024L,
    private val allowedMimeTypes: Set<String> = setOf(
        "image/jpeg",
        "image/png",
        "application/pdf",
        "text/plain"
    )
) : AttachmentValidator {

    override fun validate(
        fileName: String,
        mimeType: String,
        sizeBytes: Long
    ): DomainResult<Unit> {
        if (fileName.trim().isEmpty()) {
            return DomainResult.Failure(DomainError.EmptyAttachmentFileName)
        }

        if (sizeBytes < 0L) {
            return DomainResult.Failure(DomainError.InvalidAttachmentSize)
        }

        if (sizeBytes > maxSizeBytes) {
            return DomainResult.Failure(DomainError.AttachmentTooLarge)
        }

        if (mimeType !in allowedMimeTypes) {
            return DomainResult.Failure(DomainError.UnsupportedAttachmentType)
        }

        return DomainResult.Success(Unit)
    }
}
