package com.z23u184.studymate.domain.validation

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult

class DefaultTopicValidator(
    private val maxTitleLength: Int = 100
) : TopicValidator {

    override fun validateTitle(title: String): DomainResult<Unit> {
        val normalized = title.trim()

        if (normalized.isEmpty()) {
            return DomainResult.Failure(DomainError.EmptyTopicTitle)
        }

        if (normalized.length > maxTitleLength) {
            return DomainResult.Failure(DomainError.TopicTitleTooLong)
        }

        return DomainResult.Success(Unit)
    }
}
