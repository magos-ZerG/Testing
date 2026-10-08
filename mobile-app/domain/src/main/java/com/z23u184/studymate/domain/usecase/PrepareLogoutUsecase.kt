package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.UserMode
import com.z23u184.studymate.domain.repository.SessionRepository
import com.z23u184.studymate.domain.service.SyncGateway

class PrepareLogoutUseCase(
    private val sessionRepository: SessionRepository,
    private val syncGateway: SyncGateway,
) {
    suspend operator fun invoke() {
        val userMode = sessionRepository.getUserMode()
        if (userMode == UserMode.AUTHORIZED) {
            syncGateway.requestLogoutSync()
        }
    }
}
