package com.z23u184.studymate.domain.validation

import com.z23u184.studymate.domain.DomainResult

interface TaskValidator {
    fun validateTitle(title: String): DomainResult<Unit>
    fun validateDescription(description: String): DomainResult<Unit>
}
