package com.z23u184.studymate.domain.validation

import com.z23u184.studymate.domain.DomainResult

interface AttachmentValidator {
    fun validate(
        fileName: String,
        mimeType: String,
        sizeBytes: Long
    ): DomainResult<Unit>
}
