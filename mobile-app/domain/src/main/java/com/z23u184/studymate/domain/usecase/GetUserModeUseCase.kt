package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.UserMode
import com.z23u184.studymate.domain.repository.SessionRepository

class GetUserModeUseCase(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): UserMode = sessionRepository.getUserMode()
}
