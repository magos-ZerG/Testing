package com.z23u184.studymate.domain.validation

import com.z23u184.studymate.domain.DomainResult

interface TopicValidator {
    fun validateTitle(title: String): DomainResult<Unit>
}
