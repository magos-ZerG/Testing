package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.repository.SessionRepository

class GetAuthorizedUserIdUseCase(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): String? = sessionRepository.getAuthorizedUserId()
}
