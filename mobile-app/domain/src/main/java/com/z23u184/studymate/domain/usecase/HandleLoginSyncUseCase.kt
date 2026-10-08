package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.repository.SessionRepository
import com.z23u184.studymate.domain.service.SyncGateway
import com.z23u184.studymate.domain.service.SyncPolicyService

class HandleLoginSyncUseCase(
    private val sessionRepository: SessionRepository,
    private val syncPolicyService: SyncPolicyService,
    private val syncGateway: SyncGateway,
) {
    suspend operator fun invoke(): DomainResult<Unit> {
        val userMode = sessionRepository.getUserMode()
        if (!syncPolicyService.shouldSyncOnLogin(userMode)) {
            return DomainResult.Success(Unit)
        }
        syncGateway.requestLoginSync()
        return DomainResult.Success(Unit)
    }
}
