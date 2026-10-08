package com.z23u184.studymate.domain.validation

import com.z23u184.studymate.domain.DomainError
import com.z23u184.studymate.domain.DomainResult

class DefaultTaskValidator(
    private val maxTitleLength: Int = 150,
    private val maxDescriptionLength: Int = 10_000
) : TaskValidator {

    override fun validateTitle(title: String): DomainResult<Unit> {
        val normalized = title.trim()

        if (normalized.isEmpty()) {
            return DomainResult.Failure(DomainError.EmptyTaskTitle)
        }

        if (normalized.length > maxTitleLength) {
            return DomainResult.Failure(DomainError.TaskTitleTooLong)
        }

        return DomainResult.Success(Unit)
    }

    override fun validateDescription(description: String): DomainResult<Unit> {
        if (description.length > maxDescriptionLength) {
            return DomainResult.Failure(DomainError.TaskDescriptionTooLong)
        }

        return DomainResult.Success(Unit)
    }
}
